package com.kh.finalproject.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.kh.finalproject.dto.IconDto;
import com.kh.finalproject.dto.MemberIconDto;

@Repository
public class MemberIconDao {

    @Autowired 
    private SqlSession sqlSession;

 // 내 보유 아이콘 목록 조회
    public List<MemberIconDto> selectMyIcons(String memberId) {
        return sqlSession.selectList("memberIcon.selectMyIcons", memberId);
    }

    // 유저가 이미 보유 중인 아이콘인지 확인
    // 중복 지급 방지용
    public int checkUserHasIcon(String memberId, int iconId) {
        Map<String, Object> params = new HashMap<>();
        params.put("memberId", memberId);
        params.put("iconId", iconId);
        return sqlSession.selectOne("memberIcon.checkUserHasIcon", params);
    }

    // 유저에게 아이콘 지급
    public int insertMemberIcon(String memberId, int iconId) {
        Map<String, Object> params = new HashMap<>();
        params.put("memberId", memberId);
        params.put("iconId", iconId);
        return sqlSession.insert("memberIcon.insertMemberIcon", params);
    }

    // 해당 유저의 모든 아이콘 장착 해제
    public void unequipAllIcons(String memberId) {
        sqlSession.update("memberIcon.unequipAllIcons", memberId);
    }

    // 특정 아이콘 장착
    public void equipIcon(String memberId, int iconId) {
        Map<String, Object> params = new HashMap<>();
        params.put("memberId", memberId);
        params.put("iconId", iconId);
        sqlSession.update("memberIcon.equipIcon", params);
    }

    // 현재 장착 중인 아이콘 이미지 경로 조회
    public String selectEquippedIconSrc(String memberId) {
        Map<String, Object> param = new HashMap<>();
        param.put("memberId", memberId);
        return sqlSession.selectOne("memberIcon.selectEquippedIconSrc", param);
    }

    // 장착 중인 프레임 스타일 조회
    public String selectEquippedFrameStyle(String memberId) {
        return sqlSession.selectOne("memberIcon.selectEquippedFrameStyle", memberId);
    }

    // 장착 중인 배경 스타일 조회
    public String selectEquippedBgStyle(String memberId) {
        return sqlSession.selectOne("memberIcon.selectEquippedBgStyle", memberId);
    }

    // 장착 중인 닉네임 스타일 조회
    public String selectEquippedNickStyle(String memberId) {
        return sqlSession.selectOne("memberIcon.selectEquippedNickStyle", memberId);
    }

    // 특정 타입(프레임/배경/닉네임) 아이템 전체 장착 해제
    // 동일 타입 중복 장착 방지용
    public void unequipAllItemsByType(String memberId, String itemType) {
        Map<String, Object> params = new HashMap<>();
        params.put("memberId", memberId);
        params.put("itemType", itemType);
        sqlSession.update("memberIcon.unequipAllItemsByType", params);
    }

    // 인벤토리 번호 기준 아이템 장착
    public void equipItem(int inventoryNo) {
        sqlSession.update("memberIcon.equipItem", inventoryNo);
    }

    // 전체 아이콘 목록 조회 (상점/관리자용)
    public List<IconDto> selectIconList() {
        return sqlSession.selectList("memberIcon.selectIconList");
    }

    // 유저 아이콘 삭제 (회수 또는 정리용)
    public int deleteMemberIcon(long memberIconId) {
        return sqlSession.delete("memberIcon.deleteMemberIcon", memberIconId);
    }

    // 유저 아이콘 상세 조회
    // 관리자 또는 마이페이지 상세용
    public List<MemberIconDto> selectUserIcon(String memberId) {
        return sqlSession.selectList("memberIcon.selectUserIconsDetail", memberId);
    }
}

