package com.chuz.reservex.ticket.reservation;

import com.chuz.reservex.ticket.product.Product;
import com.chuz.reservex.ticket.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 예매 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

  private final ReservationRepository reservationRepository;
  private final ProductRepository productRepository;

  /**
   * 예매 생성
   * 
   * @deprecated SAGA 패턴 사용을 위해 ReservationSagaService.createReservationAndStartSaga() 사용 권장
   */
  @Deprecated
  @Transactional
  public Reservation createReservation(Long userId, Long productId, Integer quantity) {
    Product product = productRepository.findById(productId)
        .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

    // 재고 감소
    product.decreaseStock(quantity);

    // 예매 생성
    String sagaId = UUID.randomUUID().toString();
    Reservation reservation = Reservation.create(userId, product, quantity, sagaId);

    return reservationRepository.save(reservation);
  }

  /**
   * 예매 확정
   */
  @Transactional
  public Reservation confirmReservation(Long reservationId) {
    Reservation reservation = reservationRepository.findById(reservationId)
        .orElseThrow(() -> new IllegalArgumentException("예매를 찾을 수 없습니다."));

    reservation.confirm();

    return reservation;
  }

  /**
   * 예매 취소
   */
  @Transactional
  public Reservation cancelReservation(Long reservationId) {
    Reservation reservation = reservationRepository.findById(reservationId)
        .orElseThrow(() -> new IllegalArgumentException("예매를 찾을 수 없습니다."));

    // 재고 복구
    reservation.getProduct().increaseStock(reservation.getQuantity());

    reservation.cancel();

    return reservation;
  }

  /**
   * 사용자별 예매 내역 조회
   */
  public List<Reservation> getUserReservations(Long userId) {
    return reservationRepository.findByUserId(userId);
  }

  /**
   * 예매 상세 조회
   */
  public Reservation getReservation(Long reservationId) {
    return reservationRepository.findById(reservationId)
        .orElseThrow(() -> new IllegalArgumentException("예매를 찾을 수 없습니다."));
  }
}
