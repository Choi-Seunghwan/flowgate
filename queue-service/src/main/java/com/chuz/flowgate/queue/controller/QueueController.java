package com.chuz.flowgate.queue.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chuz.flowgate.queue.dto.EnqueueRes;
import com.chuz.flowgate.queue.dto.StatusRes;
import com.chuz.flowgate.queue.service.QueueService;

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

}
