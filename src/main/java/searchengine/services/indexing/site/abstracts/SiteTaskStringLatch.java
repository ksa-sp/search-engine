package searchengine.services.indexing.site.abstracts;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * {@link searchengine.services.indexing.site.SiteTask} equal paths and lemmas concurrent synchronizer.
 */
public abstract class SiteTaskStringLatch extends SiteTaskLogger {
    private final Map<String, CountDownLatch> stringLatches = new HashMap<>();

    /**
     * Try to enter string dependant serialization.
     * <br>
     * Returns immediately.
     *
     * @param string String to capture working on.
     *
     * @return Latch object to wait for the string is free,
     * <br>or null if the string is successfully captured.
     */
    public CountDownLatch tryLockString(String string) {
        CountDownLatch lock;

        synchronized (stringLatches) {
            lock = stringLatches.get(string);

            if (lock == null) {
                stringLatches.put(string, new CountDownLatch(1));
            }
        }

        return lock;
    }

    /**
     * Enter string dependant serialization.
     * <br>
     * Waits for the string is free to work on.
     *
     * @param string String to capture working on.
     *
     * @throws InterruptedException Waiting is interrupted.
     */
    public void lockString(String string) throws InterruptedException {
        do {
            CountDownLatch lock = tryLockString(string);

            if (lock == null) {
                break;
            } else {
                lock.await();
            }
        } while (true);
    }

    /**
     * Exit from string dependant serialization.
     * <br>
     * Free the string to work on it by anther thread.
     *
     * @param string String to free.
     */
    public void unlockString(String string) {
        synchronized (stringLatches) {
            CountDownLatch lock = stringLatches.remove(string);

            if (lock != null) {
                lock.countDown();
            }
        }
    }
}
