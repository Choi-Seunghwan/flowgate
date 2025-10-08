package com.chuz.reservex.account.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 엔티티
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String password; // 암호화된 비밀번호

  @Column(nullable = false)
  private String phoneNumber;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private UserRole role; // 사용자 권한

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
    if (role == null) {
      role = UserRole.USER; // 기본 권한
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  @Builder
  public User(String email, String name, String password, String phoneNumber, UserRole role) {
    this.email = email;
    this.name = name;
    this.password = password;
    this.phoneNumber = phoneNumber;
    this.role = role != null ? role : UserRole.USER;
  }

  // 비밀번호 변경
  public void updatePassword(String newPassword) {
    this.password = newPassword;
  }

  // 사용자 권한
  public enum UserRole {
    USER, // 일반 사용자
    ADMIN // 관리자
  }
}
