package com.example.knittdaserver.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.knittdaserver.service.S3Service;

@Component
public class S3DeleteHelper {
    private static S3Service s3Service;

    @Autowired
    public S3DeleteHelper(S3Service s3Service) {
        S3DeleteHelper.s3Service = s3Service;
    }

    public static void deleteFile(String url) {
        s3Service.deleteFile(url);
    }
}
