package com.tripnext.demo.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.tripnext.demo.exception.ResourceNotFoundException;
import com.tripnext.demo.exception.UnauthorizedException;
import com.tripnext.demo.model.User;
import com.tripnext.demo.repo.UserRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepo userRepo;

    public User getCurrentUser() {

        String email = getCurrentUserEmail();

        return userRepo.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );
    }

    public String getCurrentUserEmail() {
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null) {
            throw new UnauthorizedException(
                    "User is not authenticated"
            );
        }

        return authentication.getName();
    }
}