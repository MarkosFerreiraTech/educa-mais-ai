package br.com.educamais.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rate limiting por IP com janela fixa de 60 s (RNF14): 60 req/min por usuario
 * e 10 req/min no endpoint de login.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int DEFAULT_LIMIT = 60;
    private static final int LOGIN_LIMIT = 10;
    private static final long WINDOW_SECONDS = 60;
    private static final String LOGIN_PATH = "/api/v1/auth/login";

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String clientKey = resolveClientKey(request);
        int limit = request.getRequestURI().equals(LOGIN_PATH) ? LOGIN_LIMIT : DEFAULT_LIMIT;
        Window window = windows.computeIfAbsent(clientKey, key -> new Window(Instant.now().getEpochSecond()));
        long now = Instant.now().getEpochSecond();
        synchronized (window) {
            if (now - window.start >= WINDOW_SECONDS) {
                window.start = now;
                window.count = 0;
            }
            window.count++;
            if (window.count > limit) {
                response.setStatus(429);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setHeader("Retry-After", String.valueOf(WINDOW_SECONDS - (now - window.start)));
                response.getWriter().write("""
                        {"status":429,"code":"RATE_LIMIT_EXCEEDED","message":"Muitas requisicoes"}""");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/actuator") || uri.startsWith("/swagger-ui") || uri.startsWith("/v3/api-docs");
    }

    private String resolveClientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = forwarded != null && !forwarded.isBlank()
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
        return ip + "|" + request.getRequestURI();
    }

    private static final class Window {
        private long start;
        private int count;

        private Window(long start) {
            this.start = start;
        }
    }
}
