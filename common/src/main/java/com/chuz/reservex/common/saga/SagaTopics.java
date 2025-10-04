package com.chuz.reservex.common.saga;

/**
 * SAGA 이벤트에 사용되는 Kafka 토픽 정의
 */
public class SagaTopics {

	public static final String RESERVATION_CREATED = "saga.reservation.created";
	public static final String PAYMENT_COMPLETED = "saga.payment.completed";
	public static final String PAYMENT_FAILED = "saga.payment.failed";
	public static final String RESERVATION_CANCELLED = "saga.reservation.cancelled";

	private SagaTopics() {
		// 유틸리티 클래스
	}
}
