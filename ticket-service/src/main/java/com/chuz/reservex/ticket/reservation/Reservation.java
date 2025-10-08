package com.chuz.reservex.ticket.reservation;

import com.chuz.reservex.ticket.product.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 예매 엔티티
 */
@Entity
@Table(name = "reservations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String sagaId; // SAGA 트랜잭션 ID

    @Column(nullable = false)
    private Long userId; // account-service의 User ID 참조

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity; // 예매 수량

    @Column(nullable = false)
    private BigDecimal totalPrice; // 총 결제 금액

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status; // 예매 상태

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

    // 예매 생성
    public static Reservation create(Long userId, Product product, Integer quantity, String sagaId) {
        Reservation reservation = new Reservation();
        reservation.sagaId = sagaId;
        reservation.userId = userId;
        reservation.product = product;
        reservation.quantity = quantity;
        reservation.totalPrice = product.getPrice().multiply(BigDecimal.valueOf(quantity));
        reservation.status = ReservationStatus.PENDING;
        return reservation;
    }

    // 결제 대기 상태로 변경
    public void awaitPayment() {
        if (this.status != ReservationStatus.PENDING) {
            throw new IllegalStateException("대기 중인 예매만 결제 대기 상태로 변경할 수 있습니다.");
        }
        this.status = ReservationStatus.PAYMENT_PENDING;
    }

    // 예매 확정 (결제 완료 후)
    public void confirm() {
        if (this.status != ReservationStatus.PAYMENT_PENDING) {
            throw new IllegalStateException("결제 대기 중인 예매만 확정할 수 있습니다.");
        }
        this.status = ReservationStatus.CONFIRMED;
    }

    // 예매 취소
    public void cancel() {
        if (this.status == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("이미 취소된 예매입니다.");
        }
        this.status = ReservationStatus.CANCELLED;
    }

    public enum ReservationStatus {
        PENDING,          // 대기 (예약 생성됨, 결제 대기 중)
        PAYMENT_PENDING,  // 결제 처리 중
        CONFIRMED,        // 확정 (결제 완료)
        CANCELLED         // 취소 (결제 실패 또는 사용자 취소)
    }
}