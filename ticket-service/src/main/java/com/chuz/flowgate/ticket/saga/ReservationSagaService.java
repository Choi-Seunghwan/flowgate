package com.chuz.flowgate.ticket.saga;

import com.chuz.flowgate.common.saga.SagaTopics;
import com.chuz.flowgate.common.saga.events.PaymentCompletedEvent;
import com.chuz.flowgate.common.saga.events.PaymentFailedEvent;
import com.chuz.flowgate.common.saga.events.ReservationCancelledEvent;
import com.chuz.flowgate.common.saga.events.ReservationCreatedEvent;
import com.chuz.flowgate.ticket.product.Product;
import com.chuz.flowgate.ticket.product.ProductRepository;
import com.chuz.flowgate.ticket.reservation.Reservation;
import com.chuz.flowgate.ticket.reservation.ReservationRepository;
import com.chuz.flowgate.ticket.entity.User;
import com.chuz.flowgate.ticket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 예약 SAGA 오케스트레이션 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationSagaService {

	private final ReservationRepository reservationRepository;
	private final ProductRepository productRepository;
	private final UserRepository userRepository;
	private final KafkaTemplate<String, Object> kafkaTemplate;

	/**
	 * 예약 생성 및 SAGA 시작
	 */
	@Transactional
	public Reservation createReservationAndStartSaga(Long userId, Long productId, Integer quantity) {
		log.info("예약 SAGA 시작: userId={}, productId={}, quantity={}", userId, productId, quantity);

		// SAGA ID 생성
		String sagaId = UUID.randomUUID().toString();

		// 사용자 및 상품 조회
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

		Product product = productRepository.findById(productId)
			.orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

		// 재고 감소
		product.decreaseStock(quantity);
		productRepository.save(product);

		// 예약 생성
		Reservation reservation = Reservation.create(user, product, quantity, sagaId);
		reservation.awaitPayment();
		reservationRepository.save(reservation);

		// 예약 생성 이벤트 발행 (payment-service로 전송)
		ReservationCreatedEvent event = new ReservationCreatedEvent(
			sagaId,
			reservation.getId(),
			userId,
			productId,
			quantity,
			reservation.getTotalPrice().longValue()
		);

		kafkaTemplate.send(SagaTopics.RESERVATION_CREATED, event);
		log.info("예약 생성 이벤트 발행: sagaId={}, reservationId={}", sagaId, reservation.getId());

		return reservation;
	}

	/**
	 * 결제 완료 이벤트 처리
	 */
	@KafkaListener(topics = SagaTopics.PAYMENT_COMPLETED)
	@Transactional
	public void handlePaymentCompleted(PaymentCompletedEvent event) {
		log.info("결제 완료 이벤트 수신: sagaId={}, reservationId={}", event.getSagaId(), event.getReservationId());

		Reservation reservation = reservationRepository.findById(event.getReservationId())
			.orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다."));

		// 예약 확정
		reservation.confirm();
		reservationRepository.save(reservation);

		log.info("예약 확정 완료: sagaId={}, reservationId={}", event.getSagaId(), reservation.getId());
	}

	/**
	 * 결제 실패 이벤트 처리 (보상 트랜잭션)
	 */
	@KafkaListener(topics = SagaTopics.PAYMENT_FAILED)
	@Transactional
	public void handlePaymentFailed(PaymentFailedEvent event) {
		log.info("결제 실패 이벤트 수신: sagaId={}, reservationId={}, reason={}",
			event.getSagaId(), event.getReservationId(), event.getReason());

		Reservation reservation = reservationRepository.findById(event.getReservationId())
			.orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다."));

		// 재고 복구
		Product product = reservation.getProduct();
		product.increaseStock(reservation.getQuantity());
		productRepository.save(product);

		// 예약 취소
		reservation.cancel();
		reservationRepository.save(reservation);

		// 예약 취소 이벤트 발행
		ReservationCancelledEvent cancelledEvent = new ReservationCancelledEvent(
			event.getSagaId(),
			reservation.getId(),
			"결제 실패: " + event.getReason()
		);

		kafkaTemplate.send(SagaTopics.RESERVATION_CANCELLED, cancelledEvent);
		log.info("예약 취소 완료 (보상 트랜잭션): sagaId={}, reservationId={}", event.getSagaId(), reservation.getId());
	}
}
