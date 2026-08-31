package com.tripnext.demo.security;

import java.util.List;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.tripnext.demo.model.User;
import com.tripnext.demo.model.help.UserStatus;
import com.tripnext.demo.repo.UserRepo;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepo userRepo;

    public CustomUserDetailsService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        User user = userRepo.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found: " + email
                        ));

        boolean enabled = user.getStatus() == UserStatus.ACTIVE;

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(
                        user.getPasswordHash() == null
                                ? ""
                                : user.getPasswordHash()
                )
                .authorities(List.of())
                .disabled(!enabled)
                .build();
    }
}