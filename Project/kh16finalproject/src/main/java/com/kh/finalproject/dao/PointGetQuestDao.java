package com.kh.finalproject.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class PointGetQuestDao {

    @Autowired
    private SqlSession sqlSession;
    // 예: <mapper namespace="pointGetQuest"> 로 설정했다고 가정
    private static final String NAMESPACE = "dailyquest."; 
 // 오늘 날짜 기준으로 유저의 퀘스트 진행 현황 조회
 // 이미 진행한 퀘스트 종류, 횟수, 보상 수령 여부 확인용
 public List<Map<String, Object>> selectTodayLogs(String memberId, String date) {
     Map<String, Object> params = new HashMap<>();
     params.put("memberId", memberId);
     params.put("date", date);
     return sqlSession.selectList(NAMESPACE + "selectTodayLogs", params);
 }

 // 퀘스트 진행 처리
 // 같은 날 동일 퀘스트가 있으면 횟수 +1
 // 없으면 신규 로그 생성 (MERGE 사용)
 public int upsertQuestLog(String memberId, String type, String date) {
     Map<String, Object> params = new HashMap<>();
     params.put("memberId", memberId);
     params.put("type", type);
     params.put("date", date);
     return sqlSession.update(NAMESPACE + "upsertQuestLog", params);
 }

 // 퀘스트 보상 수령 완료 처리
 // 이미 보상을 받은 경우 중복 수령 방지
 public int updateRewardStatus(String memberId, String type, String date) {
     Map<String, Object> params = new HashMap<>();
     params.put("memberId", memberId);
     params.put("type", type);
     params.put("date", date);
     return sqlSession.update(NAMESPACE + "updateRewardStatus", params);
 }

}