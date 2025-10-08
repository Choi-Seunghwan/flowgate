package com.chuz.reservex.account.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원가입 요청 DTO
 */
@Getter
@NoArgsConstructor
public class SignupRequest {
  private String email;
  private String name;
  private String password;
  private String phoneNumber;
}
