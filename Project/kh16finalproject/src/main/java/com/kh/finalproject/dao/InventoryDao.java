package com.kh.finalproject.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import com.kh.finalproject.dto.InventoryDto;
import com.kh.finalproject.dto.MemberDto;

@Repository
public class InventoryDao {

    @Autowired
    private SqlSession sqlSession;

 // 아이템 신규 추가 (구매 시 인벤토리 등록)
    public int insert(InventoryDto inventoryDto) {
        return sqlSession.insert("inventory.insert", inventoryDto);
    }

    // 아이템 수량 또는 장착 상태 변경
    public boolean update(InventoryDto inventoryDto) {
        return sqlSession.update("inventory.update", inventoryDto) > 0;
    }

    // 특정 회원 인벤토리 전체 조회
    public List<InventoryDto> selectListByMemberId(String memberId) {
        return sqlSession.selectList("inventory.selectListByMemberId", memberId);
    }

    // 인벤토리 번호 기준 단일 조회
    public InventoryDto selectOne(long inventoryNo) {
        return sqlSession.selectOne("inventory.selectOne", inventoryNo);
    }

    // 회원이 특정 아이템을 보유 중인지 확인
    // 동일 아이템 중복 지급 방지 및 수량 처리용
    public InventoryDto selectOneByMemberAndItem(String memberId, long itemNo) {
        InventoryDto params = new InventoryDto();
        params.setInventoryMemberId(memberId);
        params.setInventoryItemNo(itemNo);
        return sqlSession.selectOne("inventory.selectOneByMemberAndItem", params);
    }

    // 특정 회원의 아이템 보유 개수 조회
    public int selectCountMyItem(String memberId, long itemNo) {
        InventoryDto params = new InventoryDto();
        params.setInventoryMemberId(memberId);
        params.setInventoryItemNo(itemNo);
        return sqlSession.selectOne("inventory.selectCountMyItem", params);
    }

    // 인벤토리 항목 삭제
    // 수량 0이거나 관리자 강제 삭제 시 사용
    public boolean delete(long inventoryNo) {
        return sqlSession.delete("inventory.delete", inventoryNo) > 0;
    }

    // 관리자용 특정 회원 인벤토리 조회
    public List<InventoryDto> selectListByAdmin(String memberId) {
        return sqlSession.selectList("inventory.selectListByAdmin", memberId);
    }

    // 동일 타입 아이템 장착 해제
    // 새 아이템 장착 전 기존 장착 해제 처리
    public void unequipByType(String memberId, String type) {
        Map<String, Object> params = new HashMap<>();
        params.put("memberId", memberId);
        params.put("type", type);
        sqlSession.update("inventory.unequipByType", params);
    }

    // 관리자용 회원 목록 조회 (검색 + 페이징)
    public List<MemberDto> fetchAdminMemberList(String keyword, int startRow, int endRow) {
        Map<String, Object> params = new HashMap<>();
        params.put("keyword", keyword);
        params.put("startRow", startRow);
        params.put("endRow", endRow);
        return sqlSession.selectList("inventory.fetchAdminMemberList", params);
    }

    // 관리자 페이지 페이징용 회원 수 조회
    public int countAdminMembers(String keyword) {
        return sqlSession.selectOne("inventory.countAdminMembers", keyword);
    }
    
}

	