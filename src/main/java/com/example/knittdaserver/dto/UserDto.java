package com.example.knittdaserver.dto;

import com.example.knittdaserver.entity.User;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class UserDto {
    private Long id;
    private Long kakaoId;
    private String email;
    private String nickname;
    private String profileImageUrl;

    public static UserDto from(User user) {
        return UserDto.builder()
                .id(user.getId())
                .kakaoId(user.getKakaoId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }
}
