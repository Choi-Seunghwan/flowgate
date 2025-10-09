package com.chuz.reservex.ticket.reservation;

import com.chuz.reservex.ticket.config.RequirePassToken;
import com.chuz.reservex.ticket.saga.ReservationSagaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 예매 컨트롤러
 */
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

  private final ReservationSagaService reservationSagaService;
  private final ReservationService reservationService;

  /**
   * 예매 생성 (SAGA 패턴)
   * - JWT 인증 필수
   * - Pass Token 검증 필수 (대기열 통과 후에만 예매 가능)
   * - 예매 → 결제 → 티켓 발급 흐름 자동 처리
   */
  @PostMapping
  @RequirePassToken
  public ResponseEntity<ReservationResponse> createReservation(
      Authentication authentication,
      @RequestBody ReservationRequest request) {

    // JWT에서 userId 추출
    Long userId = (Long) authentication.getPrincipal();

    // SAGA 패턴으로 예매 생성 및 결제 프로세스 시작
    Reservation reservation = reservationSagaService.createReservationAndStartSaga(
        userId,
        request.getProductId(),
        request.getQuantity()
    );

    return ResponseEntity.ok(ReservationResponse.from(reservation));
  }

  /**
   * 예매 확정
   */
  @PutMapping("/{reservationId}/confirm")
  public ResponseEntity<Reservation> confirmReservation(@PathVariable Long reservationId) {
    Reservation reservation = reservationService.confirmReservation(reservationId);
    return ResponseEntity.ok(reservation);
  }

  /**
   * 예매 취소
   */
  @PutMapping("/{reservationId}/cancel")
  public ResponseEntity<Reservation> cancelReservation(@PathVariable Long reservationId) {
    Reservation reservation = reservationService.cancelReservation(reservationId);
    return ResponseEntity.ok(reservation);
  }

  /**
   * 내 예매 내역 조회 (JWT 인증)
   */
  @GetMapping("/my")
  public ResponseEntity<List<ReservationResponse>> getMyReservations(Authentication authentication) {
    Long userId = (Long) authentication.getPrincipal();
    List<Reservation> reservations = reservationService.getUserReservations(userId);
    return ResponseEntity.ok(reservations.stream()
        .map(ReservationResponse::from)
        .toList());
  }

  /**
   * 예매 상세 조회
   */
  @GetMapping("/{reservationId}")
  public ResponseEntity<Reservation> getReservation(@PathVariable Long reservationId) {
    Reservation reservation = reservationService.getReservation(reservationId);
    return ResponseEntity.ok(reservation);
  }
}
