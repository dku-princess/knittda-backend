package com.example.knittdaserver;

import com.example.knittdaserver.entity.Record;
import com.example.knittdaserver.repository.RecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.embedding.EmbeddingResult;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecordEmbeddingInitializer implements CommandLineRunner {

    private final RecordRepository recordRepository;
    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("🔄 Embedding 일괄 생성 시작...");
        List<Record> records = recordRepository.findAllWithAssociations();
        int updated = 0, skipped = 0, failed = 0;
        for (Record record : records) {
            try {
                if (record.getEmbeddingJson() != null && !record.getEmbeddingJson().isBlank()) {
                    skipped++;
                    continue;
                }
                String text = buildTextFromRecord(record);
                float[] embedding = getEmbeddingFromOpenAI(text);
                String json = objectMapper.writeValueAsString(embedding);
                record.setEmbeddingJson(json);
                updated++;
                log.info("✅ Record ID[{}] 임베딩 생성 완료", record.getId());
            } catch (Exception e) {
                log.error("❌ Record ID[{}] 임베딩 생성 실패: {}", record.getId(), e.getMessage());
                failed++;
            }
        }
        if (updated > 0) {
            recordRepository.saveAll(records);
            log.info("💾 DB 저장 완료");
        }
        log.info("🎉 Embedding 일괄 생성 완료 - 업데이트: {}, 스킵: {}, 실패: {}", updated, skipped, failed);
    }

    private String buildTextFromRecord(Record record) {
        StringBuilder sb = new StringBuilder();
        if (record.getProject() != null) {
            if (record.getProject().getNickname() != null)
                sb.append(record.getProject().getNickname()).append(" ");
            if (record.getProject().getDesign() != null) {
                if (record.getProject().getDesign().getTitle() != null)
                    sb.append(record.getProject().getDesign().getTitle()).append(" ");
                if (record.getProject().getDesign().getDesigner() != null)
                    sb.append(record.getProject().getDesign().getDesigner()).append(" ");
            }
        }
        if (record.getComment() != null)
            sb.append(record.getComment()).append(" ");
        if (record.getTags() != null) {
            for (String tag : record.getTags())
                sb.append(tag).append(" ");
        }
        return sb.toString().trim();
    }

    private float[] getEmbeddingFromOpenAI(String text) {
        EmbeddingRequest request = EmbeddingRequest.builder()
                .model("text-embedding-ada-002")
                .input(List.of(text))
                .build();
        EmbeddingResult result = openAiService.createEmbeddings(request);
        List<Double> list = result.getData().get(0).getEmbedding();
        float[] embedding = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            embedding[i] = list.get(i).floatValue();
        }
        return embedding;
    }
} 