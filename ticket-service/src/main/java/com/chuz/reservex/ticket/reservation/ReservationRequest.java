package com.chuz.reservex.ticket.reservation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 예매 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationRequest {

  @NotNull(message = "상품 ID는 필수입니다")
  private Long productId;

  @NotNull(message = "수량은 필수입니다")
  @Min(value = 1, message = "수량은 1개 이상이어야 합니다")
  private Integer quantity;
}
