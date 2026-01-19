package com.kh.finalproject.restcontroller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.kh.finalproject.service.DailyQuestService;
import com.kh.finalproject.vo.DailyQuestVO;
import com.kh.finalproject.vo.DailyQuizVO;

@RestController
@RequestMapping("/point/quest")
@CrossOrigin
public class DailyQuestRestController {

    @Autowired private DailyQuestService dailyQuestService;

 // 1. 내 퀘스트 목록 조회
    @GetMapping("/list")
    public List<DailyQuestVO> list(@RequestAttribute(value="loginId", required=false) String loginId) {
        // 비로그인 상태면 그냥 빈 리스트 던져줌
        if(loginId == null) return List.of();
        
        // 오늘 완료한 퀘스트랑 진행 중인 목록 싹 긁어오기
        return dailyQuestService.getQuestList(loginId);
    }

    // 2. 퀘스트용 랜덤 퀴즈 문제 요청
    @GetMapping("/quiz/random")
    public DailyQuizVO getRandomQuiz(@RequestAttribute("loginId") String loginId) {
        // 이미 오늘 분량을 다 풀었다면 서비스에서 null이 올 수 있음
        // 프론트에서 null이면 '이미 완료' 메시지 띄워주는 로직 필요함
        return dailyQuestService.getRandomQuiz(loginId);
    }

    // 3. 사용자가 입력한 퀴즈 정답 제출 및 채점
    @PostMapping("/quiz/check")
    public String checkQuiz(@RequestAttribute("loginId") String loginId, @RequestBody Map<String, Object> body) {
        // 퀴즈 번호 안 넘어오면 바로 컷 (방어 코드)
        if (body.get("quizNo") == null) {
            System.out.println("오류: 프론트엔드에서 quizNo가 오지 않았습니다.");
            return "fail:quizNo is null";
        }

        try {
            int quizNo = Integer.parseInt(String.valueOf(body.get("quizNo")));
            String userAnswer = (String) body.get("answer");
            
            // 정답인지 확인하고, 정답이면 퀘스트 진행도(Progress)까지 같이 업데이트
            boolean isCorrect = dailyQuestService.checkQuizAndProgress(loginId, quizNo, userAnswer);
            
            return isCorrect ? "success" : "fail";
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return "fail:invalid number format";
        }
    }

    // 4. 완료된 퀘스트 보상 받기
    @PostMapping("/claim")
    public String claim(@RequestAttribute("loginId") String loginId, @RequestBody Map<String, String> body) {
        try {
            // 어떤 종류의 퀘스트(type) 보상인지 확인
            String type = body.get("type");
            
            // 보상 지급 로직 실행 후 획득한 금액 반환
            int reward = dailyQuestService.claimReward(loginId, type);
            
            // "success:500" 같은 형태로 보내서 프론트에서 얼마 받았는지 바로 띄우게 함
            return "success:" + reward;
        } catch (Exception e) {
            // 이미 받았거나 완료되지 않은 경우 에러 메시지 반환
            return "fail:" + e.getMessage();
        }
    }
}