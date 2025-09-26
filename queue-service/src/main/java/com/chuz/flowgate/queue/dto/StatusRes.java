package com.chuz.flowgate.queue.dto;

public record StatusRes(long position, boolean passReady, String passToken) {
}
