package com.example.knittdaserver.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MemberController {

    @GetMapping("/login/kakao")
    public void loginKakao() {

    }

}
