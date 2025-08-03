package com.example.knittdaserver.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.knittdaserver.entity.Project;
import com.example.knittdaserver.entity.Record;
import com.example.knittdaserver.entity.User;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;

import jakarta.annotation.Generated;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final OpenAiService openAiService;
    private static final Logger logger = LoggerFactory.getLogger(QuestionService.class);
    
    private final Map<Integer, Function<Project, String>> promptStrategies = Map.of(
        1, this::generateV1Prompt,
        2, this::generateV2Prompt,
        3, this::generateV3Prompt,
        4, this::generateV4Prompt,
        5, this::generateV5Prompt,
        6, this::generateV6Prompt
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

    private String generateV1Prompt(Project project) {
        return String.format("""
            다음은 뜨개 프로젝트 정보입니다:
            - 프로젝트 제목: %s
            - 디자인 제목: %s
            - 시작일: %s
            - 마지막 기록일: %s
            - 현재 상태: %s
            - 닉네임(선물 대상 또는 목적): %s
            - 목표일: %s
    
            위 프로젝트를 바탕으로, 사용자가 오늘의 뜨개 기록을 쉽게 쓸 수 있도록
            감성적이고 친근한 질문 하나만 만들어주세요.
            반드시 **한 문장**이어야 하며, 출력은 질문 문장 **하나만** 해주세요.
            """,
            project.getDesign() != null ? project.getDesign().getTitle() : "제목 없음",
            project.getDesign() != null ? project.getDesign(): "디자인 없음",
            project.getStartDate() != null ? project.getStartDate().toString() : "없음",
            project.getLastRecordAt() != null ? project.getLastRecordAt().toString() : "없음",
            project.getStatus() != null ? project.getStatus().name() : "없음",
            project.getNickname(),
            project.getGoalDate() != null ? project.getGoalDate().toString() : "없음"
        );
    }

    private String generateV2Prompt(Project project) {
        return String.format("""
당신은 사용자의 뜨개 활동을 함께 지켜본 도우미입니다.
아래는 한 사용자가 진행 중인 작품과 그에 대한 전체 기록들입니다.
이 데이터를 바탕으로, 사용자가 새 기록을 작성할 때 보여줄 **가이드 문구 한 문장**을 생성해주세요.

💡 문장의 특징:
- 특정 작품의 진행 흐름을 알고 있는 듯한 맥락 기반 문장입니다.
- 이전 기록의 흐름과 비교하거나 이어지는 질문을 포함할 수 있습니다.
- 이어질 작업을 암시하거나, 자연스럽게 리마인드하거나, 응원의 말을 전할 수도 있습니다.
- 너무 감성적이기보다는, 사용자의 구체적인 작업 흐름을 함께하고 있는 듯한 인상을 줍니다.
- 문장은 하나만 출력해주세요.

---

📦 프로젝트 정보:
- 프로젝트 제목(선물 대상 또는 별명): %s
- 디자인 제목: %s
- 시작일: %s
- 목표일: %s
- 마지막 기록일: %s
- 현재 상태: %s

🧵 전체 기록 목록은 서버에서 함께 제공됩니다.

---

예시 문구:
- "왼쪽 소매를 다 떴다면, 오늘은 오른쪽 소매에 도전해볼까요?"
- "지난 기록 이후 10일 만에 '첫 스웨터' 작업을 다시 시작하셨네요!"
- "'무늬뜨기' 태그가 자주 보이네요. 이번에도 같은 패턴을 사용하셨나요?"
- "이제 몸통만 남은 것 같아요. 완성이 가까워지고 있어요!"
- "처음 올린 사진과 비교해보면 정말 많이 진행됐어요."

---

이 모든 정보를 참고하여, 작품 전체 흐름을 인지한 듯한 **한 문장짜리 가이드 문구**를 생성해주세요.
""",
            project.getNickname() != null ? project.getNickname() : "제목 없음",
            project.getDesign() != null ? project.getDesign().getTitle() : "디자인 없음",
            project.getStartDate() != null ? project.getStartDate().toString() : "없음",
            project.getGoalDate() != null ? project.getGoalDate().toString() : "없음",
            project.getLastRecordAt() != null ? project.getLastRecordAt().toString() : "없음",
            project.getStatus() != null ? project.getStatus().name() : "없음"
        );
    }

    private String generateV3Prompt(Project project) {
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

    private String generateV4Prompt(Project project) {
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

    private String generateV5Prompt(Project project) {
        // User 정보를 가져오기 위해 project.getUser() 사용
        User user = project.getUser();
        return String.format("""
            당신은 사용자의 뜨개 활동을 함께 지켜본 도우미입니다.  
            아래는 사용자가 진행 중인 뜨개 프로젝트, 최근 기록 정보, 그리고 사용자 기본 정보입니다.  
            **기록의 흐름**, **직전 기록의 내용**, **사용자의 이름**, **프로젝트 상태** 등을 참고하여,  
            사용자가 새 기록을 남기기 전에 보여줄 **맥락에 맞는 가이드 문구 한 문장**을 생성해주세요.

            ---

            🧶 문장은 다음 요소를 반드시 고려해주세요:
            - 작품의 전체 흐름과 뜨개 진행 부위/기법(예: 소매, 몸통, 무늬뜨기 등)을 인지한 문장
            - 최근 기록의 태그, 작성일, 기록 상태, 코멘트를 기반으로 자연스럽게 이어지는 흐름
            - `RecordStatus.COMPLETED`인 경우, **완성 축하 or 다음 계획 유도**로 마무리
            - 마지막 기록 이후 시간이 길게 지났다면, **"오랜만이에요", 날짜 언급**을 자연스럽게 포함 가능
            - 사용자 이름(닉네임)을 활용하여 **개인 맞춤형으로 말 걸 듯** 전달
            - 이번 기록이 **작은 한 걸음이자 의미 있는 연결**이라는 느낌을 전달해도 좋음
            - 감정 표현은 절제하고, 따뜻하고 사실적인 톤으로 작성해주세요
            - 반드시 문장은 **하나만 출력**해주세요

            ---

            👤 사용자 정보:
            - 닉네임: %s

            📦 프로젝트 정보:
            - 프로젝트 이름: %s
            - 디자인 제목: %s
            - 시작일: %s
            - 목표일: %s
            - 마지막 기록일: %s
            - 현재 상태: %s (RecordStatus enum: NOT_STARTED, STARTED, IN_PROGRESS, ALMOST_DONE, COMPLETED)

            📝 직전 기록 정보:
            - 작성일: %s
            - 태그: %s
            - 기록 상태: %s
            - 기록 내용: %s

            🧵 전체 기록 목록도 함께 제공됩니다.

            ---

            예시 문구:
            - "아미님, 왼쪽 소매 이후 3주 만에 다시 만나는 '첫 스웨터'네요—이 연결이 참 반갑습니다."
            - "무늬뜨기 태그가 이어지고 있어요, 이번엔 손끝이 조금 더 익숙해졌을까요?"
            - "이 프로젝트는 이미 완성하셨네요—정성껏 쌓아온 기록들이 멋진 마무리를 증명해줘요."
            - "5월 18일 기록 이후 처음이에요. 잠시 멈췄다가도 다시 이어가주셔서 정말 멋져요!"
            - "이번 한 문장이 또 한 조각의 실처럼 이 프로젝트를 엮어갈 거예요."

            ---

            위 정보를 참고하여, **맥락 기반 + 직전 기록 반영 + 사용자 맞춤 응원**이 담긴 **한 문장 가이드 문구**를 생성해주세요.
            """,
            user != null ? user.getNickname() : "사용자",
            project.getNickname() != null ? project.getNickname() : "제목 없음",
            getDesignTitle(project),
            getDate(project.getStartDate()),
            getDate(project.getGoalDate()),
            getDateTime(project.getLastRecordAt()),
            project.getStatus() != null ? project.getStatus().name() : "없음",
            project.getLastRecordAt() != null ? getDateTime(project.getLastRecordAt()) : "없음",
            "태그 정보 없음", // 임시 태그 정보
            "상태 정보 없음", // 임시 상태 정보
            "기록 내용 없음"  // 임시 기록 내용
        );
    }

    private String generateV6Prompt(Project project) {
        return String.format("""
당신은 사용자의 뜨개 활동을 함께 지켜본 따뜻한 응원가입니다.  
사용자가 이전에 남긴 기록들의 코멘트를 바탕으로,  
새로운 기록을 남기도록 **따뜻하게 응원하는 문구 한 문장**을 생성해주세요.

---

💝 응원 문구의 특징:
- 이전 기록들의 코멘트 내용을 참고하여 **개인적인 응원**을 전달해주세요.
- 사용자가 언급한 어려움, 성취감, 감정 등을 인지하고 **공감과 격려**를 담아주세요.
- "힘들었지만 계속 해주셔서 대단해요", "이런 느낌을 공유해주셔서 감사해요" 같은 **감정적 연결**을 표현해주세요.
- 기록 자체의 가치와 의미를 강조하여 **기록 작성 동기**를 부여해주세요.
- 너무 과도하지 않고 **자연스럽고 따뜻한 톤**으로 작성해주세요.
- 반드시 **한 문장**으로 출력해주세요.

---

📦 프로젝트 정보:
- 프로젝트 이름: %s
- 디자인 제목: %s
- 시작일: %s
- 마지막 기록일: %s
- 현재 상태: %s

📝 전체 기록 목록:
%s

---

예시 응원 문구:
- "매번 솔직하게 느낀 점을 나눠주셔서, 오늘도 어떤 이야기가 나올지 기대돼요."
- "힘들 때도 포기하지 않고 기록을 남겨주셨던 그 마음이, 오늘도 빛날 거예요."
- "작은 진전도 소중하게 기록해주시는 모습이 정말 멋져요—오늘도 한 걸음 더 나아가실 거예요."
- "기록을 통해 뜨개와의 관계가 더 깊어지고 있는 것 같아요, 오늘도 특별한 순간을 만들어주세요."
- "매번 새로운 시도와 도전을 기록해주시는 모습이 영감을 줘요—오늘도 새로운 이야기가 기다리고 있어요."

---

위 정보를 참고하여, **이전 기록들의 코멘트를 바탕으로 한 따뜻한 응원 문구**를 생성해주세요.
""",
            project.getNickname() != null ? project.getNickname() : "제목 없음",
            getDesignTitle(project),
            getDate(project.getStartDate()),
            getDateTime(project.getLastRecordAt()),
            project.getStatus() != null ? project.getStatus().name() : "없음",
            formatAllRecords(project.getRecords())
        );
    }

    /**
     * User와 Record 정보를 포함한 v5 프롬프트 생성
     */
    public String generateContextualGuidePromptWithUserInfo(Project project, User user, Record lastRecord) {
        return String.format("""
당신은 사용자의 뜨개 활동을 함께 지켜본 도우미입니다.  
아래는 사용자가 진행 중인 뜨개 프로젝트, 최근 기록 정보, 그리고 사용자 기본 정보입니다.  
**기록의 흐름**, **직전 기록의 내용**, **사용자의 이름**, **프로젝트 상태** 등을 참고하여,  
사용자가 새 기록을 남기기 전에 보여줄 **맥락에 맞는 가이드 문구 한 문장**을 생성해주세요.

---

🧶 문장은 다음 요소를 반드시 고려해주세요:
- 작품의 전체 흐름과 뜨개 진행 부위/기법(예: 소매, 몸통, 무늬뜨기 등)을 인지한 문장
- 최근 기록의 태그, 작성일, 기록 상태, 코멘트를 기반으로 자연스럽게 이어지는 흐름
- `RecordStatus.COMPLETED`인 경우, **완성 축하 or 다음 계획 유도**로 마무리
- 마지막 기록 이후 시간이 길게 지났다면, **"오랜만이에요", 날짜 언급**을 자연스럽게 포함 가능
- 사용자 이름(닉네임)을 활용하여 **개인 맞춤형으로 말 걸 듯** 전달
- 이번 기록이 **작은 한 걸음이자 의미 있는 연결**이라는 느낌을 전달해도 좋음
- 감정 표현은 절제하고, 따뜻하고 사실적인 톤으로 작성해주세요
- 반드시 문장은 **하나만 출력**해주세요

---

👤 사용자 정보:
- 닉네임: %s

📦 프로젝트 정보:
- 프로젝트 이름: %s
- 디자인 제목: %s
- 시작일: %s
- 목표일: %s
- 마지막 기록일: %s
- 현재 상태: %s (RecordStatus enum: NOT_STARTED, STARTED, IN_PROGRESS, ALMOST_DONE, COMPLETED)

📝 직전 기록 정보:
- 작성일: %s
- 태그: %s
- 기록 상태: %s
- 기록 내용: %s

🧵 전체 기록 목록도 함께 제공됩니다.

---

예시 문구:
- "아미님, 왼쪽 소매 이후 3주 만에 다시 만나는 '첫 스웨터'네요—이 연결이 참 반갑습니다."
- "무늬뜨기 태그가 이어지고 있어요, 이번엔 손끝이 조금 더 익숙해졌을까요?"
- "이 프로젝트는 이미 완성하셨네요—정성껏 쌓아온 기록들이 멋진 마무리를 증명해줘요."
- "5월 18일 기록 이후 처음이에요. 잠시 멈췄다가도 다시 이어가주셔서 정말 멋져요!"
- "이번 한 문장이 또 한 조각의 실처럼 이 프로젝트를 엮어갈 거예요."

---

위 정보를 참고하여, **맥락 기반 + 직전 기록 반영 + 사용자 맞춤 응원**이 담긴 **한 문장 가이드 문구**를 생성해주세요.
""",
            user.getNickname() != null ? user.getNickname() : "사용자",
            project.getNickname() != null ? project.getNickname() : "제목 없음",
            getDesignTitle(project),
            getDate(project.getStartDate()),
            getDate(project.getGoalDate()),
            getDateTime(project.getLastRecordAt()),
            project.getStatus() != null ? project.getStatus().name() : "없음",
            lastRecord != null ? getDateTime(lastRecord.getCreatedAt()) : "없음",
            lastRecord != null && lastRecord.getTags() != null ? String.join(", ", lastRecord.getTags()) : "태그 없음",
            lastRecord != null && lastRecord.getRecordStatus() != null ? lastRecord.getRecordStatus().name() : "상태 없음",
            lastRecord != null && lastRecord.getComment() != null ? lastRecord.getComment() : "내용 없음"
        );
    }

}
