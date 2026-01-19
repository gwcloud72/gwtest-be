package com.kh.finalproject.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kh.finalproject.dao.HeartDao;
import com.kh.finalproject.dao.InventoryDao;
import com.kh.finalproject.dao.MemberDao;
import com.kh.finalproject.dao.MemberIconDao;
import com.kh.finalproject.dao.PointHistoryDao;
import com.kh.finalproject.dao.PointItemStoreDao;
import com.kh.finalproject.dao.PointWishlistDao;
import com.kh.finalproject.dto.InventoryDto;
import com.kh.finalproject.dto.MemberDto;
import com.kh.finalproject.dto.PointHistoryDto;
import com.kh.finalproject.dto.PointItemStoreDto;
import com.kh.finalproject.dto.PointWishlistDto;
import com.kh.finalproject.vo.MemberPointVO;
import com.kh.finalproject.vo.PointHistoryPageVO;
import com.kh.finalproject.vo.PointItemWishVO;

@Service
public class PointService {

    @Autowired private PointItemStoreDao pointItemDao;
    @Autowired private MemberDao memberDao;
    @Autowired private InventoryDao inventoryDao; 
    @Autowired private PointHistoryDao pointHistoryDao;
    @Autowired private PointWishlistDao pointWishlistDao;
    @Autowired private DailyQuestService dailyQuestService;
    @Autowired private MemberIconDao memberIconDao;
    @Autowired private HeartDao heartDao;

 // [1] 포인트 증감 공통 메서드 (포인트 변동 + 히스토리 기록)
    @Transactional
    public boolean addPoint(String loginId, int amount, String trxType, String reason) {
        MemberDto currentMember = memberDao.selectOne(loginId);
        if (currentMember == null) throw new RuntimeException("회원 정보가 없습니다.");

        // 차감 시 잔액이 부족하면 튕김 처리
        if (amount < 0 && (currentMember.getMemberPoint() + amount) < 0) {
            throw new RuntimeException("보유 포인트가 부족합니다.");
        }

        // 업데이트용 DTO 생성 (변동된 포인트값만 반영)
        MemberDto updateDto = MemberDto.builder()
                .memberId(loginId)
                .memberPoint(amount) 
                .build();
        
        // 포인트 반영 성공 시 이력(History) 테이블에 한 줄 남김
        if (memberDao.upPoint(updateDto)) {
            pointHistoryDao.insert(PointHistoryDto.builder()
                .pointHistoryMemberId(loginId)
                .pointHistoryAmount(amount)
                .pointHistoryTrxType(trxType)
                .pointHistoryReason(reason)
                .build());
            return true;
        }
        return false;
    }

    // 관리자 페이지에서 강제로 포인트 넣거나 뺏을 때 사용
    @Transactional
    public void adminUpdatePoint(String memberId, int amount) {
        String reason = (amount > 0) ? "관리자 포인트 지급" : "관리자 포인트 회수(차감)";
        String AdminType = (amount > 0) ?  "GET" : "USE";
        boolean result = addPoint(memberId, amount, AdminType, reason);
        if(!result) throw new RuntimeException("포인트 처리 중 오류가 발생했습니다.");
    }
    
