package com.example.knittdaserver.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
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
        String originalFileName = file.getName();
        String fileName = generateFileName(originalFileName);
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.length());
            metadata.setContentType("image/webp");

            amazonS3.putObject(bucketName, fileName, new java.io.FileInputStream(file), metadata);

            return amazonS3.getUrl(bucketName, fileName).toString();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Transactional
    public void deleteFile(String fileName) {
        amazonS3.deleteObject(bucketName, fileName);
    }


    public URL getFileUrl(String fileName) {
        return amazonS3.getUrl(bucketName, fileName);
    }

    private String generateFileName(String originalFileName) {
        return "uploads/" + UUID.randomUUID() + "-" + originalFileName;
    }
}
