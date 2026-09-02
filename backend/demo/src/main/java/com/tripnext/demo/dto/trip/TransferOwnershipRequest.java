package com.tripnext.demo.dto.trip;

import java.util.UUID;

public record TransferOwnershipRequest(
        UUID userId
) {}