    // 아이템 구매 (검증 로직이 좀 까다로움)
    @Transactional
    public void purchaseItem(String loginId, long itemNo) {
        // 1. 존재하는 상품인지 확인
        PointItemStoreDto item = pointItemDao.selectOneNumber(itemNo);
        if (item == null) throw new RuntimeException("상품 정보가 없습니다.");

        // 2. 꾸미기 아이템(DECO_)은 소모품이 아니므로 중복 구매 방지
        if (item.getPointItemType() != null && item.getPointItemType().startsWith("DECO_")) {
            InventoryDto existingItem = inventoryDao.selectOneByMemberAndItem(loginId, itemNo);
            if (existingItem != null) {
                throw new RuntimeException("이미 보유 중인 꾸미기 아이템입니다.");
            }
        }

        // 3. 재고 바닥났는지 체크
        if (item.getPointItemStock() <= 0) throw new RuntimeException("품절된 상품입니다.");

        // 4. 실제로 돈 깎기
        addPoint(loginId, -(int)item.getPointItemPrice(), "USE", "아이템 구매: " + item.getPointItemName());
        
        // 5. 상점 재고 마이너스 처리
        item.setPointItemStock(item.getPointItemStock() - 1);
        pointItemDao.update(item);

        // 6. 인벤토리에 넣어주기 (하트 충전은 즉시 반영)
        if ("HEART_RECHARGE".equals(item.getPointItemType())) {
            chargeHeart(loginId, 5); 
        } else {
            giveItemToInventory(loginId, itemNo); 
        }

        // 7. 구매했으면 위시리스트(찜)에서 자동 삭제
        PointItemWishVO wishVO = PointItemWishVO.builder()
                .memberId(loginId)
                .itemNo(itemNo)
                .build();
        if (pointWishlistDao.checkWish(wishVO) > 0) {
            pointWishlistDao.delete(wishVO);
        }
    }

    // 유저끼리 포인트 주고받기
    @Transactional
    public void donatePoints(String loginId, String targetId, int amount) {
        if (amount <= 0) throw new RuntimeException("후원 금액은 0보다 커야 합니다.");
        if (loginId.equals(targetId)) throw new RuntimeException("자신에게 후원할 수 없습니다.");

        // 내 포인트 깎고 상대방 포인트 올려줌
        addPoint(loginId, -amount, "USE", targetId + "님에게 후원");
        addPoint(targetId, amount, "GET", loginId + "님으로부터 후원");
    }

    // 아이템 선물하기 (구매 로직이랑 비슷한데 받는 사람이 다름)
    @Transactional
    public void giftItem(String loginId, String targetId, long itemNo) {
        PointItemStoreDto item = pointItemDao.selectOneNumber(itemNo);
        if (item == null || item.getPointItemStock() <= 0) throw new RuntimeException("선물 가능한 상품이 없습니다.");

        // 결제는 내가 하고 아이템은 상대방 인벤토리에 꽂아줌
        addPoint(loginId, -(int)item.getPointItemPrice(), "USE", targetId + "님에게 선물: " + item.getPointItemName());
        item.setPointItemStock(item.getPointItemStock() - 1);
        pointItemDao.update(item);
        giveItemToInventory(targetId, itemNo);

        // 보낸 사람 찜 목록 정리
        PointItemWishVO wishVO = PointItemWishVO.builder()
                .memberId(loginId)
                .itemNo(itemNo)
                .build();
        if (pointWishlistDao.checkWish(wishVO) > 0) {
            pointWishlistDao.delete(wishVO);
        }
    }

    // 마이페이지용 포인트/프로필 요약 정보 (아이콘, 프레임 등 장착 정보 포함)
    public MemberPointVO getMyPointInfo(String id) {
        MemberDto m = memberDao.selectOne(id); 
        if (m == null) return null;

        // 현재 끼고 있는 꾸미기 템들 다 긁어오기
        String iconSrc = memberIconDao.selectEquippedIconSrc(id);
        String frameStyle = memberIconDao.selectEquippedFrameStyle(id); 
        String bgStyle = memberIconDao.selectEquippedBgStyle(id); 
        String nickStyle = memberIconDao.selectEquippedNickStyle(id);

        // 기본 아이콘 세팅
        if (iconSrc == null) iconSrc = "https://i.postimg.cc/J4qNQTvy/fishicon.png";

        return MemberPointVO.builder()
                .memberId(m.getMemberId()) 
                .nickname(m.getMemberNickname())
                .point(m.getMemberPoint())
                .level(m.getMemberLevel()) 
                .iconSrc(iconSrc)
                .frameSrc(frameStyle) 
                .bgSrc(bgStyle)
                .nickStyle(nickStyle)
                .build();
    }

