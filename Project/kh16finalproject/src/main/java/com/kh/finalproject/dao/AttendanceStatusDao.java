package com.kh.finalproject.dao;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.kh.finalproject.dto.AttendanceStatusDto;

@Repository
public class AttendanceStatusDao {

	@Autowired
	private SqlSession sqlSession;

	// 출석 상태 최초 등록
	public int insert(AttendanceStatusDto attendanceStatusDto) {
	    // 회원별 출석 상태 기본 데이터 생성
	    return sqlSession.insert("attendanceStatus.insert", attendanceStatusDto);
	}

	// 출석 상태 갱신
	public boolean update(AttendanceStatusDto attendanceStatusDto) {
	    // 연속 출석, 최대 출석, 총 출석 정보 업데이트
	    return sqlSession.update("attendanceStatus.update", attendanceStatusDto) > 0;
	}

	// 회원 출석 상태 삭제
	public boolean delete(String memberId) {
	    // 회원 탈퇴 또는 초기화 시 출석 상태 제거
	    return sqlSession.delete("attendanceStatus.delete", memberId) > 0;
	}

	// 회원 출석 상태 단건 조회
	public AttendanceStatusDto selectOne(String memberId) {
	    // 회원 ID 기준 출석 상태 정보 조회
	    return sqlSession.selectOne("attendanceStatus.selectOne", memberId);
	}
}