package com.chuz.flowgate.common.saga.events;

import com.chuz.flowgate.common.saga.SagaEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 결제 실패 이벤트 (payment-service -> ticket-service)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PaymentFailedEvent extends SagaEvent {

	private Long reservationId;
	private String reason;

	public PaymentFailedEvent(String sagaId, Long reservationId, String reason) {
		super(sagaId, SagaStatus.FAILED);
		this.reservationId = reservationId;
		this.reason = reason;
	}
}
