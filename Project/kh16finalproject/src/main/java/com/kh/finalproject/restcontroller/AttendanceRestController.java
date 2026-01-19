package com.kh.finalproject.restcontroller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kh.finalproject.dto.AttendanceHistoryDto;
import com.kh.finalproject.dto.AttendanceStatusDto;
import com.kh.finalproject.service.AttendanceService;

@RestController
@RequestMapping("/point/main/attendance/")
@CrossOrigin 
public class AttendanceRestController {

	@Autowired
	private AttendanceService attendanceService;
	
	// 오늘 출석 도장 이미 찍었는지 상태 확인
		@GetMapping("/status")
		public ResponseEntity<Boolean> getStatus(@RequestAttribute String loginId) {
		    // 이미 체크했으면 true, 아니면 false 반환
		    boolean checked = attendanceService.isTodayChecked(loginId);
		    return ResponseEntity.ok(checked);
		}

		// 실제 출석 체크 버튼 눌렀을 때 처리
		@PostMapping("/check")
		public ResponseEntity<String> doCheck(
				@RequestAttribute String loginId) {
			
			try {
				attendanceService.checkAttendance(loginId);
				// 프론트에서 "success:지급포인트" 형태로 파싱해서 쓰기 때문에 포맷 맞춤
				return ResponseEntity.ok("success:100"); 
			} catch (IllegalStateException e) {
				// 이미 출석했거나 하는 등의 비즈니스 예외 처리
				return ResponseEntity.ok("fail:" + e.getMessage());
			} catch (Exception e) {
				e.printStackTrace();
				return ResponseEntity.ok("fail:에러 발생");
			}
		}

		// 내 출석 현황(날짜 리스트) 가져오기
		@GetMapping("/calendar")
		public ResponseEntity<List<String>> getCalendar(
				@RequestAttribute String loginId) { 
			
			// 캘린더 UI에 찍어줄 날짜 문자열 리스트(예: "2024-05-20") 조회
			List<String> dates = attendanceService.getMyAttendanceDates(loginId);
			return ResponseEntity.ok(dates);
		}
}