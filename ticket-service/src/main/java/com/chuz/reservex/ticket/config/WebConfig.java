package com.chuz.reservex.ticket.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 설정
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

  private final PassTokenInterceptor passTokenInterceptor;

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    // Pass Token 검증이 필요한 경로에만 적용
    registry.addInterceptor(passTokenInterceptor)
        .addPathPatterns("/api/reservations/**")  // 예매 관련 API만
        .excludePathPatterns("/api/reservations/*/cancel");  // 취소는 제외 가능
  }
}
