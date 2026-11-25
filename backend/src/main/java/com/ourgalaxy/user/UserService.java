package com.ourgalaxy.user;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ourgalaxy.auth.dto.UserVO;
import com.ourgalaxy.common.exception.BusinessException;
import com.ourgalaxy.file.FileService;
import com.ourgalaxy.user.dto.UpdateUserRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final FileService fileService;

    public UserService(UserMapper userMapper, FileService fileService) {
        this.userMapper = userMapper;
        this.fileService = fileService;
    }

    @Transactional
    public UserVO updateProfile(Long userId, UpdateUserRequest request) {
        User user = userMapper.selectByIdForUpdate(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "登录状态已失效");
        }
        if (request.avatarFileId() != null) {
            fileService.requireAvatar(userId, request.avatarFileId());
        }
        String nickname = request.nickname().trim();
        user.setNickname(nickname);
        user.setAvatarFileId(request.avatarFileId());
        userMapper.update(null, Wrappers.<User>lambdaUpdate()
                .set(User::getNickname, nickname)
                .set(User::getAvatarFileId, request.avatarFileId())
                .eq(User::getId, userId));
        return new UserVO(user.getId(), user.getEmail(), user.getNickname(), user.getAvatarFileId());
    }
}
