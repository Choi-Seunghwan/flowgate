package com.chuz.flowgate.ticket.product;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 여행 상품 엔티티
 */
@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer totalStock; // 전체 재고

    @Column(nullable = false)
    private Integer availableStock; // 예매 가능한 재고

    @Column(nullable = false)
    private LocalDateTime saleStartAt; // 판매 시작 시간

    @Column(nullable = false)
    private LocalDateTime saleEndAt; // 판매 종료 시간

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // 재고 감소
    public void decreaseStock(int quantity) {
        if (this.availableStock < quantity) {
            throw new IllegalStateException("재고가 부족합니다.");
        }
        this.availableStock -= quantity;
    }

    // 재고 복구
    public void increaseStock(int quantity) {
        this.availableStock += quantity;
    }
}
