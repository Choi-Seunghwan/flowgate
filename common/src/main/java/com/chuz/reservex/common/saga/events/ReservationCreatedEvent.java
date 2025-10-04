package com.chuz.reservex.common.saga.events;

import com.chuz.reservex.common.saga.SagaEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 예약 생성 이벤트 (ticket-service -> payment-service)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ReservationCreatedEvent extends SagaEvent {

	private Long reservationId;
	private Long userId;
	private Long ticketId;
	private Integer quantity;
	private Long amount;

	public ReservationCreatedEvent(String sagaId, Long reservationId, Long userId, Long ticketId, Integer quantity, Long amount) {
		super(sagaId, SagaStatus.STARTED);
		this.reservationId = reservationId;
		this.userId = userId;
		this.ticketId = ticketId;
		this.quantity = quantity;
		this.amount = amount;
	}
}
