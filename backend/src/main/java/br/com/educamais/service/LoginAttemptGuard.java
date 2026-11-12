package br.com.educamais.security;

import br.com.educamais.domain.exception.BusinessRuleException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Protecao contra forca bruta: 10 falhas em 5 minutos bloqueiam o login (RNF14 / UC09-E5). */
@Component
public class LoginAttemptGuard {

    private static final int MAX_FAILURES = 10;
    private static final long WINDOW_SECONDS = 300;

    private final Map<String, Counter> failures = new ConcurrentHashMap<>();

    public void checkAllowed(String identifier) {
        Counter counter = failures.get(key(identifier));
        if (counter == null) {
            return;
        }
        synchronized (counter) {
            if (Instant.now().getEpochSecond() - counter.windowStart > WINDOW_SECONDS) {
                failures.remove(key(identifier));
                return;
            }
            if (counter.count >= MAX_FAILURES) {
                throw new BusinessRuleException("LOGIN_BLOCKED",
                        "Muitas tentativas invalidas. Tente novamente em alguns minutos.");
            }
        }
    }

    public void registerFailure(String identifier) {
        failures.compute(key(identifier), (key, existing) -> {
            long now = Instant.now().getEpochSecond();
            if (existing == null || now - existing.windowStart > WINDOW_SECONDS) {
                Counter counter = new Counter(now);
                counter.count = 1;
                return counter;
            }
            existing.count++;
            return existing;
        });
    }

    public void registerSuccess(String identifier) {
        failures.remove(key(identifier));
    }

    private static String key(String identifier) {
        return identifier == null ? "anonymous" : identifier.toLowerCase();
    }

    private static final class Counter {
        private final long windowStart;
        private int count;

        private Counter(long windowStart) {
            this.windowStart = windowStart;
        }
    }
}