    // 인벤토리 아이템 사용 (타입별 분기 처리)
    @Transactional
    public void useItem(String loginId, long inventoryNo, String extraValue) {
        InventoryDto inven = inventoryDao.selectOne(inventoryNo);
        if (inven == null || !inven.getInventoryMemberId().equals(loginId)) 
            throw new RuntimeException("아이템 권한 없음");

        PointItemStoreDto item = pointItemDao.selectOneNumber(inven.getInventoryItemNo());
        String type = item.getPointItemType();

        switch (type) {
            case "CHANGE_NICK": // 닉네임 변경권
                if (extraValue == null || extraValue.trim().isEmpty()) 
                    throw new RuntimeException("새 닉네임을 입력하세요.");
                memberDao.updateNickname(MemberDto.builder()
                        .memberId(loginId)
                        .memberNickname(extraValue)
                        .build());
                decreaseInventoryOrDelete(inven);
                break;

            case "HEART_RECHARGE": // 하트 충전
                chargeHeart(loginId, 5); 
                decreaseInventoryOrDelete(inven);
                break;

            case "RANDOM_POINT": // 포인트 랜덤 박스 (500~3500원 사이 100원 단위)
                int randomIdx = new java.util.Random().nextInt(31); 
                int won = (randomIdx * 100) + 500;
                
                addPoint(loginId, won, "GET", "포인트 랜덤 박스 사용 + " + won + "원 획득");
                decreaseInventoryOrDelete(inven);
                break;

            case "DECO_NICK": case "DECO_BG": case "DECO_ICON": case "DECO_FRAME":
                // 꾸미기 템은 기존에 끼고 있던 거 해제하고 새 걸로 교체
                unequipByType(loginId, type); 
                inven.setInventoryEquipped("Y");
                inventoryDao.update(inven);
                break;
            
            case "VOUCHER": // 상품권 사용 (아이템 가격 그대로 포인트로 환전)
                addPoint(loginId, (int)item.getPointItemPrice(), "GET", "상품권 사용 " + item.getPointItemPrice() + "원 획득");
                decreaseInventoryOrDelete(inven);
                break;
        }
    }

    // 구매 취소 및 환불 (인벤토리에서 제거 후 포인트 복구)
    @Transactional
    public void cancelItem(String loginId, long inventoryNo) {
        InventoryDto inven = inventoryDao.selectOne(inventoryNo);
        if (inven == null || !inven.getInventoryMemberId().equals(loginId)) throw new RuntimeException("환불 권한 없음");
        
        PointItemStoreDto item = pointItemDao.selectOneNumber(inven.getInventoryItemNo());
        addPoint(loginId, (int)item.getPointItemPrice(), "GET", "환불: " + item.getPointItemName());

        item.setPointItemStock(item.getPointItemStock() + 1); // 상점 재고 복구
        pointItemDao.update(item);
        decreaseInventoryOrDelete(inven);
    }

    // 아이템 지급 (이미 있으면 수량만 늘리고, 없으면 새로 insert)
    private void giveItemToInventory(String loginId, long itemNo) {
        InventoryDto existing = inventoryDao.selectOneByMemberAndItem(loginId, itemNo);
        if (existing != null) {
            existing.setInventoryQuantity(existing.getInventoryQuantity() + 1);
            inventoryDao.update(existing);
        } else {
            inventoryDao.insert(InventoryDto.builder()
                .inventoryMemberId(loginId)
                .inventoryItemNo(itemNo)
                .inventoryQuantity(1)
                .inventoryEquipped("N")
                .build());
        }
    }

    // 아이템 수량 감소 (0개 되면 인벤토리에서 삭제)
    private void decreaseInventoryOrDelete(InventoryDto inven) {
        if (inven.getInventoryQuantity() > 1) {
            inven.setInventoryQuantity(inven.getInventoryQuantity() - 1);
            inventoryDao.update(inven);
        } else {
            inventoryDao.delete(inven.getInventoryNo());
        }
    }

