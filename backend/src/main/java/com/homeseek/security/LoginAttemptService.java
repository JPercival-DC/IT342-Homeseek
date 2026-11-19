package com.homeseek.security;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;

/**
 * Blocks an email for 15 minutes after 5 failed logins within 15 minutes (SRS security requirement).
 * ponytail: counters live in memory, so they reset on restart and are per instance. Upgrade path: move the
 * counters to the database (or a shared cache) when running more than one backend instance.
 */
@Service
public class LoginAttemptService {

    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);
    private static final int CLEANUP_THRESHOLD = 10_000;

    private record State(int failures, Instant windowStart, Instant lockedUntil) {}

    private final ConcurrentMap<String, State> states = new ConcurrentHashMap<>();

    /** Time left on the lock for this email, or Duration.ZERO if it is not locked. */
    public Duration remainingLock(String email) {
        State s = states.get(email);
        if (s == null || s.lockedUntil() == null) {
            return Duration.ZERO;
        }
        Duration left = Duration.between(Instant.now(), s.lockedUntil());
        return left.isNegative() ? Duration.ZERO : left;
    }

    public void recordFailure(String email) {
        Instant now = Instant.now();
        if (states.size() > CLEANUP_THRESHOLD) {
            states.values().removeIf(s -> now.isAfter(s.windowStart().plus(WINDOW)) && !isLocked(s, now));
        }
        states.compute(email, (k, s) -> {
            if (s != null && isLocked(s, now)) {
                return s;                                   // already locked: keep the original lock time
            }
            if (s == null || now.isAfter(s.windowStart().plus(WINDOW))) {
                s = new State(0, now, null);                // first failure, or the 15-minute window (or lock) ended
            }
            int failures = s.failures() + 1;
            return failures >= MAX_FAILURES
                    ? new State(failures, s.windowStart(), now.plus(WINDOW))
                    : new State(failures, s.windowStart(), null);
        });
    }

    public void reset(String email) {
        states.remove(email);
    }

    private static boolean isLocked(State s, Instant now) {
        return s.lockedUntil() != null && now.isBefore(s.lockedUntil());
    }
}
