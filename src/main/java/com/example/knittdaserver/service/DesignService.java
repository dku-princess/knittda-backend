package com.example.knittdaserver.service;

import com.example.knittdaserver.dto.CreateDesignRequest;
import com.example.knittdaserver.dto.DesignDto;
import com.example.knittdaserver.entity.Design;
import com.example.knittdaserver.repository.DesignRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DesignService {
    private final DesignRepository designRepository;
    public DesignDto createDesign(CreateDesignRequest request) {
        Design design = designRepository.save(request.to());
        return DesignDto.from(design);
    }
}
