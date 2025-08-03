package com.example.knittdaserver.util;

import java.io.File;

import org.springframework.stereotype.Component;

import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.webp.WebpWriter;

@Component
public class WebpUtil {
        public File convertToWebp(File file) {
        try {
            return ImmutableImage.loader()// 라이브러리 객체 생성
                    .fromFile(file) // .jpg or .png File 가져옴
                    .output(WebpWriter.DEFAULT, new File(file.getParent(), file.getName().replace(".jpg", ".webp"))); // 손실 압축 설정, fileName.webp로 파일 생성
        } catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }
}