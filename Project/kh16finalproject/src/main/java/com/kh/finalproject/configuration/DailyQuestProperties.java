package com.kh.finalproject.configuration;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "daily-quest") // application.yml 의 daily-quest 설정 매핑
public class DailyQuestProperties {

    private List<QuestDetail> list; // 데일리 퀘스트 목록

    @Data
    public static class QuestDetail {

        private String type;   // 퀘스트 구분용 타입
        private String title;  // 화면에 표시할 제목
        private int target;    // 달성 조건 수치
        private int reward;    // 완료 시 지급 포인트
    }
}
