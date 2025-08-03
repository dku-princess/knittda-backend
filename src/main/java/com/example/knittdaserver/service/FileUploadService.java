package com.example.knittdaserver.service;

import com.example.knittdaserver.util.WebpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import java.io.File;
import java.io.IOException;

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
        File tempFile = File.createTempFile("upload_", ".jpg");
        File webpFile = null;
        try {
            multipartFile.transferTo(tempFile);
            webpFile = webpUtil.convertToWebp(tempFile);
            return s3Service.uploadFile(webpFile);
        } finally {
            tempFile.delete();
            if (webpFile != null) webpFile.delete();
        }
    }
} 