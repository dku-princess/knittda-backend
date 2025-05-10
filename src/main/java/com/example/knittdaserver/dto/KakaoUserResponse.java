package com.example.knittdaserver.dto;

import lombok.Data;
import lombok.Getter;
import org.springframework.context.annotation.Profile;

@Data
@Getter
public class KakaoUserResponse {
    private Long id;
    private KakaoAccount kakao_account;

    @Data
    public static class KakaoAccount {
        private KakaoProfile profile;
        private String email;

        @Data
        public static class KakaoProfile {
            private String nickname;
            private String profile_image_url;
        }
    }
}