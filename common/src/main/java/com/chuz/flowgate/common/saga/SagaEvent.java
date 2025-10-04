package com.chuz.flowgate.common.saga;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * SAGA 패턴의 기본 이벤트 클래스
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class SagaEvent {

	private String eventId = UUID.randomUUID().toString();
	private String sagaId;
	private LocalDateTime timestamp = LocalDateTime.now();
	private SagaStatus status;

	public SagaEvent(String sagaId, SagaStatus status) {
		this.sagaId = sagaId;
		this.status = status;
	}

	public enum SagaStatus {
		STARTED,      // SAGA 시작
		COMPLETED,    // 정상 완료
		FAILED,       // 실패
		COMPENSATING, // 보상 트랜잭션 진행 중
		COMPENSATED   // 보상 완료
	}
}
