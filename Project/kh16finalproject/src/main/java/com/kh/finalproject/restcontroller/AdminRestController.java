package com.kh.finalproject.restcontroller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession; // [추가]
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping; // [추가]
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody; // [추가]
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kh.finalproject.dao.BoardDao;
import com.kh.finalproject.dao.BoardReportDao;
import com.kh.finalproject.dao.DailyQuestDao;
import com.kh.finalproject.dao.InventoryDao;
import com.kh.finalproject.dao.MemberDao;
import com.kh.finalproject.dao.MemberIconDao;
import com.kh.finalproject.dao.MemberTokenDao;
import com.kh.finalproject.dao.PointHistoryDao;
import com.kh.finalproject.dto.BoardDto;

import com.kh.finalproject.dao.PointItemStoreDao;

import com.kh.finalproject.dto.IconDto;
import com.kh.finalproject.dto.InventoryDto;
import com.kh.finalproject.dto.MemberDto;
import com.kh.finalproject.dto.MemberIconDto;
import com.kh.finalproject.dto.PointHistoryDto;
import com.kh.finalproject.dto.PointItemStoreDto;
import com.kh.finalproject.dto.QuizDto;
import com.kh.finalproject.error.NeedPermissionException;
import com.kh.finalproject.error.TargetNotfoundException;
import com.kh.finalproject.service.AdminAssetService;
import com.kh.finalproject.service.AdminService;
import com.kh.finalproject.service.IconService;
import com.kh.finalproject.service.PointService;
import com.kh.finalproject.service.QuizService;
import com.kh.finalproject.service.TokenService;
import com.kh.finalproject.vo.BoardReportDetailVO;
import com.kh.finalproject.vo.BoardReportStatsVO;
import com.kh.finalproject.vo.DailyQuizVO;
import com.kh.finalproject.vo.IconPageVO;
import com.kh.finalproject.vo.PageResponseVO;
import com.kh.finalproject.vo.PageVO;
import com.kh.finalproject.vo.AdminPointItemPageVO;
import com.kh.finalproject.vo.QuizReportDetailVO;
import com.kh.finalproject.vo.QuizReportStatsVO;
import com.kh.finalproject.vo.TokenVO;

@CrossOrigin
@RestController
@RequestMapping("/admin")
public class AdminRestController {

	@Autowired
	private QuizService quizService;

	@Autowired
	private AdminService adminService;

	@Autowired
	private MemberDao memberDao;

	@Autowired
	private TokenService tokenService;

	@Autowired
	private MemberTokenDao memberTokenDao;

	@Autowired
	private IconService iconService;

	@Autowired
	private SqlSession sqlSession; // [추가] 포인트 관리 쿼리 실행을 위해 추가

	@Autowired
	private DailyQuestDao dailyQuizDao;

	@Autowired
	private InventoryDao inventoryDao;
	@Autowired
	private PointItemStoreDao pointItemStoreDao;
	@Autowired
	private MemberIconDao memberIconDao;

	@Autowired
	private BoardDao boardDao;

	@Autowired
	private BoardReportDao boardReportDao;
	
	@Autowired
	private PointService pointService;
    
	@Autowired
	private PointHistoryDao pointHistoryDao;
	@Autowired private AdminAssetService adminAssetService;

	// =============================================================
		// [1] 회원 관리 (일반 관리자용)
		// =============================================================

		// 회원 목록 조회 (검색 기능 포함, 관리자는 제외하고 보여줌)
		@GetMapping("/members") 
		public PageResponseVO getMemberList(
				@RequestParam int page,
				@RequestParam(required = false) String type, 
				@RequestParam(required = false) String keyword
				){
			PageVO pageVO = new PageVO();
			pageVO.setPage(page);

			// 검색어가 있을 때와 없을 때 구분해서 처리
			if (type != "" && keyword != "") { 
				int totalCount = memberDao.countSearchMember(type, keyword);
				pageVO.setTotalCount(totalCount);
				List<MemberDto> list = memberDao.selectAdminMemberList(type, keyword, pageVO);
				return new PageResponseVO<>(list, pageVO);
			} else { 
				int totalCount = memberDao.countMember();
				pageVO.setTotalCount(totalCount);
				List<MemberDto> list = memberDao.selectListExceptAdmin(pageVO);
				return new PageResponseVO<>(list, pageVO);
			}
		}

