package com.example.knittdaserver.controller;

import com.example.knittdaserver.dto.DesignDto;
import com.example.knittdaserver.service.DesignQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/designs")
@RequiredArgsConstructor
public class DesignController {

    private final DesignQueryService designQueryService;
    private final WebSocketServletAutoConfiguration webSocketServletAutoConfiguration;

    @GetMapping("/v1")
    public List<DesignDto> searchV1(@RequestParam(required = false) String title, @RequestParam(required = false) String designer) {
        Long startTime = System.currentTimeMillis();
        log.info("Searching designs with title {} and designer {}", title, designer);
        List<DesignDto> result = designQueryService.searchV1(title, designer);
        log.info("Result count {}, Search completed in {} ms ", result.size() ,System.currentTimeMillis() - startTime);
        return result;
    }

    @GetMapping("/v2")
    public List<DesignDto> searchV2(@RequestParam(required = false) String keyword) {
        Long startTime = System.currentTimeMillis();
        List<DesignDto> result = designQueryService.searchV2(keyword);
        log.info("completed in {}ms, size:{}", System.currentTimeMillis() - startTime, result.size());
        return result;
    }

    @GetMapping("/v3")
    public List<DesignDto> searchV3(@RequestParam(required = false) String keyword) {
        Long startTime = System.currentTimeMillis();
        List<DesignDto> result = designQueryService.searchV3(keyword);
        log.info("completed in {}ms, size:{}", System.currentTimeMillis() - startTime, result.size());
        return result;
    }
}
