package com.example.knittdaserver.controller;

import com.example.knittdaserver.common.response.ApiResponse;
import com.example.knittdaserver.dto.DesignDto;
import com.example.knittdaserver.service.DesignQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/designs")
@RequiredArgsConstructor
public class DesignController {

    private final DesignQueryService designQueryService;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<?>> search(@RequestParam(required = false) String keyword) {
        List<DesignDto> result = designQueryService.search(keyword);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
