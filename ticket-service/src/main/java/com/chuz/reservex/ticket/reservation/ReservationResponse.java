package com.chuz.reservex.ticket.reservation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 예매 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse {

  private Long id;
  private String sagaId;
  private Long userId;
  private Long productId;
  private String productName;
  private Integer quantity;
  private BigDecimal totalPrice;
  private String status;
  private LocalDateTime createdAt;

  public static ReservationResponse from(Reservation reservation) {
    return ReservationResponse.builder()
        .id(reservation.getId())
        .sagaId(reservation.getSagaId())
        .userId(reservation.getUserId())
        .productId(reservation.getProduct().getId())
        .productName(reservation.getProduct().getName())
        .quantity(reservation.getQuantity())
        .totalPrice(reservation.getTotalPrice())
        .status(reservation.getStatus().name())
        .createdAt(reservation.getCreatedAt())
        .build();
  }
}
