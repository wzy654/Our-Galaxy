package com.ourgalaxy.file;

import com.ourgalaxy.common.result.ApiResponse;
import com.ourgalaxy.common.security.JwtPrincipal;
import com.ourgalaxy.file.dto.FileVO;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileVO> upload(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.success(fileService.upload(principal.userId(), file));
    }

    @PostMapping("/avatar")
    public ApiResponse<FileVO> uploadAvatar(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.success(fileService.uploadAvatar(principal.userId(), file));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> download(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long id
    ) {
        FileService.AuthorizedFile authorizedFile = fileService.getAuthorizedFile(principal.userId(), id);
        FileObject file = authorizedFile.file();
        Resource resource = new FileSystemResource(authorizedFile.path());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(file.getOriginalName(), StandardCharsets.UTF_8)
                        .build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }
}
