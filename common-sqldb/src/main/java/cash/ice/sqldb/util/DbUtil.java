package cash.ice.sqldb.util;

import jakarta.persistence.OptimisticLockException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.function.Supplier;

@Slf4j
public final class DbUtil {

    private DbUtil() {
    }

    public static void optimisticLockProtection(Runnable logic, Logger log, Object object) {
        optimisticLockProtection(() -> {
            logic.run();
            return null;
        }, object, log);
    }

    public static <T> T optimisticLockProtection(Supplier<T> logic, Object object, Logger log) {
        for (int i = 0; i < 100; i++) {                 // optimistic lock protection
            try {
                return logic.get();
            } catch (OptimisticLockException | ObjectOptimisticLockingFailureException ignored) {
                log.warn("  Optimistic lock triggered, attempt: {} for: {}", i + 1, object);
            }
        }
        throw new IllegalStateException("Error! Optimistic lock triggered and all attempts are exhausted for: " + object);
    }
}
