package com.example.knittdaserver.service;

import com.example.knittdaserver.util.WebpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import java.io.File;
import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {
    private final S3Service s3Service;
    private final WebpUtil webpUtil;

    /**
     * MultipartFile을 WebP로 변환 후 S3에 업로드하고, 업로드된 이미지 URL을 반환합니다.
     */
    @Transactional
    public String uploadImageAsWebp(MultipartFile multipartFile) throws IOException {
        String originalFileName = multipartFile.getOriginalFilename();
        long fileSize = multipartFile.getSize();
        String contentType = multipartFile.getContentType();
        
        log.info("[FileUpload] 이미지 업로드 시작 - 파일명: {}, 크기: {} bytes, Content-Type: {}", 
                originalFileName, fileSize, contentType);
        
        File tempFile = null;
        File webpFile = null;
        
        try {
            // 임시 파일 생성
            String extension = getFileExtension(originalFileName);
            tempFile = File.createTempFile("upload_", extension);
            log.debug("[FileUpload] 임시 파일 생성 완료 - 경로: {}", tempFile.getAbsolutePath());
            
            // MultipartFile을 임시 파일로 저장
            multipartFile.transferTo(tempFile);
            log.debug("[FileUpload] 임시 파일 저장 완료 - 크기: {} bytes", tempFile.length());
            
            // WebP로 변환
            log.info("[FileUpload] WebP 변환 시작 - 원본 파일: {}", tempFile.getName());
            webpFile = webpUtil.convertToWebp(tempFile);
            
            if (webpFile == null) {
                log.error("[FileUpload] WebP 변환 실패 - 원본 파일: {}, 크기: {} bytes", 
                        originalFileName, fileSize);
                throw new IOException("Failed to convert image to WebP format");
            }
            
            log.info("[FileUpload] WebP 변환 완료 - 변환된 파일: {}, 크기: {} bytes", 
                    webpFile.getName(), webpFile.length());
            
            // S3 업로드
            log.info("[FileUpload] S3 업로드 시작 - 파일: {}", webpFile.getName());
            String imageUrl = s3Service.uploadFile(webpFile);
            log.info("[FileUpload] S3 업로드 완료 - URL: {}", imageUrl);
            
            return imageUrl;
            
        } catch (IOException e) {
            log.error("[FileUpload] 이미지 업로드 중 IOException 발생 - 파일명: {}, 크기: {} bytes, 에러: {}", 
                    originalFileName, fileSize, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("[FileUpload] 이미지 업로드 중 예상치 못한 에러 발생 - 파일명: {}, 크기: {} bytes, 에러: {}", 
                    originalFileName, fileSize, e.getMessage(), e);
            throw new IOException("Unexpected error during image upload: " + e.getMessage(), e);
        } finally {
            // 임시 파일 정리
            if (tempFile != null && tempFile.exists()) {
                boolean deleted = tempFile.delete();
                if (!deleted) {
                    log.warn("[FileUpload] 임시 파일 삭제 실패 - 경로: {}", tempFile.getAbsolutePath());
                } else {
                    log.debug("[FileUpload] 임시 파일 삭제 완료 - 경로: {}", tempFile.getAbsolutePath());
                }
            }
            if (webpFile != null && webpFile.exists()) {
                boolean deleted = webpFile.delete();
                if (!deleted) {
                    log.warn("[FileUpload] WebP 파일 삭제 실패 - 경로: {}", webpFile.getAbsolutePath());
                } else {
                    log.debug("[FileUpload] WebP 파일 삭제 완료 - 경로: {}", webpFile.getAbsolutePath());
                }
            }
        }
    }
    
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return ".tmp";
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex);
        }
        return ".tmp";
    }
} 