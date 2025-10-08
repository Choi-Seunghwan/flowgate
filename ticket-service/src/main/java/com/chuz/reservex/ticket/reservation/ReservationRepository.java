package com.chuz.reservex.ticket.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 예매 레포지토리
 */
@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

  // 사용자별 예매 내역 조회
  List<Reservation> findByUserId(Long userId);

  Optional<Reservation> findBySagaId(String sagaId);

  // 상품별 예매 내역 조회
  List<Reservation> findByProductId(Long productId);

  // 사용자의 특정 상태 예매 조회
  List<Reservation> findByUserIdAndStatus(Long userId, Reservation.ReservationStatus status);
}
