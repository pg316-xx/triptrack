package com.tripnext.demo.service;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.tripnext.demo.dto.user.UpdateProfileRequest;
import com.tripnext.demo.dto.user.UserProfileResponse;
import com.tripnext.demo.exception.ResourceNotFoundException;
import com.tripnext.demo.model.User;

import com.tripnext.demo.repo.UserRepo;

import lombok.RequiredArgsConstructor;
@RequiredArgsConstructor
@Service
public class UserProfileService {
    
    private final UserRepo userRepo;
    
    public UserProfileResponse getCurrentUser() {
    
    String email =
            SecurityContextHolder
                    .getContext()
                    .getAuthentication()
                    .getName();

    User user =
            userRepo.findByEmail(email)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "User not found"
                            )
                    );

    return new UserProfileResponse(
        user.getId(),
        user.getName(),
        user.getEmail(),
        user.getProfileImageUrl(),
        user.getStatus()
);
}
   public UserProfileResponse updateProfile(
        UpdateProfileRequest request
) {

    String email = SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getName();

    User user = userRepo.findByEmail(email)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "User not found"
                    )
            );

    user.setName(request.name());
    user.setProfileImageUrl(
            request.profileImageUrl()
    );

    User savedUser = userRepo.save(user);

    return new UserProfileResponse(
            savedUser.getId(),
            savedUser.getName(),
            savedUser.getEmail(),
            savedUser.getProfileImageUrl(),
            savedUser.getStatus()
    );
}
    
}
