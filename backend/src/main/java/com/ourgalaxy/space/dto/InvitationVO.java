package com.ourgalaxy.space.dto;

import java.time.LocalDateTime;

public record InvitationVO(String code, String status, LocalDateTime expiresAt) {
}
