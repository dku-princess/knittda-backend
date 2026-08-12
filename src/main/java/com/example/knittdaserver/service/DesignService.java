package com.example.knittdaserver.service;

import com.example.knittdaserver.common.metrics.BusinessMetrics;
import com.example.knittdaserver.dto.CreateDesignRequest;
import com.example.knittdaserver.dto.DesignDto;
import com.example.knittdaserver.entity.Design;
import com.example.knittdaserver.repository.DesignRepository;
import lombok.RequiredArgsConstructor;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DesignService {
    private final DesignRepository designRepository;
    private final BusinessMetrics businessMetrics;
    
    @Transactional
    public DesignDto createDesign(CreateDesignRequest request) {
        Design design = designRepository.save(request.to());
        businessMetrics.count("design.created");
        return DesignDto.from(design);
    }


}
