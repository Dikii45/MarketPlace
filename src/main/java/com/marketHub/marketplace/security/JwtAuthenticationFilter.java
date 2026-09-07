package com.marketHub.marketplace.security;

import com.marketHub.marketplace.models.User;
import com.marketHub.marketplace.repositories.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// читает "Authorization: Bearer <token>" на /api/** и кладёт пользователя в SecurityContext,
// если токен валиден — дальше Principal работает так же, как и в веб-версии на сессиях
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        //проверяем что header это JWT
        if (header != null && header.startsWith("Bearer ")) {
            //достаем токен
            String token = header.substring(7);

            if (jwtService.isValid(token)) {
                //проверяем токен и достаем user по почте
                String email = jwtService.extractEmail(token);
                User user = userRepository.findByEmail(email);

                if (user != null && user.isActive() && SecurityContextHolder.getContext().getAuthentication() == null) {

                    //передаем данные в SecurityContextHolder
                    var authToken = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    //заполняем данные о текущем HTTP-запросе
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        }

        //продолжаем дальше)(вызови Controller)
        filterChain.doFilter(request, response);
    }
}
