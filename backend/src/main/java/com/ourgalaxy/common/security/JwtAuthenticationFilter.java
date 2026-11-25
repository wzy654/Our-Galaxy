package com.ourgalaxy.common.security;

import com.ourgalaxy.user.User;
import com.ourgalaxy.user.UserMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;

    public JwtAuthenticationFilter(JwtService jwtService, UserMapper userMapper, StringRedisTemplate redisTemplate) {
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            JwtService.ParsedToken token = jwtService.parse(authorization.substring(7));
            String logoutKey = "auth:logout:" + token.userId() + ":" + token.tokenId();
            if (Boolean.TRUE.equals(redisTemplate.hasKey(logoutKey))) {
                filterChain.doFilter(request, response);
                return;
            }

            User user = userMapper.selectById(token.userId());
            if (user == null || user.getDeleted() != 0 || user.getStatus() == null || user.getStatus() != 1) {
                filterChain.doFilter(request, response);
                return;
            }

            JwtPrincipal principal = new JwtPrincipal(user.getId(), user.getEmail(), token.tokenId());
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal, null, List.of()
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (RuntimeException ignored) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
