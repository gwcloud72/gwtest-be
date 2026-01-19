package com.kh.finalproject.dao;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.kh.finalproject.dto.PointWishlistDto;
import com.kh.finalproject.vo.PointItemWishVO;

@Repository
public class PointWishlistDao {

    @Autowired
    private SqlSession sqlSession;

 // 찜 추가
 // 회원이 특정 포인트 아이템을 찜했을 때 사용
 // (이미 찜한 상태인지는 서비스 단에서 사전 체크)
 public void insert(PointItemWishVO vo) {
     sqlSession.insert("pointWishlist.insert", vo);
 }

 // 찜 삭제
 // 찜 취소 시 사용
 public void delete(PointItemWishVO vo) {
     sqlSession.delete("pointWishlist.delete", vo);
 }

 // 찜 여부 확인
 // 반환값: 1 = 이미 찜함, 0 = 찜 안함
 // 아이템 목록/상세 화면에서 하트 UI 상태 판단용
 public int checkWish(PointItemWishVO vo) {
     return sqlSession.selectOne("pointWishlist.checkWish", vo);
 }

 // 내가 찜한 아이템 번호 목록 조회
 // 프론트엔드에서 하트 표시를 빠르게 처리하기 위한 용도
 public List<Long> selectMyWishItemNos(String memberId) {
     return sqlSession.selectList("pointWishlist.selectMyWishItemNos", memberId);
 }

 // 내 찜 목록 전체 조회
 // 찜 목록 페이지에서 아이템 상세 정보와 함께 출력
 public List<PointWishlistDto> selectMyWishlist(String memberId) {
     return sqlSession.selectList("pointWishlist.selectMyWishlist", memberId);
 }
}