    // 장착 해제
    @Transactional
    public void unequipItem(String loginId, long inventoryNo) { 
        InventoryDto inv = inventoryDao.selectOne(inventoryNo);
        if(inv != null && loginId.equals(inv.getInventoryMemberId())) {
            inv.setInventoryEquipped("N"); 
            inventoryDao.update(inv);
        }
    }

    // 특정 카테고리(타입) 전체 해제 (중복 장착 방지용)
    private void unequipByType(String loginId, String type) {
        inventoryDao.unequipByType(loginId, type);
    }

    // 포인트 이용 내역 조회 (페이징 처리)
    public PointHistoryPageVO getHistoryList(String loginId, int page, String type) {
        int size = 10;
        int startRow = (page - 1) * size + 1;
        int endRow = page * size;
        List<PointHistoryDto> list = pointHistoryDao.selectListByMemberIdPaging(loginId, startRow, endRow, type);
        int totalCount = pointHistoryDao.countHistory(loginId, type);
        return PointHistoryPageVO.builder().list(list).totalCount(totalCount).totalPage((totalCount + size - 1) / size).currentPage(page).build();
    }

    // 룰렛 돌리기
    @Transactional public int playRoulette(String loginId) {
        // 인벤토리에 룰렛 티켓 있는지 확인
        List<InventoryDto> userInventory = inventoryDao.selectListByMemberId(loginId);
        InventoryDto ticket = userInventory.stream()
                .filter(i -> {
                    PointItemStoreDto itemInfo = pointItemDao.selectOneNumber(i.getInventoryItemNo());
                    return itemInfo != null && "RANDOM_ROULETTE".equals(itemInfo.getPointItemType());
                })
                .findFirst()
                .orElseThrow(() -> new RuntimeException("룰렛 티켓이 없습니다."));

        // 룰렛 로직 (0~5번 칸, 확률은 하드코딩)
        int idx = (int)(Math.random() * 6);
        int reward = (idx == 4) ? 2000 : (idx == 0) ? 1000 : 0;
        
        decreaseInventoryOrDelete(ticket); // 티켓 소모
        
        if (reward > 0) {
            addPoint(loginId, reward, "GET", "룰렛 당첨");
        }
        
        // 일일 퀘스트 진행도 카운트
        dailyQuestService.questProgress(loginId, "ROULETTE");
        return idx;
    }

    // --- 상점 아이템 관리용 (관리자 전용) ---
    @Transactional public void addItem(String loginId, PointItemStoreDto d) { pointItemDao.insert(d); }
    @Transactional public void editItem(String loginId, PointItemStoreDto d) { pointItemDao.update(d); }
    @Transactional public void deleteItem(String loginId, long itemNo) { pointItemDao.delete(itemNo); }
    @Transactional public void discardItem(String loginId, long inventoryNo) { inventoryDao.delete(inventoryNo); }
    
    // 위시리스트 토글 (있으면 지우고 없으면 추가)
    @Transactional public boolean toggleWish(String loginId, long itemNo) {
        PointItemWishVO vo = PointItemWishVO.builder().memberId(loginId).itemNo(itemNo).build();
        if (pointWishlistDao.checkWish(vo) > 0) { 
            pointWishlistDao.delete(vo); 
            return false; 
        } else { 
            pointWishlistDao.insert(vo); 
            return true; 
        }
    }
    
    public List<Long> getMyWishItemNos(String loginId) { 
    	return pointWishlistDao.selectMyWishItemNos(loginId);
    }
    
    public List<PointWishlistDto> getMyWishlist(String loginId) { 
    	return pointWishlistDao.selectMyWishlist(loginId); 
    }
    
    // 하트 충전 처리 (지갑 없으면 생성부터)
    @Transactional
    public void chargeHeart(String memberId, int amount) {
        if (memberId == null) throw new RuntimeException("로그인이 필요합니다.");
        if (heartDao.selectHeart(memberId) == null) {
            heartDao.createHeartWallet(memberId);
        }
        heartDao.increaseHeart(memberId, amount);
    }
}