package com.chuz.reservex.account.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 사용자 레포지토리
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  // 이메일로 사용자 조회
  Optional<User> findByEmail(String email);

  // 이메일 중복 체크
  boolean existsByEmail(String email);
}
