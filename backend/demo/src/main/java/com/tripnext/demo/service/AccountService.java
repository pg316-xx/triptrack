package com.tripnext.demo.service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.tripnext.demo.exception.BadRequestException;
import com.tripnext.demo.model.TripMember;
import com.tripnext.demo.model.User;
import com.tripnext.demo.model.WalletItem;
import com.tripnext.demo.model.help.TripMemberRole;
import com.tripnext.demo.model.help.UserStatus;
import com.tripnext.demo.repo.OAuthAccountRepo;
import com.tripnext.demo.repo.PasswordResetTokenRepo;
import com.tripnext.demo.repo.RefreshTokenRepo;
import com.tripnext.demo.repo.TripMemberRepo;
import com.tripnext.demo.repo.UserRepo;
import com.tripnext.demo.repo.WalletItemRepo;
import com.tripnext.demo.service.storage.FileStorageService;
import com.tripnext.demo.service.storage.StorageBucket;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Account deletion (SRS 8.2.2 DELETE /users/me, 9.3 soft-anonymization).
 *
 * The user row is kept but anonymized, because expenses, splits and
 * settlements reference it (ON DELETE RESTRICT) and other members' balances
 * must stay correct. Personal data goes: name, email, photo, password, OAuth
 * links, sessions and private wallet files.
 */
@Service
@RequiredArgsConstructor
public class AccountService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final CurrentUserService currentUserService;
    private final UserRepo userRepo;
    private final TripMemberRepo tripMemberRepo;
    private final TripService tripService;
    private final WalletItemRepo walletItemRepo;
    private final OAuthAccountRepo oauthAccountRepo;
    private final RefreshTokenRepo refreshTokenRepo;
    private final PasswordResetTokenRepo passwordResetTokenRepo;
    private final FileStorageService fileStorageService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void deleteCurrentAccount() {
        User user = currentUserService.getCurrentUser();
        List<TripMember> memberships = tripMemberRepo.findByUser(user);

        // Refuse first, before changing anything, if a shared trip would lose its owner.
        for (TripMember m : memberships) {
            if (m.getRole() == TripMemberRole.OWNER && tripMemberRepo.findByTrip(m.getTrip()).size() > 1) {
                throw new BadRequestException(
                        "OWNS_SHARED_TRIP: Transfer ownership of \"" + m.getTrip().getTitle()
                                + "\" to another member before deleting your account");
            }
        }

        for (TripMember m : memberships) {
            if (m.getRole() == TripMemberRole.OWNER) {
                tripService.deleteTrip(m.getTrip().getId()); // trips only they are in
            } else {
                tripMemberRepo.delete(m);
            }
        }

        List<String> walletKeys = walletItemRepo.findByUserOrderByCreatedAtDesc(user)
                .stream().map(WalletItem::getStorageKey).toList();
        walletItemRepo.deleteAll(walletItemRepo.findByUserOrderByCreatedAtDesc(user));

        oauthAccountRepo.deleteByUser(user);
        refreshTokenRepo.revokeAllForUser(user);
        passwordResetTokenRepo.invalidateAllForUser(user);

        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        user.setName("Former Member");
        user.setEmail("deleted-" + user.getId() + "@deleted.invalid");
        user.setProfileImageUrl(null);
        user.setPasswordHash(passwordEncoder.encode(Base64.getEncoder().encodeToString(raw)));
        user.setStatus(UserStatus.INACTIVE);
        userRepo.save(user);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    walletKeys.forEach(k -> fileStorageService.delete(StorageBucket.WALLET, k));
                }
            });
        }
    }
}
