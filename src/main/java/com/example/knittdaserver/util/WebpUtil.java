package com.example.knittdaserver.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.webp.WebpWriter;

@Slf4j
@Component
public class WebpUtil {

    /** heif-convert 실행 파일 경로. 기본값은 PATH 탐색(heif-convert). 환경별 재정의 가능. */
    @Value("${image.heif-convert-path:heif-convert}")
    private String heifConvertPath;

    /** heif-convert 최대 실행 시간(초). */
    @Value("${image.heif-convert-timeout-seconds:20}")
    private long heifConvertTimeoutSeconds;

        public File convertToWebp(File file) {
        String fileName = file.getName();
        long originalSize = file.length();
        String filePath = file.getAbsolutePath();
        
        log.info("[WebpUtil] WebP 변환 시작 - 파일명: {}, 경로: {}, 원본 크기: {} bytes", 
                fileName, filePath, originalSize);
        
        try {
            // 파일 존재 여부 확인
            if (!file.exists()) {
                log.error("[WebpUtil] 파일이 존재하지 않음 - 경로: {}", filePath);
                return null;
            }
            
            if (!file.canRead()) {
                log.error("[WebpUtil] 파일을 읽을 수 없음 - 경로: {}", filePath);
                return null;
            }
            
            // 확장자를 .webp로 변경 (.jpg/.jpeg/.png/.heic/.heif 등)
            String webpFileName = toWebpFileName(fileName);

            File webpFile = new File(file.getParent(), webpFileName);
            log.debug("[WebpUtil] WebP 파일 경로 생성 - 원본: {}, 변환: {}", fileName, webpFileName);

            // 이미지 로드
            // HEIC/HEIF 는 표준 JDK ImageIO(= scrimage 기본 로더)로 디코딩되지 않으므로
            // 네이티브 libheif CLI(heif-convert)로 PNG로 선변환한 뒤 기존 파이프라인에 합류시킨다.
            ImmutableImage image;
            if (isHeif(file)) {
                log.info("[WebpUtil] HEIF/HEIC 감지 - heif-convert 로 디코딩 시작 - 파일: {}", fileName);
                image = decodeHeif(file);
            } else {
                log.debug("[WebpUtil] ImmutableImage 로더로 이미지 로드 시작");
                image = ImmutableImage.loader().fromFile(file);
            }

            File result = image.output(WebpWriter.DEFAULT, webpFile);
            
            if (result == null || !result.exists()) {
                log.error("[WebpUtil] WebP 변환 결과 파일이 생성되지 않음 - 예상 경로: {}", 
                        webpFile.getAbsolutePath());
                return null;
            }
            
            long convertedSize = result.length();
            double compressionRatio = originalSize > 0 ? (1.0 - (double) convertedSize / originalSize) * 100 : 0;
            
            log.info("[WebpUtil] WebP 변환 완료 - 원본: {} bytes, 변환: {} bytes, 압축률: {:.2f}%, 파일: {}", 
                    originalSize, convertedSize, compressionRatio, result.getName());
            
            return result;
        } catch (Exception e) {
            log.error("[WebpUtil] 이미지를 WebP로 변환하는 중 오류 발생 - 파일명: {}, 경로: {}, 크기: {} bytes, 에러: {}",
                    fileName, filePath, originalSize, e.getMessage(), e);
            return null;
        }
    }

