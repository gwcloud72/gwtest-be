package com.kh.finalproject.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kh.finalproject.configuration.DailyQuestProperties;
import com.kh.finalproject.dao.DailyQuestDao; // [NEW] DAO 추가
import com.kh.finalproject.dao.PointGetQuestDao;
import com.kh.finalproject.vo.DailyQuestVO;
import com.kh.finalproject.vo.DailyQuizVO;

@Service
public class DailyQuestService {

    @Autowired private DailyQuestProperties questProps; 
    @Autowired private PointGetQuestDao questDao;       
    
    @Lazy

    @Autowired private PointService pointService;       // 포인트 지급 및 이력 관리
    @Autowired private DailyQuestDao quizDao;            // 퀴즈 DB 접근 (SqlSession 사용)



 // 오늘 날짜를 yyyyMMdd 형식 문자열로 반환
    private String getTodayStr() {
        return LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
    }

    // 1. 일일 퀘스트 목록 조회
    public List<DailyQuestVO> getQuestList(String memberId) {
        String today = getTodayStr(); // 오늘 날짜
        List<Map<String, Object>> logs = questDao.selectTodayLogs(memberId, today); // 오늘 퀘스트 로그 조회

        // type 기준으로 로그를 빠르게 찾기 위한 Map
        Map<String, Map<String, Object>> logMap = logs.stream()
            .collect(Collectors.toMap(m -> (String) m.get("type"), m -> m));

        List<DailyQuestVO> result = new ArrayList<>();

        // 설정 파일에 정의된 퀘스트 기준으로 화면 데이터 생성
        for (DailyQuestProperties.QuestDetail q : questProps.getList()) {
            Map<String, Object> log = logMap.get(q.getType()); // 해당 퀘스트 로그
            int current = (log != null) ? Integer.parseInt(String.valueOf(log.get("count"))) : 0; // 현재 진행도
            boolean claimed = (log != null) && "Y".equals(log.get("rewardYn")); // 보상 수령 여부
            boolean done = current >= q.getTarget(); // 목표 달성 여부

            result.add(DailyQuestVO.builder()
                .type(q.getType())
                .title(q.getTitle())
                .current(current)
                .target(q.getTarget())
                .reward(q.getReward())
                .done(done)
                .claimed(claimed)
                .desc(getDescByType(q.getType()))     // 설명 텍스트
                .icon(getIconByType(q.getType()))     // 아이콘
                .action(getActionByType(q.getType())) // 동작 타입
                .build());
        }
        return result;
    }

    // 2. 오늘 아직 풀지 않은 경우에만 랜덤 퀴즈 반환
    public DailyQuizVO getRandomQuiz(String memberId) {
        List<Map<String, Object>> logs = questDao.selectTodayLogs(memberId, getTodayStr());
        boolean alreadySolved = logs.stream()
            .anyMatch(m -> "QUIZ".equals(m.get("type"))); // 오늘 퀴즈 수행 여부

        if (alreadySolved) return null;
        return quizDao.getRandomQuiz(); // 랜덤 퀴즈 1개
    }

    // 3. 퀴즈 정답 검증 및 퀘스트 진행 처리
    @Transactional
    public boolean checkQuizAndProgress(String memberId, int quizNo, String userAnswer) {
        if (userAnswer == null) return false;

        String correctAnswer = quizDao.getAnswer(quizNo); // 정답 조회
        if (correctAnswer == null) return false;

        // 공백 제거 + 소문자 비교
        String cleanUser = userAnswer.replace(" ", "").toLowerCase();
        String cleanCorrect = correctAnswer.replace(" ", "").toLowerCase();

        if (cleanUser.contains(cleanCorrect)) {
            questProgress(memberId, "QUIZ"); // 퀘스트 진행도 증가
            return true;
        }
        return false;
    }

    // 4. 퀘스트 진행도 증가 (유효한 타입만 처리)
    @Transactional
    public void questProgress(String memberId, String type) {
        boolean isValid = questProps.getList().stream()
            .anyMatch(q -> q.getType().equals(type)); // 설정에 존재하는 퀘스트인지 확인

        if (isValid) {
            questDao.upsertQuestLog(memberId, type, getTodayStr()); // count 증가 또는 insert
        }
    }

    // 5. 퀘스트 보상 수령
    @Transactional
    public int claimReward(String memberId, String type) {
        // 해당 퀘스트 설정 조회
        DailyQuestProperties.QuestDetail targetQuest = questProps.getList().stream()
            .filter(q -> q.getType().equals(type))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("존재하지 않는 퀘스트입니다."));

        List<Map<String, Object>> logs = questDao.selectTodayLogs(memberId, getTodayStr());
        Map<String, Object> myLog = logs.stream()
            .filter(m -> m.get("type").equals(type))
            .findFirst()
            .orElse(null);

        if (myLog == null) throw new RuntimeException("기록 없음");

        int current = Integer.parseInt(String.valueOf(myLog.get("count")));
        if (current < targetQuest.getTarget()) throw new RuntimeException("목표 미달성");
        if ("Y".equals(myLog.get("rewardYn"))) throw new RuntimeException("이미 수령");

        // 보상 수령 처리
        if (questDao.updateRewardStatus(memberId, type, getTodayStr()) > 0) {
            pointService.addPoint(
                memberId,
                targetQuest.getReward(),
                "GET",
                "일일 퀘스트 보상: " + targetQuest.getTitle()
            );
            return targetQuest.getReward();
        }
        return 0;
    }

    // 퀘스트 타입별 아이콘
    private String getIconByType(String type) {
        switch(type) {
            case "REVIEW": return "✍️";
            case "QUIZ": return "🧠";
            case "LIKE": return "❤️";
            case "ROULETTE": return "🎰";
            default: return "❓";
        }
    }

    // 퀘스트 설명 텍스트
    private String getDescByType(String type) {
        switch(type) {
            case "REVIEW": return "한줄평 남기기";
            case "QUIZ": return "오늘의 영화 퀴즈";
            case "LIKE": return "좋아요 누르기";
            case "ROULETTE": return "룰렛 돌리기";
            default: return "일일 퀘스트";
        }
    }

    // 프론트 동작 구분용 값
    private String getActionByType(String type) {
        switch(type) {
            case "REVIEW": return "link";
            case "QUIZ": return "quiz";
            case "LIKE": return "link";
            case "ROULETTE": return "roulette";
            default: return "none";
        }
    }

}