package com.chuz.reservex.ticket.reservation;

import com.chuz.reservex.ticket.config.RequirePassToken;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 예매 컨트롤러
 */
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    /**
     * 예매 생성
     * Pass Token 검증 필수 (대기열 통과 후에만 예매 가능)
     */
    @PostMapping
    @RequirePassToken
    public ResponseEntity<Reservation> createReservation(@RequestParam Long userId,
            @RequestParam Long productId, @RequestParam Integer quantity) {
        Reservation reservation = reservationService.createReservation(userId, productId, quantity);
        return ResponseEntity.ok(reservation);
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
     * 사용자별 예매 내역 조회
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Reservation>> getUserReservations(@PathVariable Long userId) {
        List<Reservation> reservations = reservationService.getUserReservations(userId);
        return ResponseEntity.ok(reservations);
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
