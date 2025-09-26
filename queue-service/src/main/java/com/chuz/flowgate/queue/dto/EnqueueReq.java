package com.chuz.flowgate.queue.dto;

public record EnqueueReq(Long eventId, String clientId) {
}
