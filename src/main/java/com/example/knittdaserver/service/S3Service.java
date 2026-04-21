package com.example.knittdaserver.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class S3Service {
    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucketName}")
    private String bucketName;

    @Transactional
    public String uploadFile(File file) {
        return uploadFileWithPrefix(file, "uploads/");
    }

    @Transactional
    public String uploadProfileImage(File file) {
        return uploadFileWithPrefix(file, "profiles/");
    }

    private String uploadFileWithPrefix(File file, String prefix) {
        String originalFileName = file.getName();
        long fileSize = file.length();
        String fileName = prefix + UUID.randomUUID() + "-" + originalFileName;
        
        log.info("[S3Service] 파일 업로드 시작 - 원본 파일명: {}, 크기: {} bytes, S3 파일명: {}, 버킷: {}", 
                originalFileName, fileSize, fileName, bucketName);
        
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.length());
            metadata.setContentType("image/webp");

            log.debug("[S3Service] S3 putObject 실행 - 버킷: {}, 파일명: {}, 크기: {} bytes", 
                    bucketName, fileName, file.length());
            
            amazonS3.putObject(bucketName, fileName, new java.io.FileInputStream(file), metadata);

            String url = amazonS3.getUrl(bucketName, fileName).toString();
            log.info("[S3Service] 파일 업로드 완료 - URL: {}", url);
            
            return url;
        } catch (IOException e) {
            log.error("[S3Service] 파일 업로드 중 IOException 발생 - 원본 파일명: {}, 크기: {} bytes, S3 파일명: {}, 에러: {}", 
                    originalFileName, fileSize, fileName, e.getMessage(), e);
            throw new RuntimeException("Failed to upload file to S3: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("[S3Service] 파일 업로드 중 예상치 못한 에러 발생 - 원본 파일명: {}, 크기: {} bytes, S3 파일명: {}, 에러: {}", 
                    originalFileName, fileSize, fileName, e.getMessage(), e);
            throw new RuntimeException("Unexpected error during S3 upload: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void deleteFile(String fileName) {
        log.info("[S3Service] 파일 삭제 시작 - 파일명: {}, 버킷: {}", fileName, bucketName);
        
        try {
            // URL에서 파일명 추출 (전체 URL이 전달될 수 있음)
            String key = extractKeyFromUrl(fileName);
            
            log.debug("[S3Service] S3 deleteObject 실행 - 버킷: {}, 키: {}", bucketName, key);
            amazonS3.deleteObject(bucketName, key);
            
            log.info("[S3Service] 파일 삭제 완료 - 키: {}", key);
        } catch (Exception e) {
            log.error("[S3Service] 파일 삭제 중 에러 발생 - 파일명: {}, 에러: {}", fileName, e.getMessage(), e);
            throw new RuntimeException("Failed to delete file from S3: " + e.getMessage(), e);
        }
    }
    
    private String extractKeyFromUrl(String urlOrKey) {
        // URL 형식인 경우 키 추출
        if (urlOrKey.contains("amazonaws.com") || urlOrKey.contains("s3")) {
            try {
                // URL에서 마지막 경로 부분만 추출
                int lastSlashIndex = urlOrKey.lastIndexOf('/');
                if (lastSlashIndex >= 0 && lastSlashIndex < urlOrKey.length() - 1) {
                    return urlOrKey.substring(lastSlashIndex + 1);
                }
            } catch (Exception e) {
                log.warn("[S3Service] URL에서 키 추출 실패, 원본 값 사용 - 값: {}", urlOrKey);
            }
        }
        // 이미 키 형식이거나 추출 실패 시 원본 반환
        return urlOrKey;
    }


    public URL getFileUrl(String fileName) {
        return amazonS3.getUrl(bucketName, fileName);
    }

}
