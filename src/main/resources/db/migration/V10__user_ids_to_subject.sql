-- Migrate all user-facing ID columns from internal profile primary key (user_profiles.id)
-- to Keycloak subject UUID (user_profiles.subject).
--
-- After this migration the application uses the JWT sub claim as the canonical user
-- identifier, which aligns with what the frontend reads from auth.user.profile.sub.
-- SecurityContextCurrentUserProvider was updated in the same release to derive
-- CurrentUser.id() from the JWT sub rather than the profile primary key.

UPDATE listings l
SET seller_id = up.subject::uuid
FROM user_profiles up
WHERE l.seller_id = up.id;

UPDATE auctions a
SET seller_id = up.subject::uuid
FROM user_profiles up
WHERE a.seller_id = up.id;

UPDATE auctions a
SET highest_bidder_id = up.subject::uuid
FROM user_profiles up
WHERE a.highest_bidder_id IS NOT NULL
  AND a.highest_bidder_id = up.id;

UPDATE auctions a
SET winner_id = up.subject::uuid
FROM user_profiles up
WHERE a.winner_id IS NOT NULL
  AND a.winner_id = up.id;

UPDATE bids b
SET bidder_id = up.subject::uuid
FROM user_profiles up
WHERE b.bidder_id = up.id;

UPDATE proxy_bids pb
SET bidder_id = up.subject::uuid
FROM user_profiles up
WHERE pb.bidder_id = up.id;

UPDATE orders o
SET buyer_id = up.subject::uuid
FROM user_profiles up
WHERE o.buyer_id = up.id;
