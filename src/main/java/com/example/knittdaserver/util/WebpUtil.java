package com.example.knittdaserver.util;

import java.io.File;

import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.webp.WebpWriter;

@Slf4j
@Component
public class WebpUtil {
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
            
            // .jpg 또는 .png 확장자를 .webp로 변경
            String webpFileName = fileName;
            if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                webpFileName = fileName.replace(".jpg", ".webp").replace(".jpeg", ".webp");
            } else if (fileName.endsWith(".png")) {
                webpFileName = fileName.replace(".png", ".webp");
            } else {
                // 확장자가 없거나 다른 경우 .webp 추가
                webpFileName = fileName + ".webp";
            }
            
            File webpFile = new File(file.getParent(), webpFileName);
            log.debug("[WebpUtil] WebP 파일 경로 생성 - 원본: {}, 변환: {}", fileName, webpFileName);
            
            // 이미지 로드 및 변환
            log.debug("[WebpUtil] ImmutableImage 로더로 이미지 로드 시작");
            File result = ImmutableImage.loader()
                    .fromFile(file) 
                    .output(WebpWriter.DEFAULT, webpFile);
            
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
}