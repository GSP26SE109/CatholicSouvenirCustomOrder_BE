package org.example.catholicsouvenircustomorder.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.catholicsouvenircustomorder.service.AccountService;
import org.example.catholicsouvenircustomorder.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class CustomSecurityFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authenHeader = request.getHeader("Authorization");
        if (authenHeader != null && authenHeader.startsWith("Bearer ")) {
            String token = authenHeader.substring(7);

            // Kiểm tra token có bị blacklist không
            if (!jwtService.isTokenValid(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            boolean isSuccess = jwtService.decryptToken(token);
            if (isSuccess) {
                String role = jwtService.getDataToken(token);
                Integer accountId = jwtService.getAccountIdFromToken(token);

                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority(role);
                authorities.add(authority);

                SecurityContext securityContext = SecurityContextHolder.getContext();
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        accountId, // Lưu accountId vào principal
                        "",
                        authorities
                );
                securityContext.setAuthentication(authenticationToken);
            }
        }
        filterChain.doFilter(request, response);
    }
}
