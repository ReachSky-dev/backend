package pl.reachsky.backend.shared;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * UUID v7 generator (RFC 9562).
 * Layout: 48-bit ms timestamp | version 7 | 12 random bits | variant 10 | 62 random bits.
 * No external dependency — pure Java 21.
 */
public final class Ids {

    private Ids() {}

    public static UUID next() {
        long ms = System.currentTimeMillis();
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        long rand = rng.nextLong();
        long msb = (ms << 16) | 0x7000L | (rand & 0x0FFFL);
        long lsb = (rng.nextLong() & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L;
        return new UUID(msb, lsb);
    }
}
