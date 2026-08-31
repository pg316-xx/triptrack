package com.tripnext.demo.dto.user;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(

        @Size(min = 2, max = 100)
        String name,

        String profileImageUrl

) {}