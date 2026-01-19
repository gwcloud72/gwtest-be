package com.kh.finalproject.dao;

import java.util.List;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import com.kh.finalproject.dto.PointItemStoreDto;
import com.kh.finalproject.vo.AdminPointItemPageVO;

@Repository
public class PointItemStoreDao {
    
    @Autowired
    private SqlSession sqlSession;


 // 상품 번호 시퀀스 조회
 // 아이템 등록 전 PK를 미리 생성해야 하는 경우 사용
 public int sequence() {
     return sqlSession.selectOne("pointitemstore.sequence");
 }

 // 포인트 상점 아이템 신규 등록 (관리자)
 public int insert(PointItemStoreDto pointItemDto) {
     return sqlSession.insert("pointitemstore.insert", pointItemDto);
 }

 // 포인트 상점 아이템 정보 수정 (가격, 재고, 노출 정보 등)
 public boolean update(PointItemStoreDto pointItemDto) {
     return sqlSession.update("pointitemstore.update", pointItemDto) > 0;
 }

 // 포인트 상점 아이템 목록 조회
 // 사용자 화면 기준 (필터 + 페이징 포함)
 public List<PointItemStoreDto> selectList(AdminPointItemPageVO vo) {
     return sqlSession.selectList("pointitemstore.selectList", vo);
 }

 // 포인트 상점 아이템 전체 개수 조회
 // 사용자 화면 페이지네이션 계산용
 public int selectCount(AdminPointItemPageVO vo) {
     return sqlSession.selectOne("pointitemstore.selectCount", vo);
 }

 // 아이템 번호 기준 단건 조회
 // 상세 페이지 / 수정 화면 진입 시 사용
 public PointItemStoreDto selectOneNumber(long pointItemNo) {
     return sqlSession.selectOne("pointitemstore.selectOneNumber", pointItemNo);
 }

 // 포인트 상점 아이템 삭제 (관리자)
 public boolean delete(long pointItemNo) { 
     return sqlSession.delete("pointitemstore.delete", pointItemNo) > 0;
 }

 // 관리자용 포인트 상점 아이템 목록 조회
 // 관리 페이지에서 전체 아이템 관리 시 사용 (페이징 + 검색)
 public List<PointItemStoreDto> selectAdminList(AdminPointItemPageVO vo) {
     return sqlSession.selectList("pointitemstore.selectAdminList", vo);
 }

 // 관리자용 아이템 전체 개수 조회
 // 관리 페이지 페이징 계산용
 public int selectAdminCount(AdminPointItemPageVO vo) {
     return sqlSession.selectOne("pointitemstore.selectPointCount", vo);
 }
    }