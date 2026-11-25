package com.ourgalaxy.space.dto;

import java.time.LocalDate;
import java.util.List;

public record SpaceVO(
        Long id,
        String name,
        LocalDate anniversaryDate,
        String description,
        String role,
        int memberCount,
        List<MemberVO> members
) {
}
