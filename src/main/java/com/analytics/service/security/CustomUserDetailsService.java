package com.analytics.service.security;

import com.analytics.service.entity.User;
import com.analytics.service.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Достаёт пользователя из БД по email ИЛИ по телефону
 * и превращает его роль в ROLE_*.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(login)
                .or(() -> userRepository.findByUserPhone(login))
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + login));

        String role = (user.getRole() == null || user.getRole().isBlank()) ? "USER" : user.getRole();
        String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail() != null ? user.getEmail() : user.getUserPhone())
                .password(user.getPassword())
                .authorities(new SimpleGrantedAuthority(authority))
                .build();
    }
}
