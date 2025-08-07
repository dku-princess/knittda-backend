package com.example.knittdaserver.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.knittdaserver.entity.Project;
import com.example.knittdaserver.entity.ProjectStatus;
import com.example.knittdaserver.entity.Record;
import com.example.knittdaserver.entity.RecordStatus;
import com.example.knittdaserver.entity.User;
import com.example.knittdaserver.dto.QuestionResponse;
import com.example.knittdaserver.dto.RecordDto;
import com.example.knittdaserver.dto.ImageDto;
import com.example.knittdaserver.repository.ProjectRepository;
import jakarta.persistence.EntityNotFoundException;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;

import jakarta.annotation.Generated;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final OpenAiService openAiService;
    private final ProjectRepository projectRepository;
    private static final Logger logger = LoggerFactory.getLogger(QuestionService.class);
    
    /**
     * 각 버전별 프롬프트 전략
     * v1: 기본적인 기록 흐름 인지 + 친근한 톤
     * v2: 전체 기록 분석 + 사실 기반 맥락
     * v3: 최근 5개 기록 중심 + 절제된 톤
     * v4: JSON 형태 데이터 + 구체적 질문 중심
     * v5: 진행률 기반 + 단계별 적절한 포인트
     * v6: 진행 단계별 + 구체적 작업 방향 제시
     * v7: 사용자 컨디션 고려 + 유연한 톤
     * v8: 감정 상태 반영 + 명확한 기록 유도
     */
    private final Map<Integer, Function<Project, String>> promptStrategies = Map.of(
        1, this::generateV1Prompt,
        2, this::generateV2Prompt,
        3, this::generateV3Prompt,
        4, this::generateV4Prompt,
        5, this::generateV5Prompt,
        6, this::generateV6Prompt,
        7, this::generateV7Prompt,
        8, this::generateV8Prompt
    );

    public String generateSingleQuestion(Project project, Integer version) {
        // 기본값은 v1으로 설정
        int targetVersion = (version != null) ? version : 1;
        
        // 해당 버전의 전략이 없으면 v1 사용
        Function<Project, String> strategy = promptStrategies.getOrDefault(targetVersion, this::generateV1Prompt);
        String prompt = strategy.apply(project);
    
        ChatCompletionRequest questionRequest = ChatCompletionRequest.builder()
                .model("gpt-4o")
                .messages(List.of(
                        new ChatMessage("system", "너는 뜨개 기록 작성을 돕는 친근한 질문 생성기야."),
                        new ChatMessage("user", prompt)
                ))
                .temperature(0.8)
                .maxTokens(100)
                .build();
    
        String response = openAiService.createChatCompletion(questionRequest)
                .getChoices().get(0).getMessage().getContent().trim();
        
        // 로그 출력
        logger.info("Generated question for version {} - Project: ID={}, Nickname={}, Design={}, Status={}, LastRecordAt={}, RecordsCount={}", 
            targetVersion, 
            project.getId(), 
            project.getNickname(),
            project.getDesign() != null ? project.getDesign().getTitle() : "null",
            project.getStatus(),
            project.getLastRecordAt(),
            project.getRecords() != null ? project.getRecords().size() : 0
        );
        
        // Record 상세 정보도 로그에 출력
        if (project.getRecords() != null && !project.getRecords().isEmpty()) {
            logger.info("Project {} Records: {}", project.getId(), 
                project.getRecords().stream()
                    .map(record -> String.format("ID=%d, Status=%s, CreatedAt=%s, Tags=%s", 
                        record.getId(), 
                        record.getRecordStatus(), 
                        record.getCreatedAt(),
                        record.getTags()))
                    .toList()
            );
        }
    
        return response;
    }

    /**
     * V1: 기본적인 기록 흐름 인지 + 친근한 톤
     * - 전체 기록의 흐름을 회고하고 작업 부위/기법의 반복/변화에 주목
     * - 잔잔하고 친근한 말투, 시적인 표현 지양
     * - 다음 작업을 넌지시 제안하거나 궁금해하는 톤
     */
    private String generateV1Prompt(Project project) {
        return String.format("""
            당신은 사용자의 뜨개 활동을 함께 지켜본 도우미입니다.  
            아래는 한 사용자가 진행 중인 뜨개 프로젝트와 지금까지의 모든 기록입니다.  
            **기록의 흐름**, **사용된 뜨개 기법**, **작업 부위(예: 소매, 몸통 등)**를 참고하여,   
            사용자가 새로운 기록을 남기기 전에 보여줄 **가이드 문구 한 문장**을 생성해주세요.

            ---

            🧶 문장은 다음 특징을 반드시 만족해야 합니다:
            - 이전 기록들의 흐름을 회고하고, 작업 부위나 기법의 반복 또는 변화에 주목해주세요.
            - 예를 들어 "왼쪽 소매"가 있다면 "오른쪽 소매"를, 특정 뜨개 기법이 자주 등장했다면 그 익숙함을 언급해주세요. 
            - 지나온 작업을 되돌아보고, 다음에 이어질 작업을 넌지시 제안하거나 궁금해하는 톤이면 좋습니다.
            - 감정 표현은 과도하지 않게, **잔잔하고 친근한 말투**로 작성해주세요.   
            - 지나치게 시적인 표현은 피해주세요. 

            - 출력은 반드시 **한 문장만** 해주세요.

            ---

            📦 프로젝트 정보:
            - 프로젝트 이름: %s 
            - 디자인 제목: %s
            - 시작일: %s
            - 목표일: %s
            - 마지막 기록일: %s
            - 현재 상태: %s

            🧵 전체 기록 목록은 함께 제공됩니다.  
            각 기록에는 작성일, 태그, 기록 상태, 코멘트가 포함됩니다.

            ---

            예시 문구:
            - "왼쪽 소매는 마무리되었으니, 이제 오른쪽으로 넘어갈 차례일까요?"
            - "'무늬뜨기' 태그가 자주 보이네요—이제 손에 완전히 익으셨겠죠?"
            - "목둘레 작업을 시작한 지 며칠 되었는데, 점점 형태가 잡혀가고 있을까요?"
            - "처음보다 기록 간격이 더 짧아졌어요, 리듬이 붙은 걸까요?"
            - "'지쳤어요'라는 태그가 많았지만, 그래도 계속해서 실을 잡고 계시네요—대단해요."

            ---

            위의 정보들을 모두 고려해, **기록 흐름을 인지한 듯한 한 문장짜리 가이드 문구**를 생성해주세요. 
            """,
            project.getNickname() != null ? project.getNickname() : "제목 없음",
            getDesignTitle(project),
            getDate(project.getStartDate()),
            getDate(project.getGoalDate()),
            getDateTime(project.getLastRecordAt()),
            project.getStatus() != null ? project.getStatus().name() : "없음"
        );
    }

    private String getDesignTitle(Project project) {
        return project.getDesign() != null ? project.getDesign().getTitle() : "디자인 없음";
    }

    private String getDate(java.time.LocalDate date) {
        return date != null ? date.toString() : "없음";
    }

    private String getDateTime(java.time.LocalDateTime dateTime) {
        return dateTime != null ? dateTime.toString() : "없음";
    }

    /**
     * V2: 전체 기록 분석 + 사실 기반 맥락
     * - 전체 기록 흐름과 뜨개 진행 부위/기법을 인지한 문장
     * - COMPLETED 상태일 때 완성 축하/마무리 느낌의 문장
     * - 오랜 시간 경과 시 "오랜만이에요" 같은 언급 포함 가능
     * - 사실 기반 + 따뜻한 톤
     */
    private String generateV2Prompt(Project project) {
        return String.format("""
            당신은 사용자의 뜨개 활동을 함께 지켜본 도우미입니다.  
            아래는 한 사용자가 진행 중인 뜨개 프로젝트와 지금까지의 모든 기록입니다.  
            **작업 흐름**, **기법**, **기록 상태**, **기록 간격** 등을 참고하여,  
            사용자가 새로운 기록을 남기기 전에 보여줄 **맥락에 맞는 가이드 문구 한 문장**을 생성해주세요.

            ---

            🧶 문장은 다음 특징을 반드시 포함해야 합니다:
            - 작품의 **전체 기록 흐름**과 뜨개 진행 부위/기법(예: 소매, 몸통, 목둘레, 무늬뜨기 등)을 인지한 문장이어야 합니다.
            - 작품의 상태가 `COMPLETED`일 경우, 진행 질문은 하지 말고 **완성을 축하하거나 마무리 느낌의 문장**으로 작성해주세요.
            - `lastRecordAt` 이후 시간이 오래 지났다면, **"오랜만이에요", "6월 이후 처음이에요"**, 같은 언급을 포함해도 좋습니다.
            - 감정적 표현보다는 **사실 기반 + 따뜻한 톤**으로 작성해주세요.
            - 문장은 반드시 **하나만** 출력해주세요.

            ---

            📦 프로젝트 정보:
            - 프로젝트 이름: %s
            - 디자인 제목: %s
            - 시작일: %s
            - 목표일: %s
            - 마지막 기록일: %s
            - 현재 상태: %s (RecordStatus enum: NOT_STARTED, STARTED, IN_PROGRESS, ALMOST_DONE, COMPLETED)

            전체 기록 목록 예시: %s
            

            🧵 전체 기록 목록은 함께 제공됩니다.  
            각 기록에는 작성일, 태그(예: 왼쪽소매, 무늬뜨기 등), 기록 상태, 코멘트가 포함됩니다.

            ---

            예시 문구:
            - "왼쪽 소매 이후 기록이 없었는데, 오늘은 다시 바늘을 드셨네요."
            - "무늬뜨기를 반복하며 여기까지 오셨군요, 이제 마무리가 가까워 보입니다."
            - "6월 10일 이후 처음 남기는 기록이네요, 다시 시작해주셔서 반갑습니다."
            - "이 프로젝트는 이미 완성하셨네요—다음 작품은 어떤 걸 계획 중이신가요?"
            - "거의 한 달 만에 '첫 스웨터'로 돌아오셨네요, 손이 기억하고 있었을까요?"

            ---

            위 정보를 참고하여, **사실 기반 맥락 + 흐름 인지를 담은 한 문장**을 생성해주세요.
            """,
            project.getNickname() != null ? project.getNickname() : "제목 없음",
            getDesignTitle(project),
            getDate(project.getStartDate()),
            getDate(project.getGoalDate()),
            getDateTime(project.getLastRecordAt()),
            project.getStatus() != null ? project.getStatus().name() : "없음",
            formatAllRecords(project.getRecords())
        );
    }

    /**
     * 모든 기록을 포맷팅하여 문자열로 반환
     */
    private String formatAllRecords(List<Record> records) {
        if (records == null || records.isEmpty()) {
            return "기록 없음";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < records.size(); i++) {
            Record record = records.get(i);
            sb.append(String.format("기록 %d:\n", i + 1));
            sb.append(String.format("- 작성일: %s\n", getDateTime(record.getCreatedAt())));
            sb.append(String.format("- 상태: %s\n", record.getRecordStatus() != null ? record.getRecordStatus().name() : "상태 없음"));
            sb.append(String.format("- 태그: %s\n", record.getTags() != null ? String.join(", ", record.getTags()) : "태그 없음"));
            sb.append(String.format("- 내용: %s\n", record.getComment() != null ? record.getComment() : "내용 없음"));
            sb.append("\n");
        }
        
        return sb.toString();
    }

    /**
     * V3: 최근 5개 기록 중심 + 절제된 톤
     * - 최근 5개 기록만 분석하여 맥락 기반 가이드 문장 생성
     * - COMPLETED 상태일 때 완성 축하/마무리 느낌의 문장
     * - 오랜 시간 경과 시 자연스러운 시간 언급 포함 가능
     * - 감정 표현은 절제된 톤, 사실 기반 + 따뜻한 문장
     */
    private String generateV3Prompt(Project project) {
        List<Record> records = project.getRecords();
        
        // 최근 5개 기록만 가져오기
        List<Record> recentRecords = (records != null) 
            ? records.stream()
                     .sorted(Comparator.comparing(Record::getCreatedAt).reversed())
                     .limit(5)
                     .toList()
            : List.of();

        return String.format("""
            당신은 사용자의 뜨개 활동을 함께 지켜본 도우미입니다.  
            아래는 한 사용자가 진행 중인 뜨개 프로젝트와 지금까지의 기록입니다.  
            **작업 흐름**, **기법**, **기록 상태**, **기록 간격**, **마지막 기록일** 등을 참고하여,  
            사용자가 새로운 기록을 남기기 전에 보여줄 **맥락 기반의 가이드 문장 한 줄**을 생성해주세요.

            ---

            🧶 문장의 조건은 다음과 같습니다:

            - 작품의 전체 기록 흐름을 파악한 듯한 문장이어야 합니다.  
            - 뜨개 진행 부위(예: 소매, 몸통, 목둘레)나 기법(예: 무늬뜨기, 코 줄이기 등)을 언급할 수 있습니다.
            - 상태가 `COMPLETED`이면 작업 질문은 하지 말고, **완성 축하 또는 마무리 느낌의 문장**으로 작성해주세요.
            - 마지막 기록일이 오래 전이면, "오랜만이에요", "6월 이후 처음이에요"와 같은 자연스러운 시간 언급을 포함할 수 있습니다.
            - **감정 표현은 절제된 톤**, **사실 기반 + 따뜻한 문장**으로 작성해주세요.
            - 문장은 반드시 **하나만 출력**해주세요.  
              ※ 마침표도 **한 번만** 사용해주세요.

            ---

            📦 프로젝트 정보:
            - 프로젝트 이름: %s
            - 디자인 제목: %s
            - 시작일: %s
            - 목표일: %s
            - 마지막 기록일: %s
            - 현재 상태: %s
              (설명: NOT_STARTED=기록 없음, STARTED=시작함, IN_PROGRESS=진행 중, ALMOST_DONE=거의 완료, COMPLETED=완료됨)

            ---

            🧵 최근 기록 목록 (최대 5개):
            %s

            ---

            예시 문장:
            - "왼쪽 소매 이후 기록이 없었는데, 오늘은 다시 바늘을 드셨네요."
            - "무늬뜨기를 반복하며 여기까지 오셨군요, 이제 마무리가 가까워 보입니다."
            - "6월 10일 이후 처음 남기는 기록이네요, 다시 시작해주셔서 반갑습니다."
            - "이 프로젝트는 이미 완성하셨네요—다음 작품은 어떤 걸 계획 중이신가요?"
            - "거의 한 달 만에 '첫 스웨터'로 돌아오셨네요, 손이 기억하고 있었을까요?"

            ---

            📌 위 정보를 참고하여, **작품 흐름을 반영한 자연스러운 문장 한 줄만** 생성해주세요.
            """,
            project.getNickname() != null ? project.getNickname() : "제목 없음",
            getDesignTitle(project),
            getDate(project.getStartDate()),
            getDate(project.getGoalDate()),
            getDateTime(project.getLastRecordAt()),
            project.getStatus() != null ? project.getStatus().name() : "없음",
            formatRecentRecords(recentRecords)
        );
    }

    /**
     * 최근 기록들을 포맷팅하여 문자열로 반환
     */
    private String formatRecentRecords(List<Record> records) {
        if (records == null || records.isEmpty()) {
            return "기록 없음";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < records.size(); i++) {
            Record record = records.get(i);
            sb.append(String.format("%d. 작성일: %s\n", i + 1, getDateTime(record.getCreatedAt())));
            sb.append(String.format("   - 상태: %s, 태그: %s, 내용: %s\n\n", 
                record.getRecordStatus() != null ? record.getRecordStatus().name() : "상태 없음",
                record.getTags() != null ? String.join(", ", record.getTags()) : "태그 없음",
                record.getComment() != null ? record.getComment() : "내용 없음"));
        }
        
        return sb.toString();
    }

    /**
     * V4: JSON 형태 데이터 + 구체적 질문 중심
     * - JSON 형태로 구조화된 데이터 제공
     * - daysAgo가 4 이상일 때만 "오랜만이에요" 표현 사용
     * - totalRecords가 1일 때만 "첫 업데이트" 표현 사용
     * - 직전 기록의 tags, comment, recordStatus를 짚어주고 이어질 작업을 질문형으로 제안
     * - 과도한 감성표현 대신 도움이 되는 구체적 질문 목표
     */
    private String generateV4Prompt(Project project) {
        List<Record> records = project.getRecords();
        int totalRecords = records != null ? records.size() : 0;

        // 가장 최근 기록
        Record last = (records == null || records.isEmpty()) ? null
            : records.stream().max(Comparator.comparing(Record::getCreatedAt)).orElse(null);

        // 마지막 기록과 현재 시점 차이
        long daysSinceLast = (last == null) ? -1 :
            ChronoUnit.DAYS.between(last.getCreatedAt(), LocalDateTime.now());

        return String.format("""
            당신은 사용자의 뜨개 활동 흐름을 함께 지켜본 도우미입니다.
            아래 JSON 형태의 '사실 데이터'를 읽고, **새 기록 작성을 돕는 구체적 질문 한 문장**을 생성해주세요.

            ```json
            {
              "projectName": "%s",
              "designTitle": "%s",
              "status": "%s",
              "totalRecords": %d,
              "lastRecord": {
                "createdAt": "%s",
                "daysAgo": %d,
                "tags": "%s",
                "comment": "%s",
                "recordStatus": "%s"
              }
            }
            ```
            ⚙️ 규칙

            반드시 문장은 하나.

            '오랜만이에요'/'다시 돌아오셨군요' 같은 표현은 daysAgo 가 '4 이상'일 때만 사용.

            '첫 업데이트' 라는 말은 totalRecords 가 1일 때만 사용.

            직전 기록의 tags, comment 또는 recordStatus를 짚어 준 뒤, 이어질 작업을 질문형으로 제안.

            과도한 감성표현 대신 '도움이 되는 구체적 질문'(예: "몸통 늘림 부분에서 실 장력을 일정하게 유지하셨나요?")을 목표로.

            출력 ➜ 질문 문장 하나만.
            """,
            project.getNickname() != null ? project.getNickname() : "제목 없음",
            getDesignTitle(project),
            project.getStatus() != null ? project.getStatus().name() : "없음",
            totalRecords,
            last != null ? getDateTime(last.getCreatedAt()) : "없음",
            last != null ? daysSinceLast : -1,
            last != null && last.getTags() != null ? String.join(", ", last.getTags()) : "태그 없음",
            last != null && last.getComment() != null ? last.getComment() : "내용 없음",
            last != null && last.getRecordStatus() != null ? last.getRecordStatus().name() : "상태 없음"
        );
    }

    /**
     * V5: 진행률 기반 + 단계별 적절한 포인트
     * - 진행률(0/25/50/75/100)에 맞춰 초반/중반/후반/완성 단계별로 적절한 포인트 짚어주기
     * - daysAgo ≥ 4일일 때만 "오랜만이에요/다시 돌아오셨군요" 표현 사용
     * - totalRecords == 1일 때만 "첫 업데이트" 표현 사용
     * - 직전 기록의 tags·comment·recordStatus를 언급하며 이어질 작업을 질문형으로 제안
     * - 과도한 감성 대신 실제 도움이 되는 구체적 질문 목표
     */
    private String generateV5Prompt(Project project) {
        List<Record> records = project.getRecords();
        int totalRecords = (records != null) ? records.size() : 0;

        Record last = (records == null || records.isEmpty())
            ? null
            : records.stream()
                     .max(Comparator.comparing(Record::getCreatedAt))
                     .orElse(null);

        long daysSinceLast = (last == null)
            ? -1
            : ChronoUnit.DAYS.between(last.getCreatedAt(), LocalDateTime.now());

        String stage = "알 수 없음";
        int progress = -1;
        if (last != null) {
            switch (last.getRecordStatus()) {
                case NOT_STARTED:
                    stage = "완전 초반";
                    progress = 0;
                    break;
                case STARTED:
                    stage = "초반";
                    progress = 25;
                    break;
                case IN_PROGRESS:
                    stage = "중반";
                    progress = 50;
                    break;
                case ALMOST_DONE:
                    stage = "후반";
                    progress = 75;
                    break;
                case COMPLETED:
                    stage = "완성";
                    progress = 100;
                    break;
                default:
                    stage = "알 수 없음";
                    progress = -1;
                    break;
            }
        }
        return String.format("""
            당신은 사용자의 뜨개 프로젝트 흐름을 함께 지켜본 도우미입니다.
            아래 JSON 데이터를 읽고 **새 기록 작성을 돕는 구체적 질문 한 문장**을 한국어로 작성하세요.

            ```json
            {
              "projectName": "%s",
              "designTitle": "%s",
              "progressPercent": %d,
              "totalRecords": %d,
              "lastRecord": {
                "createdAt": "%s",
                "daysAgo": %d,
                "tags": "%s",
                "comment": "%s",
                "recordStatus": "%s"
              }
            }

            ⚙️ 규칙

            반드시 질문은 한 문장.

            "오랜만이에요/다시 돌아오셨군요" 표현은 daysAgo ≥ 4일일 때만.

            "첫 업데이트" 표현은 totalRecords == 1일 때만.

            직전 기록의 tags·comment·recordStatus를 언급하며 이어질 작업을 질문형으로 제안.

            progressPercent에 맞춰 초반(0-25), 중반(50), 후반(75), 완성(100) 단계별로 적절한 포인트를 짚어주세요.

            과도한 감성 대신 실제 도움이 되는 구체적 질문을 목표로.

            출력 ➜ 질문 문장 하나만.
            """,
            project.getNickname() != null ? project.getNickname() : "제목 없음",
            project.getDesign() != null ? project.getDesign().getTitle() : "디자인 없음",
            progress,
            totalRecords,
            last != null ? last.getCreatedAt().toString() : "없음",
            last != null ? daysSinceLast : -1,
            last != null && last.getTags() != null ? String.join(", ", last.getTags()) : "태그 없음",
            last != null && last.getComment() != null ? last.getComment() : "내용 없음",
            last != null && last.getRecordStatus() != null ? last.getRecordStatus().name() : "상태 없음"
        );
    }

    /**
     * V6: 진행 단계별 + 구체적 작업 방향 제시
     * - 진행 단계(초반/중반/후반/완성)에 어울리는 구체 작업을 물어보기
     * - 날짜·기간·업데이트 횟수는 절대 언급하지 않음
     * - 작품의 진행 단계(stage)와 lastRecord.tags·comment·recordStatus를 활용해 구체적인 작업 방향이나 세부점을 물어봄
     * - 단순한 응원·격려 문구는 피하고, 실제 기록 내용을 유도하는 실질적 가이드가 되도록 함
     */
    private String generateV6Prompt(Project project) {
        /* ------------- 최근 기록·진행 단계 파악 ------------- */
        List<Record> records = project.getRecords();
        Record last = (records == null || records.isEmpty())
            ? null
            : records.stream()
                     .max(Comparator.comparing(Record::getCreatedAt))
                     .orElse(null);

        // 진행 단계 이름 매핑 (RecordStatus enum ➞ 초반/중반/후반/완성)
        String stage;
        if (last != null) {
            switch (last.getRecordStatus()) {
                case NOT_STARTED -> stage = "완전 초반";
                case STARTED -> stage = "초반";
                case IN_PROGRESS -> stage = "중반";
                case ALMOST_DONE -> stage = "후반";
                case COMPLETED -> stage = "완성";
                default -> stage = "알 수 없음";
            }
        } else {
            stage = "알 수 없음";
        }

        /* ------------- 프롬프트 ------------- */
        return String.format("""
            당신은 사용자의 뜨개 프로젝트 과정을 함께 지켜본 조력자입니다.  
            아래 JSON 데이터를 참고하여 **사용자가 새 기록을 작성할 때 도움이 될 '질문형 문장' 한 개**를 한국어로 생성하세요.

            ```json
            {
              "projectName": "%s",
              "designTitle": "%s",
              "stage": "%s",
              "lastRecord": {
                "tags": "%s",
                "comment": "%s",
                "recordStatus": "%s"
              }
            }
            ⚙️ 작성 규칙

            반드시 한 문장이고 질문형이어야 합니다.

            날짜·기간·업데이트 횟수("오랜만"·"첫 업데이트" 같은 표현)는 절대 언급하지 마세요.

            작품의 진행 단계(stage) 와 lastRecord.tags‧comment‧recordStatus 를 활용해, 구체적인 작업 방향이나 세부점을 물어봐 주세요.

            단순한 응원·격려 문구는 피하고, 실제 기록 내용을 유도하는 실질적 가이드가 되도록 합니다.

            예시)

            "왼쪽 소매를 마무리하셨다면 오늘 기록에는 오른쪽 소매 게이지를 어떻게 잡으셨는지 적어보실래요?"

            "무늬뜨기 패턴을 반복 중인데, 이번 단에서는 실 장력이 일정했는지 기록해볼까요?"

            출력 → 질문형 문장 1개만.
            """,
            project.getNickname() != null ? project.getNickname() : "제목 없음",
            project.getDesign() != null ? project.getDesign().getTitle() : "디자인 없음",
            stage,
            (last != null && last.getTags() != null) ? String.join(", ", last.getTags()) : "태그 없음",
            (last != null && last.getComment() != null) ? last.getComment() : "내용 없음",
            (last != null && last.getRecordStatus() != null) ? last.getRecordStatus().name() : "상태 없음"
        );
    }

    /**
     * V7: 사용자 컨디션 고려 + 유연한 톤
     * - 사용자의 상태(피곤, 지침, 집중력 높음 등)를 고려하여 유연하게 맞춘 질문
     * - "지치는 날도 있지만...", "혹시 오늘은..."처럼 사용자의 상태를 먼저 이해하고 시작하는 표현
     * - 감정적으로 지나치게 밝거나 일률적인 격려는 지양
     * - 진행 흐름, 작업 부위, 이전 기록을 참고하여 맥락 기반 질문
     * - 관찰형/질문형/중립형 문장 스타일 우선 사용
     */
    private String generateV7Prompt(Project project) {
        return String.format("""
            당신은 사용자의 뜨개 활동을 함께 지켜본 도우미입니다.  
            아래는 한 사용자가 진행 중인 뜨개 프로젝트와 지금까지의 기록입니다.  
            사용자의 상태는 언제든 다양할 수 있으며, 피곤하거나 지친 상태, 혹은 집중력이 높은 상태일 수도 있습니다.  
            따라서 지금 남길 새로운 기록에 앞서, 사용자의 컨디션과 흐름에 **유연하게 맞춘 질문 한 문장**을 제안해주세요.

            ---

            🧶 문장의 조건은 다음과 같습니다:

            - 질문은 반드시 **유연한 톤**을 유지해주세요.  
            예: "지치는 날도 있지만...", "혹시 오늘은..."처럼 사용자의 상태를 먼저 이해하고 시작하는 표현.
            - 감정적으로 지나치게 밝거나 일률적인 격려는 지양해주세요.
            - 진행 흐름, 작업 부위, 이전 기록을 참고하여 **맥락 기반** 질문을 만들어주세요.
            - 표현 방식은 자유롭지만, **관찰형 / 질문형 / 중립형 문장 스타일**을 우선 사용해주세요.
            - 문장은 반드시 **하나만 출력**해야 합니다.
            - 마침표는 **한 번만 사용**해주세요.

            ---

            📦 프로젝트 정보:
            - 프로젝트 이름: %s
            - 디자인 제목: %s
            - 시작일: %s
            - 목표일: %s
            - 마지막 기록일: %s
            - 현재 상태: %s (설명: NOT_STARTED=기록 없음, STARTED=시작함, IN_PROGRESS=진행 중, ALMOST_DONE=거의 완료, COMPLETED=완료됨)

            🧵 최근 기록 3개:
            %s

            ---

            예시 문장:
            - "조금 지치는 날일 수도 있지만, 실을 다시 들어주셔서 고마워요—이번엔 어느 부분을 이어가셨나요?"
            - "오랜만에 바늘을 드셨네요, 오늘은 어떤 느낌으로 시작하셨을까요?"
            - "무늬뜨기가 반복되는 시기인데, 혹시 지루함보다는 익숙함이 생기고 있진 않을까요?"
            - "최근 기록과 비교해보면 속도가 꽤 빨라졌어요—혹시 이번엔 집중해서 하신 걸까요?"

            ---

            📌 위 정보를 참고하여, **유연하고 따뜻한 질문 한 문장**을 생성해주세요.
            """,
            project.getNickname() != null ? project.getNickname() : "제목 없음",
            getDesignTitle(project),
            getDate(project.getStartDate()),
            getDate(project.getGoalDate()),
            getDateTime(project.getLastRecordAt()),
            project.getStatus() != null ? project.getStatus().name() : "없음",
            formatRecentRecords(project.getRecords(), 3)
        );
    }

    /**
     * 최근 기록들을 포맷팅하여 문자열로 반환 (limit 개수만큼)
     */
    private String formatRecentRecords(List<Record> records, int limit) {
        if (records == null || records.isEmpty()) {
            return "기록 없음";
        }
        
        return records.stream()
            .sorted(Comparator.comparing(Record::getCreatedAt).reversed())
            .limit(limit)
            .map(record -> String.format("- %s / 태그: %s / 내용: %s",
                getDateTime(record.getCreatedAt()),
                record.getTags() != null ? String.join(", ", record.getTags()) : "없음",
                record.getComment() != null ? record.getComment() : "없음"))
            .collect(Collectors.joining("\n"));
    }

    /**
     * V8: 감정 상태 반영 + 명확한 기록 유도
     * - stage(초반/중반/후반/완성)에 어울리는 구체 작업을 물어보기
     * - 날짜·기간·업데이트 횟수는 절대 언급하지 않음
     * - lastRecord.comment의 감정이 부정("지쳤", "힘들")이라면 어조를 살짝 부드럽게, 긍정/중립이면 바로 작업 팁을 제안
     * - 단순 응원으로 끝내지 말고 "어떤 내용을 기록하면 좋을지"를 명확히 유도
     */
    private String generateV8Prompt(Project project) {
        /* ── 최근 기록 및 진행 단계 파악 ─────────────────────── */
        List<Record> records = project.getRecords();
        Record last = (records == null || records.isEmpty())
            ? null
            : records.stream()
                     .max(Comparator.comparing(Record::getCreatedAt))
                     .orElse(null);

        // 진행 퍼센트(0/25/50/75/100) → 단계 라벨
        String stage = switch (last.getRecordStatus()) {
            case RecordStatus.NOT_STARTED, RecordStatus.STARTED     -> "초반";
            case RecordStatus.IN_PROGRESS             -> "중반";
            case RecordStatus.ALMOST_DONE             -> "후반";
            case RecordStatus.COMPLETED               -> "완성";
            default                      -> "알 수 없음";
        };

        /* ── 프롬프트 ─────────────────────────────────────────── */
        return String.format("""
            당신은 사용자의 뜨개 프로젝트 과정을 지켜본 조력자입니다.  
            아래 JSON 정보를 이해하고, **사용자가 기록을 작성할 때 도움이 될 '질문형 문장' 딱 1개**를 한국어로 생성하세요.

            ```json
            {
              "projectName": "%s",
              "designTitle": "%s",
              "stage": "%s",
              "totalRecords": %d,
              "lastRecord": {
                "tags": "%s",
                "comment": "%s",
                "recordStatus": "%s"
              }
            }
            ⚙️ 작성 규칙

            반드시 한 문장이며 질문형이어야 합니다.

            날짜·기간·업데이트 횟수(예: "오랜만", "첫 업데이트")는 절대 언급하지 마세요.

            stage(초반/중반/후반/완성)에 어울리는 구체 작업을 물어보세요.

            lastRecord.comment의 감정이 부정("지쳤", "힘들")이라면 어조를 살짝 부드럽게,
            긍정/중립이면 바로 작업 팁을 제안하세요.

            단순 응원으로 끝내지 말고 **"어떤 내용을 기록하면 좋을지"**를 명확히 유도하세요.

            출력 ➜ 질문형 문장 1개만.
            """,
            project.getNickname() != null ? project.getNickname() : "제목 없음",
            (project.getDesign() != null ? project.getDesign().getTitle() : "디자인 없음"),
            stage,
            (records != null ? records.size() : 0),
            (last != null && last.getTags() != null) ? String.join(", ", last.getTags()) : "태그 없음",
            (last != null && last.getComment() != null) ? last.getComment() : "내용 없음",
            (last != null && last.getRecordStatus() != null) ? last.getRecordStatus().name() : "상태 없음"
        );
    }

    /**
     * 프로젝트 ID를 기반으로 모든 버전의 질문과 프로젝트 정보를 조회
     */
    public QuestionResponse getAllVersionsQuestions(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("해당 프로젝트가 존재하지 않습니다."));

        // 버전 1~8까지의 질문 생성
        Map<Integer, String> questions = new HashMap<>();
        for (int version = 1; version <= 8; version++) {
            try {
                String question = generateSingleQuestion(project, version);
                questions.put(version, question);
                logger.info("Generated question for version {}: {}", version, question);
            } catch (Exception e) {
                logger.error("Failed to generate question for version {}: {}", version, e.getMessage());
                questions.put(version, "질문 생성에 실패했습니다.");
            }
        }

        // Record를 RecordDto로 변환
        List<RecordDto> recordDtos = project.getRecords().stream()
            .map(record -> RecordDto.from(record))
            .collect(Collectors.toList());

        return QuestionResponse.builder()
                .projectId(project.getId())
                .projectName(project.getNickname())
                .designTitle(project.getDesign() != null ? project.getDesign().getTitle() : "디자인 없음")
                .projectStatus(project.getStatus() != null ? project.getStatus().name() : "상태 없음")
                .records(recordDtos)
                .questions(questions)
                .build();
    }
}