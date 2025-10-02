package com.chuz.flowgate.ticket.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 상품 레포지토리
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // 판매 가능한 상품 조회
    List<Product> findBySaleStartAtBeforeAndSaleEndAtAfter(LocalDateTime now1, LocalDateTime now2);

    // 재고가 있는 상품 조회
    List<Product> findByAvailableStockGreaterThan(Integer stock);
}
