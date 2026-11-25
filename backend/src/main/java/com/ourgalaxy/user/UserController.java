package com.ourgalaxy.user;

import com.ourgalaxy.auth.dto.UserVO;
import com.ourgalaxy.common.result.ApiResponse;
import com.ourgalaxy.common.security.JwtPrincipal;
import com.ourgalaxy.user.dto.UpdateUserRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PutMapping("/me")
    public ApiResponse<UserVO> updateProfile(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return ApiResponse.success(userService.updateProfile(principal.userId(), request));
    }
}
