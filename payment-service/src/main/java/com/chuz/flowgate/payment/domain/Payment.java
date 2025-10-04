package com.chuz.flowgate.payment.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String sagaId;

	@Column(nullable = false)
	private Long reservationId;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false)
	private Long amount;

	@Column(unique = true)
	private String transactionId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PaymentStatus status;

	@Column(length = 500)
	private String failureReason;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	private LocalDateTime completedAt;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
	}

	public void complete(String transactionId) {
		this.transactionId = transactionId;
		this.status = PaymentStatus.COMPLETED;
		this.completedAt = LocalDateTime.now();
	}

	public void fail(String reason) {
		this.status = PaymentStatus.FAILED;
		this.failureReason = reason;
		this.completedAt = LocalDateTime.now();
	}

	public enum PaymentStatus {
		PENDING,    // 결제 대기
		PROCESSING, // 결제 처리 중
		COMPLETED,  // 결제 완료
		FAILED      // 결제 실패
	}
}
