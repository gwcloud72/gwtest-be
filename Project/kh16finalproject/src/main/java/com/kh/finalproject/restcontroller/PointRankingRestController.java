package com.kh.finalproject.restcontroller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kh.finalproject.vo.PointRankingVO;

@RestController
@RequestMapping("/point/ranking")
@CrossOrigin
public class PointRankingRestController {
    
    @Autowired private SqlSession sqlSession;

    @GetMapping("/total")
    public Map<String, Object> getTotalRanking(
            @RequestParam(required = false) String keyword, // 검색어 (닉네임 등)
            @RequestParam(defaultValue = "1") int page,     // 현재 몇 페이지인지
            @RequestParam(defaultValue = "10") int size) {  // 한 번에 몇 개씩 보여줄 건지
        
        // MyBatis에 넘길 파라미터들 Map으로 묶기
        Map<String, Object> param = new HashMap<>();
        param.put("keyword", keyword);
        
        // 1. 전체 데이터 개수부터 파악 (페이징 계산의 기본)
        int totalCount = sqlSession.selectOne("member.countTotalRanking", param);
        
        // 2. ROWNUM 구간 계산 (1페이지: 1~10, 2페이지: 11~20...)
        int start = (page - 1) * size + 1;
        int end = page * size;
        
        param.put("start", start);
        param.put("end", end);
        
        // 3. 실제 구간만큼만 랭킹 데이터 가져오기
        List<PointRankingVO> list = sqlSession.selectList("member.totalPointRanking", param);
        
        // 4. 리스트랑 페이징 정보를 한 바구니(Map)에 담아서 리턴
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("count", totalCount);
        
        // 전체 페이지 수 계산 (나머지가 있으면 +1 페이지 하도록 센스있게 계산)
        int totalPage = (totalCount + size - 1) / size;
        result.put("totalPage", totalPage);
        
        return result;
    }
}		