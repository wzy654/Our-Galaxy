package com.ourgalaxy.file;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ourgalaxy.common.exception.BusinessException;
import com.ourgalaxy.file.dto.FileVO;
import com.ourgalaxy.space.SpaceService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

@Service
public class FileService {

    private final FileMapper fileMapper;
    private final LocalStorageService localStorageService;
    private final SpaceService spaceService;

    public FileService(FileMapper fileMapper, LocalStorageService localStorageService, SpaceService spaceService) {
        this.fileMapper = fileMapper;
        this.localStorageService = localStorageService;
        this.spaceService = spaceService;
    }

    @Transactional
    public FileVO upload(Long userId, MultipartFile multipartFile) {
        Long coupleId = spaceService.currentCoupleId(userId);
        spaceService.requireMember(userId, coupleId);
        LocalStorageService.StoredFile storedFile = localStorageService.store(multipartFile, coupleId);

        FileObject file = new FileObject();
        file.setCoupleId(coupleId);
        file.setUploaderId(userId);
        file.setStorageProvider("LOCAL");
        file.setStorageKey(storedFile.storageKey());
        file.setOriginalName(safeOriginalName(multipartFile.getOriginalFilename()));
        file.setMimeType(storedFile.mimeType());
        file.setSizeBytes(storedFile.sizeBytes());
        file.setChecksumSha256(storedFile.checksumSha256());
        file.setWidth(storedFile.width());
        file.setHeight(storedFile.height());

        try {
            fileMapper.insert(file);
        } catch (RuntimeException exception) {
            localStorageService.deleteQuietly(storedFile.storageKey());
            throw exception;
        }
        return toVO(file);
    }

    public AuthorizedFile getAuthorizedFile(Long userId, Long fileId) {
        Long coupleId = spaceService.currentCoupleId(userId);
        spaceService.requireMember(userId, coupleId);
        FileObject file = fileMapper.selectOne(Wrappers.<FileObject>lambdaQuery()
                .eq(FileObject::getId, fileId)
                .eq(FileObject::getCoupleId, coupleId));
        if (file == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "文件不存在或无权访问");
        }
        return new AuthorizedFile(file, localStorageService.load(file.getStorageKey()));
    }

    public FileObject requireFile(Long coupleId, Long fileId) {
        FileObject file = fileMapper.selectOne(Wrappers.<FileObject>lambdaQuery()
                .eq(FileObject::getId, fileId)
                .eq(FileObject::getCoupleId, coupleId));
        if (file == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "图片不存在或不属于当前空间");
        }
        return file;
    }

    public FileVO toVO(FileObject file) {
        return new FileVO(
                file.getId(),
                file.getOriginalName(),
                file.getMimeType(),
                file.getSizeBytes(),
                file.getWidth(),
                file.getHeight(),
                "/api/files/" + file.getId()
        );
    }

    private String safeOriginalName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "image";
        }
        String name = originalName.replace("\\", "/");
        name = name.substring(name.lastIndexOf('/') + 1);
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    public record AuthorizedFile(FileObject file, Path path) {
    }
}
