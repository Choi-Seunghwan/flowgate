package com.chuz.reservex.account.auth.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 인증 응답 DTO
 */
@Getter
@Builder
public class AuthResponse {

  private String accessToken;
  private String refreshToken;
  private Long userId;
  private String email;
  private String name;
}
