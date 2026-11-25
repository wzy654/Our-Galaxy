package com.ourgalaxy.common.security;

public record JwtPrincipal(Long userId, String email, String tokenId) {
}
