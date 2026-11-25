package com.ourgalaxy.common.ratelimit;

import com.ourgalaxy.common.exception.BusinessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class LoginRateLimiter {

    private static final DefaultRedisScript<Long> INCREMENT_SCRIPT = new DefaultRedisScript<>(
            "local current = redis.call('INCR', KEYS[1]); "
                    + "if current == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end; "
                    + "return current",
            Long.class
    );

    private final StringRedisTemplate redisTemplate;

    public LoginRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void check(String ip, String email) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        String key = "auth:login:" + ip + ":" + Integer.toHexString(normalizedEmail.hashCode());
        Long attempts;
        try {
            attempts = redisTemplate.execute(INCREMENT_SCRIPT, List.of(key), "900");
        } catch (RuntimeException exception) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "登录服务暂时不可用，请稍后重试");
        }
        if (attempts != null && attempts > 10) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "登录尝试过于频繁，请 15 分钟后再试");
        }
    }
}
