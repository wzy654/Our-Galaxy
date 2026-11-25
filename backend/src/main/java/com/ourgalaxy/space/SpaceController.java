package com.ourgalaxy.space;

import com.ourgalaxy.common.result.ApiResponse;
import com.ourgalaxy.common.security.JwtPrincipal;
import com.ourgalaxy.space.dto.CreateSpaceRequest;
import com.ourgalaxy.space.dto.InvitationVO;
import com.ourgalaxy.space.dto.JoinSpaceRequest;
import com.ourgalaxy.space.dto.SpaceVO;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/spaces")
public class SpaceController {

    private final SpaceService spaceService;

    public SpaceController(SpaceService spaceService) {
        this.spaceService = spaceService;
    }

    @GetMapping("/current")
    public ApiResponse<SpaceVO> current(@AuthenticationPrincipal JwtPrincipal principal) {
        return ApiResponse.success(spaceService.currentSpace(principal.userId()));
    }

    @PostMapping
    public ApiResponse<SpaceVO> create(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody CreateSpaceRequest request
    ) {
        return ApiResponse.success(spaceService.createSpace(principal.userId(), request));
    }

    @PostMapping("/invitations")
    public ApiResponse<InvitationVO> invite(@AuthenticationPrincipal JwtPrincipal principal) {
        return ApiResponse.success(spaceService.createInvitation(principal.userId()));
    }

    @PostMapping("/join")
    public ApiResponse<SpaceVO> join(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody JoinSpaceRequest request
    ) {
        return ApiResponse.success(spaceService.joinSpace(principal.userId(), request));
    }
}
