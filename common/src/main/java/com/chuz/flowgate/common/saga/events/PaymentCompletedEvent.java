package com.chuz.flowgate.common.saga.events;

import com.chuz.flowgate.common.saga.SagaEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 결제 완료 이벤트 (payment-service -> ticket-service)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PaymentCompletedEvent extends SagaEvent {

	private Long paymentId;
	private Long reservationId;
	private Long amount;
	private String transactionId;

	public PaymentCompletedEvent(String sagaId, Long paymentId, Long reservationId, Long amount, String transactionId) {
		super(sagaId, SagaStatus.COMPLETED);
		this.paymentId = paymentId;
		this.reservationId = reservationId;
		this.amount = amount;
		this.transactionId = transactionId;
	}
}
