package com.ourgalaxy.auth;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ourgalaxy.auth.dto.LoginRequest;
import com.ourgalaxy.auth.dto.RegisterRequest;
import com.ourgalaxy.auth.dto.TokenResponse;
import com.ourgalaxy.auth.dto.UserVO;
import com.ourgalaxy.common.exception.BusinessException;
import com.ourgalaxy.common.ratelimit.LoginRateLimiter;
import com.ourgalaxy.common.security.JwtProperties;
import com.ourgalaxy.common.security.JwtService;
import com.ourgalaxy.user.User;
import com.ourgalaxy.user.UserMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final LoginRateLimiter loginRateLimiter;
    private final StringRedisTemplate redisTemplate;

    public AuthService(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JwtProperties jwtProperties,
            LoginRateLimiter loginRateLimiter,
            StringRedisTemplate redisTemplate
    ) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.loginRateLimiter = loginRateLimiter;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public UserVO register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getEmail, email)) > 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "该邮箱已注册");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname().trim());
        user.setStatus(1);
        userMapper.insert(user);
        return toVO(user);
    }

    public TokenResponse login(LoginRequest request, String ip) {
        String email = request.email().trim().toLowerCase();
        loginRateLimiter.check(ip, email);
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getEmail, email));
        if (user == null || user.getStatus() == null || user.getStatus() != 1
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "邮箱或密码不正确");
        }

        JwtService.IssuedToken issuedToken = jwtService.issue(user.getId(), user.getEmail());
        return new TokenResponse(
                issuedToken.token(),
                "Bearer",
                issuedToken.expiresAt(),
                toVO(user)
        );
    }

    public UserVO me(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "登录状态已失效");
        }
        return toVO(user);
    }

    public void logout(Long userId, String tokenId) {
        String key = "auth:logout:" + userId + ":" + tokenId;
        redisTemplate.opsForValue().set(key, "1", Duration.ofMinutes(jwtProperties.accessTokenMinutes() + 1));
    }

    private UserVO toVO(User user) {
        return new UserVO(user.getId(), user.getEmail(), user.getNickname(), user.getAvatarFileId());
    }
}
