package com.ourgalaxy.memory;

import com.ourgalaxy.common.result.ApiResponse;
import com.ourgalaxy.common.security.JwtPrincipal;
import com.ourgalaxy.memory.dto.MemoryRequest;
import com.ourgalaxy.memory.dto.MemoryVO;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/memories")
public class MemoryController {

    private final MemoryService memoryService;

    public MemoryController(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @GetMapping
    public ApiResponse<List<MemoryVO>> list(@AuthenticationPrincipal JwtPrincipal principal) {
        return ApiResponse.success(memoryService.list(principal.userId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<MemoryVO> detail(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long id
    ) {
        return ApiResponse.success(memoryService.get(principal.userId(), id));
    }

    @PostMapping
    public ApiResponse<MemoryVO> create(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody MemoryRequest request
    ) {
        return ApiResponse.success(memoryService.create(principal.userId(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<MemoryVO> update(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody MemoryRequest request
    ) {
        return ApiResponse.success(memoryService.update(principal.userId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long id
    ) {
        memoryService.delete(principal.userId(), id);
        return ApiResponse.success();
    }
}