		// 회원 상세 정보 보기
		@GetMapping("/members/{memberId}")
		public MemberDto getMemberDetail(@PathVariable String memberId) {
			MemberDto member = memberDao.selectOne(memberId);
			if (member == null) throw new TargetNotfoundException();
			return member;
		}

		// 회원 등급(일반, 우수 등) 수동 변경
		@PatchMapping("/members/{memberId}/memberLevel")
		public void changeLevel(@PathVariable String memberId, @RequestParam String memberLevel) {
			MemberDto memberDto = memberDao.selectOne(memberId);
			if (memberDto == null) throw new TargetNotfoundException();

			memberDto.setMemberLevel(memberLevel);
			memberDao.updateMemberLevel(memberDto);
		}

		// 회원 강제 탈퇴 및 발급된 토큰 전부 무효화
		@DeleteMapping("/members/{memberId}")
		public void delete(@PathVariable String memberId, @RequestHeader("Authorization") String bearerToken) {
			MemberDto memberDto = memberDao.selectOne(memberId);
			if (memberDto == null) throw new TargetNotfoundException("존재하지 않는 회원입니다");
			
			memberDao.delete(memberId);
			
			// 로그인 세션(토큰)도 같이 날려버림
			TokenVO tokenVO = tokenService.parse(bearerToken);
			memberTokenDao.deleteByTarget(tokenVO.getLoginId());
		}

		// =============================================================
		// [2] 포인트 & 아이콘 관리 (포인트 관리자용)
		// =============================================================

