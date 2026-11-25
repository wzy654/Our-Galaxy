package com.ourgalaxy.system;

import com.ourgalaxy.common.result.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/system")
@Tag(name = "System", description = "System health and runtime information")
public class SystemController {

    private final String version;

    public SystemController(@Value("${app.version}") String version) {
        this.version = version;
    }

    @GetMapping("/health")
    @Operation(summary = "Application liveness check")
    public ApiResponse<SystemHealthVO> health() {
        return ApiResponse.success(new SystemHealthVO(
                "UP",
                "our-galaxy-backend",
                version,
                "APPLICATION",
                Instant.now()
        ));
    }
}
