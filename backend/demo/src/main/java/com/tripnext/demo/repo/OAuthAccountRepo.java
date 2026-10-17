package com.tripnext.demo.repo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tripnext.demo.model.OAuthAccount;
import com.tripnext.demo.model.User;
import com.tripnext.demo.model.help.OAuthProvider;

public interface OAuthAccountRepo extends JpaRepository<OAuthAccount, UUID>{
    
    Optional<OAuthAccount> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);

    void deleteByUser(User user);
}
