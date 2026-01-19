package com.kh.finalproject.restcontroller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.kh.finalproject.dto.InventoryDto;
import com.kh.finalproject.dto.PointItemStoreDto;
import com.kh.finalproject.dto.PointWishlistDto;
import com.kh.finalproject.service.PointService;
import com.kh.finalproject.dao.PointItemStoreDao;
import com.kh.finalproject.dao.InventoryDao;
import com.kh.finalproject.vo.*;

@RestController
@RequestMapping("/point/main/store")
@CrossOrigin
public class PointStoreRestController {

    @Autowired private PointService pointService;
    @Autowired private PointItemStoreDao pointItemDao;
    @Autowired private InventoryDao inventoryDao;

 // =========================================================
    // [공통 예외 처리] 
    // 컨트롤러 어디서든 에러(Exception) 터지면 여기서 잡아서 
    // 프론트에 400(Bad Request) 에러랑 메시지 같이 보내줌
    // =========================================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        e.printStackTrace(); // 서버 로그 확인용
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    // =========================================================
    // [1] 상점 기능 (조회, 구매, 선물)
    // =========================================================

    // 전체 상품 목록 가져오기 (필터, 검색, 페이징 포함)
    @GetMapping("")
    public Map<String, Object> list(
            @RequestParam(required = false, defaultValue = "ALL") String type,
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {
        
        AdminPointItemPageVO vo = new AdminPointItemPageVO();
        vo.setItemType(type);
        vo.setKeyword(keyword);
        vo.setPage(page);
        vo.setSize(size);
        
        // 검색 조건에 맞는 리스트랑 전체 개수 조회
        List<PointItemStoreDto> list = pointItemDao.selectList(vo);
        int count = pointItemDao.selectCount(vo);
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("totalCount", count);
        return result;
    }

    // 일반 구매 처리
    @PostMapping("/buy")
    public ResponseEntity<String> buy(
            @RequestAttribute(required = false) String loginId,
            @RequestBody PointBuyVO vo) {
        
        if(loginId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        
        // 포인트 차감 및 아이템 지급은 서비스에서 한 번에 처리
        pointService.purchaseItem(loginId, vo.getBuyItemNo());
        return ResponseEntity.ok("success");
    }

    // 친구에게 선물하기 (내 포인트 차감 -> 상대 인벤토리 지급)
    @PostMapping("/gift")
    public ResponseEntity<String> gift(
            @RequestAttribute(required = false) String loginId,
            @RequestBody PointGiftVO vo) {
        
        if(loginId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        
        pointService.giftItem(loginId, vo.getTargetId(), vo.getItemNo());
        return ResponseEntity.ok("success");
    }

    // =========================================================
    // [2] 인벤토리 및 아이템 관리
    // =========================================================

    // 내 보관함 전체 리스트 조회
    @GetMapping("/inventory/my")
    public List<InventoryDto> myInventory(@RequestAttribute(required = false) String loginId) {
        if(loginId == null) return List.of();
        return inventoryDao.selectListByMemberId(loginId);
    }

    // 소모성 아이템 사용 (닉네임 변경 등 추가 입력값 포함)
    @PostMapping("/inventory/use")
    public ResponseEntity<String> useItem(
            @RequestAttribute(required = false) String loginId,
            @RequestBody PointUseVO vo) {
        if(loginId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");
        
        pointService.useItem(loginId, vo.getInventoryNo(), vo.getExtraValue());
        return ResponseEntity.ok("success");
    }
    
    // 구매한 아이템 환불 처리 (포인트 복구)
    @PostMapping("/cancel")
    public ResponseEntity<String> cancel(
            @RequestAttribute(required = false) String loginId,
            @RequestBody PointCancelVO vo) {
        if(loginId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");
        
        pointService.cancelItem(loginId, vo.getInventoryNo());
        return ResponseEntity.ok("success");
    }
    
    // 필요 없는 아이템 영구 삭제 (환불 없음)
    @PostMapping("/inventory/delete")
    public ResponseEntity<String> discardItem(
            @RequestAttribute(required = false) String loginId,
            @RequestBody PointCancelVO vo) {
        if(loginId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("fail");
        
        pointService.discardItem(loginId, vo.getInventoryNo());
        return ResponseEntity.ok("success");
    }

    // 착용 중인 꾸미기 아이템 해제
    @PostMapping("/inventory/unequip")
    public ResponseEntity<String> unequipItem(
            @RequestAttribute(required = false) String loginId,
            @RequestBody PointUseVO vo) {
        if(loginId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");
        
        pointService.unequipItem(loginId, vo.getInventoryNo());
        return ResponseEntity.ok("success");
    }

    // =========================================================
    // [3] 위시리스트 (찜하기)
    // =========================================================

    // 찜하기 버튼 토글 (있으면 지우고 없으면 추가)
    @PostMapping("/wish/toggle")
    public boolean toggleWish(@RequestAttribute(required = false) String loginId, @RequestBody PointItemWishVO vo) {
        if(loginId == null) return false;
        return pointService.toggleWish(loginId, vo.getItemNo());
    }

    // 내가 찜한 상품 번호들만 조회 (아이콘 표시용)
    @GetMapping("/wish/check")
    public List<Long> myWishItemNos(@RequestAttribute(required = false) String loginId) {
        if(loginId == null) return List.of();
        return pointService.getMyWishItemNos(loginId);
    }

    // 찜 목록 상세 데이터 조회
    @GetMapping("/wish/my")
    public List<PointWishlistDto> myWishlist(@RequestAttribute(required = false) String loginId) {
        if(loginId == null) return List.of();
        return pointService.getMyWishlist(loginId);
    }

    // =========================================================
    // [4] 부가 기능 (룰렛, 내 정보 요약)
    // =========================================================

    // 룰렛 이벤트 돌리기
    @PostMapping("/roulette")
    public ResponseEntity<Integer> startRoulette(@RequestAttribute(required = false) String loginId) {
        if(loginId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(pointService.playRoulette(loginId));
    }

    // 마이페이지용 내 포인트 및 프로필 정보
    @GetMapping("/my-info")
    public MemberPointVO getMyInfo(@RequestAttribute(required = false) String loginId) {
        if (loginId == null) return null; 
        return pointService.getMyPointInfo(loginId);
    }
    
    // =========================================================
    // [5] 관리자용 상품 관리
    // =========================================================

    // 새 상품 등록
    @PostMapping("/item/add")
    public ResponseEntity<String> addItem(@RequestAttribute(required = false) String loginId, @RequestBody PointItemStoreDto dto) {
        pointService.addItem(loginId, dto);
        return ResponseEntity.ok("success");
    }
    
    // 상품 정보 수정
    @PostMapping("/item/edit")
    public ResponseEntity<String> editItem(@RequestAttribute(required = false) String loginId, @RequestBody PointItemStoreDto dto) {
        pointService.editItem(loginId, dto);
        return ResponseEntity.ok("success");
    }
    
    // 상품 완전 삭제
    @PostMapping("/item/delete")
    public ResponseEntity<String> deleteItem(@RequestAttribute(required = false) String loginId, @RequestBody PointItemStoreDto dto) {
        pointService.deleteItem(loginId, dto.getPointItemNo());
        return ResponseEntity.ok("success");
    }

    // 상품 상세 정보 조회
    @GetMapping("/detail/{itemNo}")
    public ResponseEntity<PointItemStoreDto> detail(@PathVariable long itemNo) {
        PointItemStoreDto item = pointItemDao.selectOneNumber(itemNo);
        if (item == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(item);
    }

    // 상세 페이지에서 바로 구매하기
    @PostMapping("/detail/{itemNo}/buy")
    public ResponseEntity<String> buy(
            @RequestAttribute(required = false) String loginId,
            @PathVariable long itemNo) { 

        if (loginId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        pointService.purchaseItem(loginId, itemNo);
        return ResponseEntity.ok("success");
    }
}