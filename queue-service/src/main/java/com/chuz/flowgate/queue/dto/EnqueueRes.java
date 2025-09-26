package com.chuz.flowgate.queue.dto;

public record EnqueueRes(String userKey, long position, int displayOffset) {
}
