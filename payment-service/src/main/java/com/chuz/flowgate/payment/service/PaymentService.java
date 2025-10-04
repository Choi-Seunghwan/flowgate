package com.chuz.flowgate.payment.service;

import com.chuz.flowgate.common.saga.SagaTopics;
import com.chuz.flowgate.common.saga.events.PaymentCompletedEvent;
import com.chuz.flowgate.common.saga.events.PaymentFailedEvent;
import com.chuz.flowgate.common.saga.events.ReservationCreatedEvent;
import com.chuz.flowgate.payment.domain.Payment;
import com.chuz.flowgate.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

	private final PaymentRepository paymentRepository;
	private final KafkaTemplate<String, Object> kafkaTemplate;

	/**
	 * 예약 생성 이벤트를 수신하여 결제 처리
	 */
	@KafkaListener(topics = SagaTopics.RESERVATION_CREATED)
	@Transactional
	public void handleReservationCreated(ReservationCreatedEvent event) {
		log.info("예약 생성 이벤트 수신: sagaId={}, reservationId={}", event.getSagaId(), event.getReservationId());

		// 결제 엔티티 생성
		Payment payment = Payment.builder()
			.sagaId(event.getSagaId())
			.reservationId(event.getReservationId())
			.userId(event.getUserId())
			.amount(event.getAmount())
			.status(Payment.PaymentStatus.PROCESSING)
			.build();

		paymentRepository.save(payment);

		try {
			// 실제 결제 처리 로직 (외부 PG사 연동 등)
			String transactionId = processPayment(payment);

			// 결제 완료 처리
			payment.complete(transactionId);
			paymentRepository.save(payment);

			// 결제 완료 이벤트 발행
			PaymentCompletedEvent completedEvent = new PaymentCompletedEvent(
				event.getSagaId(),
				payment.getId(),
				payment.getReservationId(),
				payment.getAmount(),
				transactionId
			);

			kafkaTemplate.send(SagaTopics.PAYMENT_COMPLETED, completedEvent);
			log.info("결제 완료 이벤트 발행: sagaId={}, paymentId={}", event.getSagaId(), payment.getId());

		} catch (Exception e) {
			log.error("결제 처리 실패: sagaId={}, error={}", event.getSagaId(), e.getMessage(), e);

			// 결제 실패 처리
			payment.fail(e.getMessage());
			paymentRepository.save(payment);

			// 결제 실패 이벤트 발행
			PaymentFailedEvent failedEvent = new PaymentFailedEvent(
				event.getSagaId(),
				payment.getReservationId(),
				e.getMessage()
			);

			kafkaTemplate.send(SagaTopics.PAYMENT_FAILED, failedEvent);
			log.info("결제 실패 이벤트 발행: sagaId={}, reason={}", event.getSagaId(), e.getMessage());
		}
	}

	/**
	 * 실제 결제 처리 (PG사 연동 등)
	 * 현재는 Mock 구현
	 */
	private String processPayment(Payment payment) {
		log.info("결제 처리 시작: paymentId={}, amount={}", payment.getId(), payment.getAmount());

		// TODO: 실제 PG사 API 호출
		// 현재는 Mock으로 성공 처리
		// 테스트를 위해 일정 확률로 실패하도록 할 수도 있음
		if (payment.getAmount() < 0) {
			throw new IllegalArgumentException("결제 금액이 유효하지 않습니다");
		}

		// Mock Transaction ID 생성
		String transactionId = "TXN-" + UUID.randomUUID().toString();
		log.info("결제 처리 완료: transactionId={}", transactionId);

		return transactionId;
	}
}
