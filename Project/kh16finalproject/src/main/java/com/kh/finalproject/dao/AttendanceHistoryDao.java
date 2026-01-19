package com.kh.finalproject.dao;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.kh.finalproject.dto.AttendanceHistoryDto;



@Repository
public class AttendanceHistoryDao {

	@Autowired
	private SqlSession sqlSession;

	// 출석 이력 등록
	public int insert(AttendanceHistoryDto attendanceHistoryDto) {
	    // attendance_history 테이블에 출석 기록 1건 추가
	    return sqlSession.insert("attendanceHistory.insert", attendanceHistoryDto);
	}

	// 출석 이력 단건 삭제
	public boolean delete(int historyNo) {
	    // 출석 이력 번호로 해당 기록 삭제
	    return sqlSession.delete("attendanceHistory.delete", historyNo) > 0;
	}

	// 특정 회원의 출석 이력 전체 삭제
	public boolean deleteAll(String memberId) {
	    // 회원 ID 기준으로 출석 이력 전체 제거
	    return sqlSession.delete("attendanceHistory.deleteAll", memberId) > 0;
	}

	// 특정 회원의 출석 이력 목록 조회
	public List<AttendanceHistoryDto> selectList(String memberId) {
	    // 회원 ID 기준 출석 이력 리스트 조회 (최신순)
	    return sqlSession.selectList("attendanceHistory.selectList", memberId);
	}

	// 출석 달력 표시용 날짜 목록 조회
	public List<String> selectCalendarDates(String memberId) {
	    // 출석한 날짜를 문자열(YYYY-MM-DD) 형태로 조회
	    return sqlSession.selectList("attendanceHistory.selectCalendarDates", memberId);
	}
}