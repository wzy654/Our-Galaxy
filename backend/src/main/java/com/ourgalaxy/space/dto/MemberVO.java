package com.ourgalaxy.space.dto;

import java.time.LocalDateTime;

public record MemberVO(Long userId, String nickname, String role, LocalDateTime joinedAt) {
}
