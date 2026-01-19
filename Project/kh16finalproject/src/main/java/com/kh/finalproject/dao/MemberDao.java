package com.kh.finalproject.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import com.kh.finalproject.dto.MemberDto;
import com.kh.finalproject.dto.MemberProfileDto;
import com.kh.finalproject.vo.PageVO;

@Repository
public class MemberDao {

	@Autowired
	private SqlSession sqlSession;
	@Autowired
	private PasswordEncoder passwordEncoder;

	/// 등록
	public void insert(MemberDto memberDto) {
		// ++ 비밀번호 암호화
		String origin = memberDto.getMemberPw();
		String encoded = passwordEncoder.encode(origin); // 암호화
		memberDto.setMemberPw(encoded);
		sqlSession.insert("member.insert", memberDto);
	}

	/// 조회
	// 기본조회(목록)
	public List<MemberDto> selectList() {
		return sqlSession.selectList("member.selectList");
	}

	// 상세조회
	public MemberDto selectOne(String memberId) {
		return sqlSession.selectOne("member.detail", memberId);
	}
	
	public MemberProfileDto selectProfile(String memberId) {
		return sqlSession.selectOne("member.selectProfile", memberId);
	}

	// 닉네임 중복 검사용 조회
	public MemberDto selectOneByMemberNickname(String memberNickname) {
		return sqlSession.selectOne("member.detailByNickname", memberNickname);
	}

	// 관리자 제외하고 조회
	public List<MemberDto> selectListExceptAdmin(PageVO pageVO) {
		return sqlSession.selectList("member.selectListExceptAdmin", pageVO);
	}

	public int countMember() {
		return sqlSession.selectOne("member.countMember");
	}

	// 회원검색
	public List<MemberDto> selectAdminMemberList(String type, String keyword, PageVO pageVO) {
		Map<String, Object> params = new HashMap<>();
		params.put("pageVO", pageVO);
		params.put("type", type);
		params.put("keyword", keyword);
		return sqlSession.selectList("member.selectAdminList", params);
	}

	public int countSearchMember(String type, String keyword) {
		Map<String, Object> params = new HashMap<>();
		params.put("type", type);
		params.put("keyword", keyword);
		return sqlSession.selectOne("member.countSearchMember", params);
	}

	/// 수정
	// (회원기본정보 수정)
	public boolean update(MemberDto memberDto) {
		return sqlSession.update("member.update", memberDto) > 0;
	}

	// (닉네임 수정)
	// + 컨트롤러에서 닉네임 수정할때 포인트 차감이 필요할지?
	// + 포인트가 부족하면 닉네임 수정이 불가능할지
	public boolean updateNickname(MemberDto memberDto) {
		return sqlSession.update("member.updateNickname", memberDto) > 0;
	}

	// (비밀번호 수정)
	public boolean updatePassword(MemberDto memberDto) {
		// ++ 비밀번호 암호화
		String origin = memberDto.getMemberPw();
		String encoded = passwordEncoder.encode(origin); // 암호화
		memberDto.setMemberPw(encoded);
		return sqlSession.update("member.updatePassword", memberDto) > 0;
	}

	// (포인트 갱신)
	public boolean updatePoint(MemberDto memberDto) {
		return sqlSession.update("member.updatePoint", memberDto) > 0;
	}

	public boolean upPoint(MemberDto memberDto) {
		return sqlSession.update("member.upPoint", memberDto) > 0;
	}

	// (신뢰도 갱신)
	public void updateReliability(String memberId, int rel) {
		Map<String, Object> param = new HashMap<>();
		param.put("memberId", memberId);
		param.put("rel", rel);
		sqlSession.update("member.updateReliability", param);
	}

	// (회원등급 수정)
	public boolean updateMemberLevel(MemberDto memberDto) {
		return sqlSession.update("member.updateMemberLevel", memberDto) > 0;
	}

	/// 삭제 (회원탈퇴)
	public boolean delete(String memberId) {
		return sqlSession.delete("member.delete", memberId) > 0;
	}

	// 좋아요 신뢰도
	public void updateReliabilitySet(String memberId, int rel) {
		Map<String, Object> param = new HashMap<>();
		param.put("memberId", memberId);
		param.put("rel", rel);
		sqlSession.update("member.updateReliabilitySet", param);
	}

	// 회원 단건 조회 (기본 정보 맵핑용)
	// 마이페이지, 포인트/아이콘 등 여러 기능에서 공통으로 사용
	public MemberDto selectMap(String memberId) {
	    return sqlSession.selectOne("member.selectMap", memberId);
	}

	// -------------------------------------------------------------
	// 관리자 포인트 관리 페이지 전용 메서드
	// -------------------------------------------------------------

	// 관리자 포인트 관리용 회원 목록 조회
	// - 키워드 검색 가능 (아이디 / 닉네임 등)
	// - 페이지 번호와 페이지당 개수를 받아 페이징 처리
	public List<MemberDto> selectPointAdminList(String keyword, int page, int size) {

	    // Oracle ROWNUM 기준 페이징 계산
	    int end = page * size;
	    int start = end - (size - 1);

	    Map<String, Object> params = new HashMap<>();
	    params.put("keyword", keyword);
	    params.put("start", start);
	    params.put("end", end);

	    return sqlSession.selectList("member.selectPointAdminList", params);
	}

	// 관리자 포인트 관리용 회원 전체 수 조회
	// 페이징 계산(총 페이지 수)을 위해 사용
	public int countPointAdminList(String keyword) {
	    Map<String, Object> params = new HashMap<>();
	    params.put("keyword", keyword);
	    return sqlSession.selectOne("member.countPointAdminList", params);
	}

	// 관리자 포인트 강제 지급 / 차감
	// amount 값이 양수면 지급, 음수면 차감
	// 이벤트 보상, 제재 처리 등 관리자 수동 조정용
	public boolean adminUpdatePoint(String memberId, int amount) {
	    Map<String, Object> params = new HashMap<>();
	    params.put("memberId", memberId);
	    params.put("amount", amount);
	    return sqlSession.update("member.adminUpdatePoint", params) > 0;
	}

	// 관리자 회원 정보 수정
	// 닉네임, 등급 등 관리자 권한으로 변경 가능한 정보 수정
	public boolean adminUpdateMemberInfo(MemberDto memberDto) {
	    return sqlSession.update("member.adminUpdateMemberInfo", memberDto) > 0;
	}

	// 회원 신뢰도 조회
	// 신고 누적, 활동 이력 기반 신뢰도 확인용
	// 관리자 페이지 또는 제재 판단 로직에서 사용
	public int selectReliability(String memberId) {
	    return sqlSession.selectOne("member.selectReliability", memberId);
	}
}
