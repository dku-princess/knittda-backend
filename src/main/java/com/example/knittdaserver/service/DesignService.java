package com.example.knittdaserver.service;

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
    
    @Transactional
    public DesignDto createDesign(CreateDesignRequest request) {
        Design design = designRepository.save(request.to());
        return DesignDto.from(design);
    }


}
