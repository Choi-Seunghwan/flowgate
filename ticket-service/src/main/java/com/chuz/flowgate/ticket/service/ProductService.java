package com.chuz.flowgate.ticket.service;

import com.chuz.flowgate.ticket.entity.Product;
import com.chuz.flowgate.ticket.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 상품 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    /**
     * 전체 상품 조회
     */
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    /**
     * 상품 상세 조회
     */
    public Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
    }

    /**
     * 현재 판매 중인 상품 조회
     */
    public List<Product> getAvailableProducts() {
        LocalDateTime now = LocalDateTime.now();
        return productRepository.findBySaleStartAtBeforeAndSaleEndAtAfter(now, now);
    }

    /**
     * 재고가 있는 상품 조회
     */
    public List<Product> getProductsWithStock() {
        return productRepository.findByAvailableStockGreaterThan(0);
    }
}