    /** 원본 파일명의 확장자를 .webp 로 치환한다. 확장자가 없으면 .webp 를 덧붙인다. */
    private String toWebpFileName(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            return fileName.substring(0, lastDot) + ".webp";
        }
        return fileName + ".webp";
    }

    /** HEIF/HEIC 컨테이너 브랜드 목록 (ISO BMFF ftyp 박스의 major/compatible brand) */
    private static final List<String> HEIF_BRANDS =
            Arrays.asList("heic", "heix", "heif", "heim", "heis", "hevc", "hevx", "mif1", "msf1");

    /**
     * ISO BMFF ftyp 박스의 브랜드로 HEIF/HEIC 여부를 판별한다.
     * 파일 구조: [0-3]=box size, [4-7]="ftyp", [8-11]=major brand.
     * 확장자/Content-Type 에 의존하지 않으므로 iOS 가 octet-stream 으로 올려도 감지된다.
     */
    private boolean isHeif(File file) {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            byte[] header = new byte[12];
            int read = raf.read(header);
            if (read < 12) {
                return false;
            }
            String box = new String(header, 4, 4, StandardCharsets.US_ASCII);
            if (!"ftyp".equals(box)) {
                return false;
            }
            String majorBrand = new String(header, 8, 4, StandardCharsets.US_ASCII).toLowerCase();
            return HEIF_BRANDS.contains(majorBrand);
        } catch (IOException e) {
            log.warn("[WebpUtil] HEIF 매직바이트 판별 실패, 일반 이미지로 처리 - 파일: {}, 에러: {}",
                    file.getName(), e.getMessage());
            return false;
        }
    }

    /**
     * HEIC/HEIF 파일을 heif-convert(libheif CLI)로 PNG 임시파일로 디코딩한 뒤
     * scrimage 로 로드해 반환한다. 사용한 PNG 임시파일은 finally 에서 정리한다.
     */
    private ImmutableImage decodeHeif(File file) throws IOException {
        File pngTemp = new File(file.getParentFile(), "heif_" + UUID.randomUUID() + ".png");
        try {
            File produced = runHeifConvert(file, pngTemp);
            if (produced == null) {
                throw new IOException("heif-convert failed to decode " + file.getName());
            }
            return ImmutableImage.loader().fromFile(produced);
        } finally {
            deleteQuietly(pngTemp);
            for (File extra : indexedOutputs(pngTemp)) {
                deleteQuietly(extra);
            }
        }
    }

    /**
     * heif-convert 를 자식 프로세스로 실행해 HEIC → PNG 변환한다.
     * 성공 시 실제 생성된 출력 파일을, 실패/타임아웃 시 null 을 반환한다.
     * - 인자 배열 실행(쉘 인젝션 차단), 출력은 임시 로그파일로 리다이렉트(파이프 블로킹 방지),
     *   타임아웃 초과 시 강제 종료.
     */
    private File runHeifConvert(File in, File preferredOut) {
        List<String> cmd = Arrays.asList(
                heifConvertPath, in.getAbsolutePath(), preferredOut.getAbsolutePath());
        log.debug("[WebpUtil] heif-convert 실행 - cmd: {}", cmd);

        File logFile = null;
        Process process = null;
        try {
            logFile = File.createTempFile("heif_out_", ".log", in.getParentFile());
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            pb.redirectOutput(logFile);
            process = pb.start();

            boolean finished = process.waitFor(heifConvertTimeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.error("[WebpUtil] heif-convert 타임아웃({}s) - 입력: {}",
                        heifConvertTimeoutSeconds, in.getName());
                return null;
            }

            int exit = process.exitValue();
            if (exit != 0) {
                log.error("[WebpUtil] heif-convert 실패 - exit={}, 입력: {}, 출력: {}",
                        exit, in.getName(), readTail(logFile));
                return null;
            }

            // 단일 이미지: preferredOut 생성. 다중 이미지: heif-convert 가 접미사(-1 등)를 붙일 수 있음.
            if (preferredOut.exists()) {
                return preferredOut;
            }
            File[] indexed = indexedOutputs(preferredOut);
            if (indexed.length > 0) {
                return indexed[0];
            }
            log.error("[WebpUtil] heif-convert 출력 파일 없음 - 예상: {}, 입력: {}",
                    preferredOut.getName(), in.getName());
            return null;
        } catch (IOException e) {
            log.error("[WebpUtil] heif-convert 실행 오류 - 입력: {}, 에러: {}", in.getName(), e.getMessage(), e);
            if (process != null) {
                process.destroyForcibly();
            }
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[WebpUtil] heif-convert 대기 중 인터럽트 - 입력: {}", in.getName(), e);
            if (process != null) {
                process.destroyForcibly();
            }
            return null;
        } finally {
            deleteQuietly(logFile);
        }
    }

    /** preferredOut 과 같은 base 이름에 접미사(-1, -2 …)가 붙은 형제 출력 파일들(정렬됨). */
    private File[] indexedOutputs(File preferredOut) {
        String name = preferredOut.getName();
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String ext = dot > 0 ? name.substring(dot) : "";
        File parent = preferredOut.getParentFile();
        File[] matches = (parent == null) ? null
                : parent.listFiles((dir, n) -> n.startsWith(base + "-") && n.endsWith(ext));
        if (matches == null) {
            return new File[0];
        }
        Arrays.sort(matches);
        return matches;
    }

    private void deleteQuietly(File f) {
        if (f != null && f.exists() && !f.delete()) {
            log.warn("[WebpUtil] 임시 파일 삭제 실패 - 경로: {}", f.getAbsolutePath());
        }
    }

    /** 로그 파일의 마지막 500자 정도를 읽어 실패 원인 로깅에 사용. */
    private String readTail(File logFile) {
        try {
            String s = Files.readString(logFile.toPath(), StandardCharsets.UTF_8).trim();
            return s.length() > 500 ? s.substring(s.length() - 500) : s;
        } catch (IOException e) {
            return "(로그 읽기 실패)";
        }
    }
}