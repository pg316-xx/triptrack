package com.tripnext.demo.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tripnext.demo.dto.audit.AuditLogResponse;
import com.tripnext.demo.service.AuditLogService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/trips/{tripId}/audit-logs")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<Page<AuditLogResponse>> getTripAuditLogs(
            @PathVariable UUID tripId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                auditLogService.getTripAuditLogs(tripId, pageable)
        );
    }
}
