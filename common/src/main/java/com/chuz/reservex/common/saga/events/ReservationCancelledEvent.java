package com.chuz.reservex.common.saga.events;

import com.chuz.reservex.common.saga.SagaEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 예약 취소 이벤트 (보상 트랜잭션)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ReservationCancelledEvent extends SagaEvent {

	private Long reservationId;
	private String reason;

	public ReservationCancelledEvent(String sagaId, Long reservationId, String reason) {
		super(sagaId, SagaStatus.COMPENSATED);
		this.reservationId = reservationId;
		this.reason = reason;
	}
}
