package com.kh.finalproject.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kh.finalproject.dao.IconDao;
import com.kh.finalproject.dao.InventoryDao;
import com.kh.finalproject.dao.MemberIconDao;
import com.kh.finalproject.dao.PointItemStoreDao;
import com.kh.finalproject.dto.IconDto;
import com.kh.finalproject.dto.InventoryDto;
import com.kh.finalproject.dto.MemberDto;
import com.kh.finalproject.dto.MemberIconDto;
import com.kh.finalproject.dto.PointItemStoreDto;
import com.kh.finalproject.vo.AdminPointItemPageVO;

@Service
public class AdminAssetService {
    @Autowired private InventoryDao inventoryDao;
    @Autowired private MemberIconDao memberIconDao;
    @Autowired private PointItemStoreDao pointItemStoreDao;
    @Autowired private IconDao iconDao;

 // [1] 관리자용 유저 목록 조회 (검색 + 페이징)
    @Transactional(readOnly = true)
    public Map<String, Object> getAdminMemberList(String keyword, int page) {
        int size = 10;                          // 페이지당 데이터 수
        int startRow = (page - 1) * size + 1;  // 시작 행
        int endRow = page * size;              // 끝 행

        List<MemberDto> list =
                inventoryDao.fetchAdminMemberList(keyword, startRow, endRow); // 유저 목록
        int totalCount =
                inventoryDao.countAdminMembers(keyword);                       // 전체 유저 수
        int totalPage = (totalCount + size - 1) / size;                        // 전체 페이지 수

        Map<String, Object> response = new HashMap<>();
        response.put("list", list);
        response.put("totalPage", totalPage);
        return response;
    }

    // [2] 관리자용 특정 유저 인벤토리 조회
    @Transactional(readOnly = true)
    public List<InventoryDto> getUserInventory(String memberId) {
        return inventoryDao.selectListByAdmin(memberId); // 아이템 목록
    }

    // [2-1] 관리자용 특정 유저 아이콘 조회
    @Transactional(readOnly = true)
    public List<MemberIconDto> getUserIcons(String memberId) {
        return memberIconDao.selectUserIcon(memberId); // 아이콘 목록
    }

    // [3] 마스터 아이템 목록 조회 (검색 + 페이징)
    @Transactional(readOnly = true)
    public Map<String, Object> getMasterItemList(String type, String keyword, int page, int size) {
        AdminPointItemPageVO vo = new AdminPointItemPageVO();
        vo.setItemType(type);   // 아이템 타입 필터
        vo.setKeyword(keyword); // 검색어
        vo.setPage(page);
        vo.setSize(size);

        List<PointItemStoreDto> list =
                pointItemStoreDao.selectList(vo); // 아이템 목록
        int totalCount =
                pointItemStoreDao.selectCount(vo); // 전체 개수
        int totalPage = (totalCount + size - 1) / size; // 페이지 수 계산

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("totalPage", totalPage);
        return result;
    }

    // [4] 마스터 아이콘 목록 조회 (페이징)
    @Transactional(readOnly = true)
    public Map<String, Object> getMasterIconList(int page) {
        int size = 15;                          // 페이지당 아이콘 수
        int startRow = (page - 1) * size + 1;
        int endRow = page * size;

        List<IconDto> list =
                iconDao.selectListPaging(startRow, endRow, "ALL", "ALL"); // 아이콘 목록
        int totalCount =
                iconDao.countIcons("ALL", "ALL");                         // 전체 아이콘 수
        int totalPage = (totalCount + size - 1) / size;

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("totalPage", totalPage);
        return result;
    }

    // [5] 자산 회수 (아이템 or 아이콘)
    @Transactional
    public boolean recallAsset(String type, long id) {
        return "item".equals(type)
                ? inventoryDao.delete(id)             // 아이템 회수
                : memberIconDao.deleteMemberIcon(id) > 0; // 아이콘 회수
    }

    // [6] 자산 지급 (아이템 or 아이콘)
    @Transactional
    public String grantAsset(String type, String memberId, int targetNo) {
        if ("item".equals(type)) {
            InventoryDto dto = new InventoryDto();
            dto.setInventoryMemberId(memberId);       // 대상 유저
            dto.setInventoryItemNo((long) targetNo);  // 지급할 아이템
            inventoryDao.insert(dto);
            return "success";
        } else {
            if (memberIconDao.checkUserHasIcon(memberId, targetNo) > 0)
                return "duplicate";                   // 이미 보유 중
            memberIconDao.insertMemberIcon(memberId, targetNo);
            return "success";
        }
    }
}