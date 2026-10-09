package com.manguonmo.popworld.service.impl;

import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        User user = userRepository.findByEmail(username).orElseThrow(
                ()-> new UsernameNotFoundException("Không tìm thấy tài khoản với email: "+ username)
        );
        if (!Boolean.TRUE.equals(user.getEnabled())) {
             throw new DisabledException("Tài khoản này đã bị vô hiệu hóa vui lòng liên hệ quản trị viên");
        }

        String role = user.getRole() != null && !user.getRole().isBlank() ? user.getRole() : "ROLE_USER";

        return org.springframework.security.core.userdetails.User.builder()
                .username(username)
                .password(user.getPassword())
                .disabled(false)
                .authorities(role)
                .build();


    }
}