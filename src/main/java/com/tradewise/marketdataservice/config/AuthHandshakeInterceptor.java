package com.tradewise.marketdataservice.config;

import com.tradewise.marketdataservice.service.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@Component
public class AuthHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    @Autowired
    public AuthHandshakeInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            HttpServletRequest servletReq = ((ServletServerHttpRequest) request).getServletRequest();
            String token = servletReq.getParameter("token"); // if client uses ?token=...

            if (token == null) {
                // Try to get token from cookies
                if (servletReq.getCookies() != null) {
                    for (Cookie c : servletReq.getCookies()) {
                        if ("ws_token".equals(c.getName())) { // Assuming a cookie named 'ws_token'
                            token = c.getValue();
                            break;
                        }
                    }
                }
            }

            if (token != null && jwtUtil.validateToken(token)) {
                // If valid, put user/principal into attributes
                attributes.put("userEmail", jwtUtil.extractEmail(token));
                attributes.put("userId", jwtUtil.extractUserId(token));
                System.out.println("WebSocket handshake: Token found and valid for user: " + jwtUtil.extractEmail(token)); // For debugging
                return true; // Allow handshake
            }
        }
        System.out.println("WebSocket handshake: No valid token found. Rejecting connection."); // For debugging
        return false; // Reject handshake if no valid token is found
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // Nothing to do after handshake
    }
}
