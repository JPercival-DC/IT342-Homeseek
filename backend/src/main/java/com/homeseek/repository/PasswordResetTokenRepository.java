package com.homeseek.repository;

import com.homeseek.model.PasswordResetToken;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    boolean existsByUserIdAndCreatedAtAfter(Long userId, LocalDateTime since);

    /** Single-use gate: one atomic UPDATE, so two requests with the same link cannot both succeed. 1 = taken, 0 = not usable. */
    @Transactional
    @Modifying
    @Query("update PasswordResetToken t set t.usedAt = :now "
            + "where t.tokenHash = :hash and t.usedAt is null and t.expiresAt > :now")
    int markUsed(@Param("hash") String hash, @Param("now") LocalDateTime now);

    /** Cancels every link still open for this user (a newer link was issued, or the password changed). */
    @Transactional
    @Modifying
    @Query("update PasswordResetToken t set t.usedAt = :now where t.userId = :userId and t.usedAt is null")
    int markAllUsed(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /** Housekeeping: rows whose link expired before the cutoff are no longer needed. */
    @Transactional
    @Modifying
    @Query("delete from PasswordResetToken t where t.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") LocalDateTime cutoff);
}
