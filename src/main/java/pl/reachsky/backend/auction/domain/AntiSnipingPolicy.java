package pl.reachsky.backend.auction.domain;

import java.time.Duration;

/**
 * Anti-sniping configuration for English auctions.
 *
 * A bid placed within {@code window} of the end time extends the auction by {@code extension},
 * up to {@code maxExtensions} times.
 *
 * Stored on Auction now; enforcement logic arrives in the bidding phase.
 */
public record AntiSnipingPolicy(Duration window, Duration extension, int maxExtensions) {}
