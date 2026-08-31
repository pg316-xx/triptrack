package com.tripnext.demo.dto.user;

import java.util.UUID;

import com.tripnext.demo.model.help.UserStatus;

public record UserProfileResponse(
        UUID id,
        String name,
        String email,
        String profileImageUrl,
        UserStatus status
) {

}