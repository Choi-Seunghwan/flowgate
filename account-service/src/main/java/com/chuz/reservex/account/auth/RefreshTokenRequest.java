package com.chuz.reservex.account.auth;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 토큰 갱신 요청 DTO
 */
@Getter
@NoArgsConstructor
public class RefreshTokenRequest {

  private String refreshToken;
}
