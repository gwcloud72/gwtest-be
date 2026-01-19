package com.kh.finalproject.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import com.kh.finalproject.dto.PointHistoryDto;

@Repository
public class PointHistoryDao {

    @Autowired
    private SqlSession sqlSession;

 // 포인트 내역 신규 등록 (적립 / 사용 / 아이템 지급 로그)
    public int insert(PointHistoryDto pointHistoryDto) { 
        return sqlSession.insert("pointhistory.insert", pointHistoryDto);
    }

    // 포인트 내역 수정 (관리자 정정용)
    public boolean update(PointHistoryDto pointHistoryDto) {
        return sqlSession.update("pointhistory.update", pointHistoryDto) > 0;
    }

    // 특정 회원의 포인트 내역 전체 조회
    // 마이페이지 포인트 히스토리 기본 조회용
    public List<PointHistoryDto> selectListByMemberId(String memberId) {
        return sqlSession.selectList("pointhistory.selectListByMemberId", memberId);
    }

    // 포인트 내역 단건 조회 (히스토리 상세 보기용)
    public PointHistoryDto selectOne(long pointHistoryId) {
        return sqlSession.selectOne("pointhistory.selectOne", pointHistoryId);
    }

    // 포인트 내역 삭제 (관리자 전용)
    public boolean delete(long pointHistoryId) {
        return sqlSession.delete("pointhistory.delete", pointHistoryId) > 0;
    }

    // 포인트 내역 전체 건수 조회
    // type 값에 따라 적립 / 사용 / 아이템 내역 필터링
    public int countHistory(String memberId, String type) {
        Map<String, Object> params = new HashMap<>();
        params.put("memberId", memberId);
        params.put("type", type);
        return sqlSession.selectOne("pointhistory.countHistory", params);
    }

    // 포인트 내역 페이징 조회
    // 마이페이지에서 페이지 이동 시 사용
    public List<PointHistoryDto> selectListByMemberIdPaging(
            String memberId, int startRow, int endRow, String type) {

        Map<String, Object> params = new HashMap<>();
        params.put("memberId", memberId);
        params.put("startRow", startRow);
        params.put("endRow", endRow);
        params.put("type", type);

        return sqlSession.selectList("pointhistory.selectListByMemberIdPaging", params);
    }

    // 오늘 날짜 기준 특정 아이템 구매 횟수 조회
    // 일일 구매 제한 체크용
    public int countTodayPurchase(String memberId, String itemName) {
        Map<String, Object> params = new HashMap<>();
        params.put("memberId", memberId);
        params.put("itemName", itemName);
        return sqlSession.selectOne("pointhistory.countTodayPurchase", params);
    }
}