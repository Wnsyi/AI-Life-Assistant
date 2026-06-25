package com.lifeassistant.config;

import com.lifeassistant.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuthService authService;

    /** 跨域配置 — 允许前端调用 */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }

    /** 登录拦截 — /api/agent-chat 等接口需要登录 */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor(authService))
                .addPathPatterns("/api/agent-chat", "/api/chat", "/api/forget",
                        "/api/memory-size", "/api/action-result", "/api/test-trigger",
                        "/api/conversations", "/api/conversations/**")
                .excludePathPatterns("/api/ping", "/api/register", "/api/login",
                        "/api/check-bean", "/api/test-chat", "/api/test-extract",
                        "/api/poll-notifications", "/api/pending-reminders",
                        "/api/agent-chat-stream", "/ws/**");
    }

    static class AuthInterceptor implements HandlerInterceptor {
        private final AuthService authService;

        AuthInterceptor(AuthService authService) { this.authService = authService; }

        @Override
        public boolean preHandle(HttpServletRequest request,
                                 HttpServletResponse response, Object handler) {
            if ("OPTIONS".equals(request.getMethod())) return true;

            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            Long userId = authService.validateToken(token);
            if (userId != null) {
                request.setAttribute("userId", userId);
                return true;
            }

            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            try {
                response.getWriter().write("{\"code\":401,\"message\":\"未登录或Token已过期\"}");
            } catch (Exception ignored) {}
            return false;
        }
    }
}
