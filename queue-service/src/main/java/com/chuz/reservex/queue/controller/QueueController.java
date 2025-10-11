package com.chuz.reservex.queue.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chuz.reservex.queue.dto.EnqueueRes;
import com.chuz.reservex.queue.dto.StatusRes;
import com.chuz.reservex.queue.service.QueueService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/queue")
@RequiredArgsConstructor
public class QueueController {
  private final QueueService service;

  /**
   * 대기열 진입 - JWT 인증된 사용자만 가능
   */
  @PostMapping("/{eventId}/enqueue")
  public EnqueueRes enqueue(@PathVariable Long eventId, Authentication authentication) {
    Long userId = (Long) authentication.getPrincipal();
    return service.enqueue(eventId, userId);
  }

  /**
   * 대기열 상태 조회 - JWT 인증된 사용자만 가능
   */
  @GetMapping("/{eventId}/status")
  public StatusRes status(@PathVariable Long eventId, Authentication authentication) {
    Long userId = (Long) authentication.getPrincipal();
    return service.status(eventId, userId);
  }

  /**
   * Pass Token 검증 (ticket-service에서 호출)
   * 서비스 간 통신이므로 인증 불필요
   */
  @PostMapping("/{eventId}/validate-pass-token")
  public boolean validatePassToken(
      @PathVariable Long eventId,
      @PathVariable Long userId,
      @PathVariable String passToken) {
    return service.validateAndConsumePassToken(eventId, userId, passToken);
  }

}