		// 포인트 관리 페이지용 회원 리스트 (닉네임 검색 지원)
		@GetMapping("/point/list")
		public Map<String, Object> getPointAdminMemberList(@RequestParam(required = false) String keyword,
				@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {

			List<MemberDto> list = memberDao.selectPointAdminList(keyword, page, size);
			int totalCount = memberDao.countPointAdminList(keyword);
			int totalPage = (totalCount + size - 1) / size;

			Map<String, Object> response = new HashMap<>();
			response.put("list", list);
			response.put("totalPage", totalPage);
			response.put("totalCount", totalCount);
			return response;
		}

		// 관리자가 특정 회원 포인트 수동으로 넣어주거나 빼기
		@PostMapping("/point/update")
	    public String updatePoint(@RequestBody Map<String, Object> body) {
	        try {
	            String memberId = (String) body.get("memberId");
	            int amount = Integer.parseInt(String.valueOf(body.get("amount")));
	            pointService.adminUpdatePoint(memberId, amount);
	            return "success";
	        } catch (Exception e) {
	            e.printStackTrace();
	            return e.getMessage(); 
	        }
	    }

		// 회원 닉네임이나 등급 급하게 수정할 때 사용
		@PostMapping("/point/edit")
		public String editMemberForPointAdmin(@RequestBody MemberDto memberDto) {
			int result = sqlSession.update("member.adminUpdateMemberInfo", memberDto);
			return result > 0 ? "success" : "fail";
		}

		// 아이콘 도감 전체 목록 (카테고리/희귀도 필터링 포함)
		@GetMapping("/point/icon/list")
		public IconPageVO adminIconList(@RequestParam(defaultValue = "1") int page,
	    @RequestParam(defaultValue = "ALL") String category, @RequestParam(defaultValue = "ALL") String rarity) {
	    return iconService.getIconList(page, category, rarity);
	}

		// 새 아이콘 등록 (상점용)
		@PostMapping("/point/icon/add")
		public String addIcon(@RequestBody IconDto dto) {
			try {
				iconService.addIcon(dto);
				return "success";
			} catch (Exception e) {
				return "fail";
			}
		}

		// 아이콘 정보 수정
		@PostMapping("/point/icon/edit")
		public String editIcon(@RequestBody IconDto dto) {
			try {
				iconService.editIcon(dto);
				return "success";
			} catch (Exception e) {
				return "fail";
			}
		}

		// 아이콘 도감에서 아예 삭제
		@DeleteMapping("/point/icon/delete/{iconId}")
		public String deleteIcon(@PathVariable int iconId) {
			try {
				iconService.removeIcon(iconId);
				return "success";
			} catch (Exception e) {
				return "fail";
			}
		}

		// 특정 유저의 포인트 입출금 내역(History) 싹 긁어오기
		@GetMapping("/point/history/{memberId}")
		public Map<String, Object> getMemberPointHistory(
		        @PathVariable String memberId,
		        @RequestParam(defaultValue = "1") int page,
		        @RequestParam(defaultValue = "10") int size,
		        @RequestParam(defaultValue = "ALL") String type) {
		    int startRow = (page - 1) * size + 1;
		    int endRow = page * size;
		    
		    List<PointHistoryDto> list = pointHistoryDao.selectListByMemberIdPaging(memberId, startRow, endRow, type);
		    int totalCount = pointHistoryDao.countHistory(memberId, type);
		    int totalPage = (totalCount + size - 1) / size;

		    return Map.of("list", list, "totalPage", totalPage, "totalCount", totalCount);
		}

		// =============================================================
		// [3] 신고 및 콘텐츠 관리 (퀴즈, 게시판)
		// =============================================================

		// 신고된 퀴즈 목록 (대기/완료 상태별 페이징)
		@GetMapping("/quizzes/reports")
		public List<QuizReportStatsVO> getReportList(@RequestParam String status, @RequestAttribute TokenVO tokenVO,
				@RequestParam(defaultValue = "1") Integer page) {
			int size = 2; // 샘플용 사이즈 (운영에 맞게 조절 필요)
			int end = page * size;
			int start = end - (size - 1);

			Map<String, Object> params = new HashMap<>();
			params.put("start", start); params.put("end", end);
			params.put("loginLevel", tokenVO.getLoginLevel());
			params.put("status", status);
			
			return adminService.getReportedQuizList(params);
		}

		// 퀴즈 신고 누가 왜 했는지 상세 내역 보기
		@GetMapping("/quizzes/{quizId}/reports")
		public List<QuizReportDetailVO> getReportDetail(@PathVariable int quizId, @RequestAttribute TokenVO tokenVO) {
			return adminService.getReportDetails(tokenVO.getLoginLevel(), quizId);
		}

		// 문제 있는 퀴즈 강제 삭제
		@DeleteMapping("/quizzes/{quizId}")
		public boolean deleteQuiz(@PathVariable long quizId, @RequestAttribute TokenVO tokenVO) {
			return quizService.deleteQuiz(quizId, tokenVO.getLoginId(), tokenVO.getLoginLevel());
		}

		// 퀴즈 공개 상태 변경 (정상 -> 숨김 등)
		@PatchMapping("/quizzes/{quizId}/status/{status}")
		public boolean changeStatus(@PathVariable long quizId, @PathVariable String status,
				@RequestAttribute TokenVO tokenVO) {
			QuizDto quizDto = QuizDto.builder().quizId(quizId).quizStatus(status).build();
			return quizService.changeQuizStatus(quizDto, tokenVO.getLoginId(), tokenVO.getLoginLevel());
		}

		// [데일리 퀴즈] 관리 기능 (목록 조회, 등록, 수정, 삭제)
		@GetMapping("/dailyquiz/list")
	    public Map<String, Object> list(
	            @RequestParam(defaultValue = "1") int page,
	            @RequestParam(required = false, defaultValue = "all") String type, 
	            @RequestParam(required = false, defaultValue = "") String keyword 
	    ) {
	        int size = 10;
	        int startRow = (page - 1) * size + 1;
	        int endRow = page * size;

	        List<DailyQuizVO> list = dailyQuizDao.selectList(startRow, endRow, type, keyword);
	        int totalCount = dailyQuizDao.count(type, keyword);
	        
	        return Map.of("list", list, "totalPage", (totalCount + size - 1) / size, "currentPage", page);
	    }

	    @PostMapping("/dailyquiz/")
	    public String insert(@RequestBody DailyQuizVO vo) {
	        dailyQuizDao.insert(vo);
	        return "success";
	    }

	    @PutMapping("/dailyquiz/")
	    public String update(@RequestBody DailyQuizVO vo) {
	        return dailyQuizDao.update(vo) ? "success" : "fail";
	    }

	    @DeleteMapping("/dailyquiz/{quizNo}")
	    public String delete(@PathVariable int quizNo) {
	        return dailyQuizDao.delete(quizNo) ? "success" : "fail";
	    }

	  	// 게시판 신고 목록 조회
	  	@GetMapping("/board/reports")
	  	public List<BoardReportStatsVO> getBReportList(@RequestParam String status, @RequestAttribute TokenVO tokenVO, @RequestParam(defaultValue = "1") Integer page) {
	      	int size = 2;
	        int end = page * size;
	        int start = end - (size - 1);
	          
	        Map<String, Object> params = new HashMap<>();
	        params.put("start", start); params.put("end", end);
	  		params.put("loginLevel", tokenVO.getLoginLevel());
	  		params.put("status", status);
	  		
	  		return adminService.getReportedBoardList(params);
	  	}

	  	// 게시글 신고 상세 사유 보기
	  	@GetMapping("/board/{boardNo}/reports")
	  	public List<BoardReportDetailVO> getReportBDetail(@PathVariable int boardNo, @RequestAttribute TokenVO tokenVO) {
	         return adminService.getReportBDetails(tokenVO.getLoginLevel(), boardNo);
	    }
	  	
	  	// 신고된 원문 내용 확인
	  	@GetMapping("/board/{boardNo}/text")
	  	public BoardDto getBoardText(@PathVariable int boardNo) {
	  		return boardReportDao.selectBoardText(boardNo);
	  	}
	  	
	  	// 부적절한 게시글 삭제
	  	@DeleteMapping("/board/{boardNo}")
	    public boolean deleteBoard(@PathVariable int boardNo, @RequestAttribute TokenVO tokenVO) {
	        if(!"관리자".equals(tokenVO.getLoginLevel())) throw new NeedPermissionException();
	        return boardDao.delete(boardNo);
	    }
	  	
		// =============================================================
		// [4] 자산 관리 (아이템/아이콘 지급 및 회수)
		// =============================================================

		// 유저별 인벤토리 관리용 회원 리스트
	  	@GetMapping("/inventory/list")
	    public Map<String, Object> getInventoryAdminMembers(@RequestParam(required = false) String keyword, @RequestParam(defaultValue = "1") int page) {
	        return adminAssetService.getAdminMemberList(keyword, page);
	    }

	    // 특정 유저 인벤토리 훔쳐보기(?)
	    @GetMapping("/inventory/{memberId}")
	    public List<InventoryDto> getUserInventory(@PathVariable String memberId) {
	        return adminAssetService.getUserInventory(memberId);
	    }

	    // 유저가 가진 아이템 강제로 뺏기(회수)
	    @DeleteMapping("/inventory/{inventoryNo}")
	    public ResponseEntity<String> recallItem(@PathVariable long inventoryNo) {
	        return adminAssetService.recallAsset("item", inventoryNo) ? ResponseEntity.ok("success") : ResponseEntity.notFound().build();
	    }

	    // 상점에서 파는 마스터 아이템 목록 조회
	    @GetMapping("/inventory/item-list")
	    public Map<String, Object> getItemList(@RequestParam(defaultValue = "ALL") String type, @RequestParam(defaultValue = "") String keyword, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
	        return adminAssetService.getMasterItemList(type, keyword, page, size);
	    }

	    // 특정 유저에게 아이템 공짜로 넣어주기
	    @PostMapping("/inventory/{memberId}/{itemNo}")
	    public ResponseEntity<Void> grantItem(@PathVariable String memberId, @PathVariable long itemNo) {
	        adminAssetService.grantAsset("item", memberId, (int)itemNo);
	        return ResponseEntity.ok().build();
	    }

	    // 특정 유저가 가진 아이콘 목록 조회
	    @GetMapping("/icon/{memberId}")
	    public List<MemberIconDto> getUserIcons(@PathVariable String memberId) {
	        return adminAssetService.getUserIcons(memberId);
	    }

	    // 유저에게 줄 수 있는 전체 아이콘 리스트
	    @GetMapping("/icon/list")
	    public Map<String, Object> getIconList(@RequestParam(defaultValue = "1") int page) {
	        return adminAssetService.getMasterIconList(page);
	    }

	    // 유저에게 특정 아이콘 선물(지급) - 이미 있으면 409 에러
	    @PostMapping("/icon/{memberId}/{iconId}")
	    public ResponseEntity<String> grantIcon(@PathVariable String memberId, @PathVariable int iconId) {
	        String result = adminAssetService.grantAsset("icon", memberId, iconId);
	        if ("duplicate".equals(result)) return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 보유 중");
	        return ResponseEntity.ok("지급 완료");
	    }

	    // 유저 아이콘 강제 회수
	    @DeleteMapping("/icon/{memberIconId}")
	    public ResponseEntity<Void> recallIcon(@PathVariable long memberIconId) {
	        return adminAssetService.recallAsset("icon", memberIconId) ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
	    }

	    // 상점 아이템 목록 (관리자용 어드민 페이지 전용)
	    @GetMapping("/store/list")
	    public Map<String, Object> storeList(@ModelAttribute AdminPointItemPageVO vo) {
	        int totalCount = pointItemStoreDao.selectCount(vo);
	        List<PointItemStoreDto> list = pointItemStoreDao.selectAdminList(vo);
	        return Map.of("list", list, "totalCount", totalCount);
	    }

	    // 관리자가 상점에 새 아이템 추가
	    @PostMapping("/store/add")
	    public ResponseEntity<String> addItem(@RequestAttribute TokenVO tokenVO, @RequestBody PointItemStoreDto dto) {
	        pointService.addItem(tokenVO.getLoginId(), dto);
	        return ResponseEntity.ok("아이템이 등록되었습니다.");
	    }

	    // 상점 아이템 정보 수정
	    @PutMapping("/store/edit")
	    public ResponseEntity<String> editItem(@RequestAttribute TokenVO tokenVO, @RequestBody PointItemStoreDto dto) {
	        pointService.editItem(tokenVO.getLoginId(), dto);
	        return ResponseEntity.ok("아이템 정보가 수정되었습니다.");
	    }

	    // 상점 아이템 아예 삭제
	    @DeleteMapping("/store/delete/{itemNo}")
	    public ResponseEntity<String> deleteItem(@RequestAttribute TokenVO tokenVO, @PathVariable long itemNo) {
	        pointService.deleteItem(tokenVO.getLoginId(), itemNo);
	        return ResponseEntity.ok("아이템이 삭제되었습니다.");
	    }

	    // 유저 인벤토리 비우기
	    @DeleteMapping("/discard/{inventoryNo}")
	    public ResponseEntity<String> discardItem(@RequestAttribute TokenVO tokenVO, @PathVariable long inventoryNo) {
	        pointService.discardItem(tokenVO.getLoginId(), inventoryNo);
	        return ResponseEntity.ok("인벤토리에서 아이템을 회수했습니다.");
	    }
}	