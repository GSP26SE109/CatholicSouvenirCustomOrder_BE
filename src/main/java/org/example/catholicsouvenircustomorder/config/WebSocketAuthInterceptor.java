package org.example.catholicsouvenircustomorder.config;

import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.service.JwtService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Lấy token từ header
            List<String> authorization = accessor.getNativeHeader("Authorization");
            
            if (authorization != null && !authorization.isEmpty()) {
                String token = authorization.get(0);
                
                // Xóa "Bearer " prefix nếu có
                if (token.startsWith("Bearer ")) {
                    token = token.substring(7);
                }

                // Validate và decode token
                if (jwtService.isTokenValid(token) && jwtService.decryptToken(token)) {
                    String role = jwtService.getDataToken(token);
                    UUID accountId = jwtService.getAccountIdFromToken(token);

                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    authorities.add(new SimpleGrantedAuthority(role));

                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(accountId, null, authorities);
                    
                    accessor.setUser(authentication);
                }
            }
        }

        return message;
    }
}
