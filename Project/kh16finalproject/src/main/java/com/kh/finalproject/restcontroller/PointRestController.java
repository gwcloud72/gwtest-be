package com.kh.finalproject.restcontroller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.kh.finalproject.dto.IconDto;
import com.kh.finalproject.dto.MemberIconDto;
import com.kh.finalproject.service.IconService;
import com.kh.finalproject.service.PointService;
import com.kh.finalproject.vo.PointDonateVO;
import com.kh.finalproject.vo.PointHistoryPageVO;
import com.kh.finalproject.vo.PointUseVO;

@RestController
@RequestMapping("/point") // 엔드포인트 구조 통일
@CrossOrigin
public class PointRestController {

    @Autowired private PointService pointService;
    @Autowired private IconService iconService;

 // =============================================================
    // [공통 예외 처리] 
    // 서비스나 DAO에서 터진 에러 메시지를 낚아채서 프론트에 400(Bad Request)으로 던짐
    // =============================================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        e.printStackTrace(); // 서버 콘솔 확인용
        // RuntimeException 등에서 설정한 커스텀 메시지를 그대로 응답 바디에 담아줌
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    // =============================================================
    // [1] 포인트 이용 내역 & 후원
    // =============================================================

    /**
     * 포인트 이용 내역 (페이징 + 필터링)
     * type에 따라 전체/획득/사용 내역을 구분해서 가져옴
     */
    @GetMapping("/history")
    public ResponseEntity<PointHistoryPageVO> history(
            @RequestAttribute(required = false) String loginId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "all") String type) {
        
        if (loginId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        
        // 페이징이랑 필터 로직은 서비스에서 처리함
        PointHistoryPageVO result = pointService.getHistoryList(loginId, page, type);
        return ResponseEntity.ok(result);
    }

    /**
     * 포인트 후원 및 선물
     * 내가 가진 포인트를 다른 사람에게 쏴주는 기능
     */
    @PostMapping("/donate")
    public ResponseEntity<String> donatePoints(
            @RequestAttribute(required = false) String loginId,
            @RequestBody PointDonateVO vo) {
        
        if (loginId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        if (loginId.equals(vo.getTargetId())) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("자신에게는 보낼 수 없습니다.");
        
        // 잔액 검증이나 실제 증감 처리는 서비스단 트랜잭션 내에서 처리
        pointService.donatePoints(loginId, vo.getTargetId(), vo.getAmount());
        return ResponseEntity.ok("success");
    }

    // =============================================================
    // [2] 아이콘 관련 기능 (뽑기, 보관함, 장착)
    // =============================================================

    /**
     * 아이콘 랜덤 뽑기
     * 인벤토리의 티켓 같은 걸 소모해서 랜덤으로 아이콘 하나 획득
     */
    @PostMapping("/icon/draw")
    public ResponseEntity<IconDto> drawIcon(
            @RequestAttribute(required = false) String loginId,
            @RequestBody PointUseVO vo) {
        
        if (loginId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        
        // 뽑기 로직 실행 후 당첨된 아이콘 정보 반환
        IconDto drawnIcon = iconService.drawRandomIcon(loginId, vo.getInventoryNo());
        return ResponseEntity.ok(drawnIcon);
    }

    /**
     * 내가 보유한 아이콘 목록 조회
     */
    @GetMapping("/icon/my")
    public ResponseEntity<List<MemberIconDto>> myIcons(
            @RequestAttribute(required = false) String loginId) {
        
        if (loginId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        
        List<MemberIconDto> list = iconService.getMyIcons(loginId); 
        return ResponseEntity.ok(list);
    }
    
    /**
     * 전체 아이콘 도감 조회
     * 상점이나 도감 페이지에서 보여줄 전체 리스트
     */
    @GetMapping("/icon/all")
    public ResponseEntity<List<IconDto>> allIcons() {
        return ResponseEntity.ok(iconService.getAllIcons()); 
    }

    /**
     * 아이콘 장착 (프로필 옆에 표시)
     */
    @PostMapping("/icon/equip")
    public ResponseEntity<String> equipIcon(
            @RequestAttribute(required = false) String loginId, 
            @RequestBody IconDto dto) { 
        
        if (loginId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");
        
        // 기존 장착 해제하고 새로운 아이콘으로 업데이트
        iconService.equipIcon(loginId, dto.getIconId()); 
        return ResponseEntity.ok("success");
    }

    /**
     * 장착 중인 아이콘 해제
     */
    @PostMapping("/icon/unequip")
    public ResponseEntity<String> unequipIcon(@RequestAttribute(required = false) String loginId) {
        if (loginId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 필요");
        
        iconService.unequipIcon(loginId); 
        return ResponseEntity.ok("success");
    }
}