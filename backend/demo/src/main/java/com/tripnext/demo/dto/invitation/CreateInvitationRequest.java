package com.tripnext.demo.dto.invitation;

import com.tripnext.demo.model.help.TripRole;

/**
 * @param role       role the invitee gets (MEMBER by default; never OWNER)
 * @param usageLimit how many people can use the link (default 10)
 * @param email      optional: also email the link to this address
 */
public record CreateInvitationRequest(
        TripRole role,
        Integer usageLimit,
        String email
) {}
