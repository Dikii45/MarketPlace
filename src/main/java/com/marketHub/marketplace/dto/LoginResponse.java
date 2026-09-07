package com.marketHub.marketplace.dto;

public record LoginResponse(String token, Long userId, String name, String email) {
}
