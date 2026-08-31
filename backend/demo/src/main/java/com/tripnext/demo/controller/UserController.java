package com.tripnext.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tripnext.demo.dto.user.UpdateProfileRequest;
import com.tripnext.demo.dto.user.UserProfileResponse;
import com.tripnext.demo.service.UserProfileService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserProfileService userProfileService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> me() {
        return ResponseEntity.ok(
                userProfileService.getCurrentUser()
        );
    }
    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse>updateProfile(
        @Valid
        @RequestBody
        UpdateProfileRequest request) {

    return ResponseEntity.ok(
            userProfileService.updateProfile(
                    request
            )
    );
}

}