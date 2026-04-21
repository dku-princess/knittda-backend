package com.example.knittdaserver.service;

import com.example.knittdaserver.dto.ReportResponse;
import com.example.knittdaserver.entity.Record;
import com.example.knittdaserver.entity.User;
import com.example.knittdaserver.repository.RecordRepository;
import com.example.knittdaserver.repository.UserRepository;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final AuthService authService;
    private final RecordRepository recordRepository;
    private final OpenAiService openAiService;

    public ReportResponse createReport(String token) {
        User user = authService.getUserFromJwt(token);

        // 1️⃣ 요청한 날을 포함해 이전 7일 간의 범위 계산
        LocalDateTime endOfWeek = LocalDateTime.now();
        LocalDateTime startOfWeek = endOfWeek.minusDays(6).withHour(0).withMinute(0).withSecond(0).withNano(0);

        // 2️⃣ 주간 기록 조회
        List<Record> records = recordRepository.findWeeklyRecordsByUserId(
                user.getId(),
                startOfWeek,
                endOfWeek
        );

        // 3️⃣ 통계 계산
        int weeklyKnittingCount = records.size();
        int weeklyKnittingPhotoCount = (int) records.stream()
                .filter(record -> !record.getImages().isEmpty())
                .count();

        double averageProgress = records.stream()
                .mapToInt(record -> record.getRecordStatus().getProgress())
                .average()
                .orElse(0.0);

        // 4️⃣ 가장 많이 사용한 태그 2개
        Map<String, Long> tagFrequency = records.stream()
                .flatMap(record -> record.getTags().stream())
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));

        List<String> topTags = tagFrequency.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .limit(2)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // 5️⃣ 뜨개 단계(cm) 계산
        int recordCountScore = weeklyKnittingCount;
        int photoCountScore = weeklyKnittingPhotoCount;

        int recordFrequencyScore = (int) records.stream()
                .map(record -> record.getCreatedAt().toLocalDate())
                .distinct()
                .count();

        int recordLengthScore = records.stream()
                .mapToInt(record -> record.getComment().length())
                .sum();

        double knittingLevel = Math.round((recordCountScore * 0.3
                + photoCountScore * 0.3
                + recordFrequencyScore * 0.2
                + recordLengthScore * 0.2) * 100.0) / 100.0;

        String hashtagPrompt = String.format("""
                다음 사용자 정보를 참고하여 이번 주 해시태그 키워드와 설명을 각각 3개씩 만들어주세요.

                - 사용자 이름: %s
                - 평균 주간 뜨개 진행도: %.1f%%
                - 주간 뜨개 기록 수: %d
                - 가장 많이 사용한 태그: %s

                아래는 뜨개 성격 유형과 간단한 설명입니다:
                - 꼼꼼파: 꼬 한 개라도 틀리면 바로 풀고 다시 함.
                - 즉흥파: "그냥 막 떠봐~" → 패턴 없이 감으로 진행.
                - 변덕파: 하다가 마음에 안 들면 다시 시작.
                - 스피드파: "이거 하루 만에 가능?" → 빠른 진행.
                - 느긋파: 한 달째 같은 프로젝트.
                - 도전파: 초보인데도 "가디건 도전!" → 도전정신.

                "#해시태그: 설명" 형태로 3줄 출력해주세요.
                """,
                user.getNickname(),
                averageProgress,
                weeklyKnittingCount,
                topTags
        );

        ChatCompletionRequest hashtagRequest = ChatCompletionRequest.builder()
                .model("gpt-4o")
                .messages(List.of(
                        new ChatMessage("system", "너는 뜨개 해시태그 추천 AI야."),
                        new ChatMessage("user", hashtagPrompt)
                ))
                .build();

        String hashtagsRaw = openAiService.createChatCompletion(hashtagRequest)
                .getChoices().get(0).getMessage().getContent().trim();

        // 7️⃣ 해시태그 파싱
        List<ReportResponse.Hashtag> weeklyHashtags = Arrays.stream(hashtagsRaw.split("\n"))
                .map(line -> {
                    String[] parts = line.split(":", 2);
                    String hashtag = parts[0].trim();
                    String description = parts.length > 1 ? parts[1].trim() : "";
                    return new ReportResponse.Hashtag(hashtag, description);
                })
                .collect(Collectors.toList());

        // 8️⃣ 최종 응답 생성
        return ReportResponse.builder()
                .knittingLevel(knittingLevel)
                .weeklyKnittingCount(weeklyKnittingCount)
                .weeklyKnittingPhotoCount(weeklyKnittingPhotoCount)
                .weeklyProgress((int) averageProgress)
                .topTags(topTags)
                .weeklyHashtags(weeklyHashtags)
                .build();
    }
}
