package com.chuz.reservex.queue.dto;

public record StatusRes(long position, boolean passReady, String passToken) {
}
