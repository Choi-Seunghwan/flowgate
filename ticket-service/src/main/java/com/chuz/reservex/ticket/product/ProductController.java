package com.chuz.reservex.ticket.product;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 상품 컨트롤러
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 전체 상품 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        List<Product> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    /**
     * 상품 상세 조회
     */
    @GetMapping("/{productId}")
    public ResponseEntity<Product> getProduct(@PathVariable Long productId) {
        Product product = productService.getProduct(productId);
        return ResponseEntity.ok(product);
    }

    /**
     * 판매 중인 상품 조회
     */
    @GetMapping("/available")
    public ResponseEntity<List<Product>> getAvailableProducts() {
        List<Product> products = productService.getAvailableProducts();
        return ResponseEntity.ok(products);
    }

    /**
     * 재고가 있는 상품 조회
     */
    @GetMapping("/in-stock")
    public ResponseEntity<List<Product>> getProductsWithStock() {
        List<Product> products = productService.getProductsWithStock();
        return ResponseEntity.ok(products);
    }

    /**
     * 인증 테스트 API - 현재 로그인한 사용자 정보 반환
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        response.put("userId", authentication.getPrincipal());
        response.put("authorities", authentication.getAuthorities());
        response.put("authenticated", authentication.isAuthenticated());
        return ResponseEntity.ok(response);
    }
}
