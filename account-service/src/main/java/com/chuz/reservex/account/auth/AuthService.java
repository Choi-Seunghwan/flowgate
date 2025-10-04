package com.chuz.reservex.account.auth;

import com.chuz.reservex.account.user.User;
import com.chuz.reservex.account.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 회원가입
     */
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        // 이메일 중복 체크
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }

        // 사용자 생성
        User user = User.builder().email(request.getEmail()).name(request.getName())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber()).build();

        User savedUser = userRepository.save(user);

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(savedUser.getId(),
                savedUser.getEmail(), savedUser.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(savedUser.getId());

        return AuthResponse.builder().accessToken(accessToken).refreshToken(refreshToken)
                .userId(savedUser.getId()).email(savedUser.getEmail()).name(savedUser.getName())
                .build();
    }

    /**
     * 로그인
     */
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 일치하지 않습니다."));

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 일치하지 않습니다.");
        }

        // JWT 토큰 생성
        String accessToken =
                jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        return AuthResponse.builder().accessToken(accessToken).refreshToken(refreshToken)
                .userId(user.getId()).email(user.getEmail()).name(user.getName()).build();
    }

    /**
     * 토큰 갱신
     */
    public AuthResponse refresh(String refreshToken) {
        // Refresh Token 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 새로운 토큰 생성
        String newAccessToken =
                jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        return AuthResponse.builder().accessToken(newAccessToken).refreshToken(newRefreshToken)
                .userId(user.getId()).email(user.getEmail()).name(user.getName()).build();
    }
}
