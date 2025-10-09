package com.chuz.reservex.queue.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

  @PostMapping("/{eventId}/enqueue")
  public EnqueueRes enqueue(@PathVariable Long eventId, @RequestParam String clientId) {
    return service.enqueue(eventId, clientId);
  }

  @GetMapping("/{eventId}/status")
  public StatusRes status(@PathVariable Long eventId, @RequestParam String clientId) {
    return service.status(eventId, clientId);
  }

  /**
   * Pass Token 검증 (ticket-service에서 호출)
   */
  @PostMapping("/{eventId}/validate-pass-token")
  public boolean validatePassToken(
      @PathVariable Long eventId,
      @RequestParam String clientId,
      @RequestParam String passToken) {
    return service.validateAndConsumePassToken(eventId, clientId, passToken);
  }

}
