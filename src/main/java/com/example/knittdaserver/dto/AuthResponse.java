package com.example.knittdaserver.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@Builder
@AllArgsConstructor
@ToString
public class AuthResponse {
    private String jwt;
    private UserDto user;
}
