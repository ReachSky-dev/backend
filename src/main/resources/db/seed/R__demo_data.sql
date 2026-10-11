-- Repeatable Flyway migration — dev/demo data only.
-- Idempotent: delete existing seed rows by fixed IDs, then re-insert.
-- Keycloak user "id" fields match user_profiles.subject values below.
--
-- UUID v7 layout used here (RFC 9562):
--   0193a41c-0000 = timestamp ~2024-12-19 (fixed for stability)
--   7[0-5]xx      = version 7 + group discriminator
--                   0=profiles, 1=subjects, 2=listings, 3=auctions, 4=bids, 5=proxy_bids
--   8xxx          = variant 10xx + sequential counter
--   000000000001  = sequential tail

-- ── clean up ────────────────────────────────────────────────────────────────
-- Cascade-delete everything referencing seed listings to satisfy FK constraints.

DELETE FROM orders WHERE auction_id IN (SELECT id FROM auctions WHERE listing_id IN (
    '0193a41c-0000-7002-8001-000000000001',
    '0193a41c-0000-7002-8002-000000000002',
    '0193a41c-0000-7002-8003-000000000003',
    '0193a41c-0000-7002-8004-000000000004',
    '0193a41c-0000-7002-8005-000000000005',
    '0193a41c-0000-7002-8006-000000000006',
    '0193a41c-0000-7002-8007-000000000007',
    '0193a41c-0000-7002-8008-000000000008',
    '0193a41c-0000-7002-8009-000000000009',
    '0193a41c-0000-7002-8010-000000000010',
    '0193a41c-0000-7002-8011-000000000011'
));
DELETE FROM proxy_bids WHERE auction_id IN (SELECT id FROM auctions WHERE listing_id IN (
    '0193a41c-0000-7002-8001-000000000001',
    '0193a41c-0000-7002-8002-000000000002',
    '0193a41c-0000-7002-8003-000000000003',
    '0193a41c-0000-7002-8004-000000000004',
    '0193a41c-0000-7002-8005-000000000005',
    '0193a41c-0000-7002-8006-000000000006',
    '0193a41c-0000-7002-8007-000000000007',
    '0193a41c-0000-7002-8008-000000000008',
    '0193a41c-0000-7002-8009-000000000009',
    '0193a41c-0000-7002-8010-000000000010',
    '0193a41c-0000-7002-8011-000000000011'
));
DELETE FROM bids WHERE auction_id IN (SELECT id FROM auctions WHERE listing_id IN (
    '0193a41c-0000-7002-8001-000000000001',
    '0193a41c-0000-7002-8002-000000000002',
    '0193a41c-0000-7002-8003-000000000003',
    '0193a41c-0000-7002-8004-000000000004',
    '0193a41c-0000-7002-8005-000000000005',
    '0193a41c-0000-7002-8006-000000000006',
    '0193a41c-0000-7002-8007-000000000007',
    '0193a41c-0000-7002-8008-000000000008',
    '0193a41c-0000-7002-8009-000000000009',
    '0193a41c-0000-7002-8010-000000000010',
    '0193a41c-0000-7002-8011-000000000011'
));
DELETE FROM auctions WHERE listing_id IN (
    '0193a41c-0000-7002-8001-000000000001',
    '0193a41c-0000-7002-8002-000000000002',
    '0193a41c-0000-7002-8003-000000000003',
    '0193a41c-0000-7002-8004-000000000004',
    '0193a41c-0000-7002-8005-000000000005',
    '0193a41c-0000-7002-8006-000000000006',
    '0193a41c-0000-7002-8007-000000000007',
    '0193a41c-0000-7002-8008-000000000008',
    '0193a41c-0000-7002-8009-000000000009',
    '0193a41c-0000-7002-8010-000000000010',
    '0193a41c-0000-7002-8011-000000000011'
);
DELETE FROM listings WHERE id IN (
    '0193a41c-0000-7002-8001-000000000001',
    '0193a41c-0000-7002-8002-000000000002',
    '0193a41c-0000-7002-8003-000000000003',
    '0193a41c-0000-7002-8004-000000000004',
    '0193a41c-0000-7002-8005-000000000005',
    '0193a41c-0000-7002-8006-000000000006',
    '0193a41c-0000-7002-8007-000000000007',
    '0193a41c-0000-7002-8008-000000000008',
    '0193a41c-0000-7002-8009-000000000009',
    '0193a41c-0000-7002-8010-000000000010',
    '0193a41c-0000-7002-8011-000000000011'
);
DELETE FROM user_profiles WHERE subject IN (
    '0193a41c-0000-7001-8001-000000000001',
    '0193a41c-0000-7001-8002-000000000002',
    '0193a41c-0000-7001-8003-000000000003'
);

-- ── user_profiles ─────────────────────────────────────────────────────────────
-- id      = internal UUID (used as PK)
-- subject = Keycloak user "id" (= JWT sub claim)
INSERT INTO user_profiles (id, subject, display_name) VALUES
    ('0193a41c-0000-7000-8001-000000000001', '0193a41c-0000-7001-8001-000000000001', 'Demo Seller'),
    ('0193a41c-0000-7000-8002-000000000002', '0193a41c-0000-7001-8002-000000000002', 'Demo Buyer'),
    ('0193a41c-0000-7000-8003-000000000003', '0193a41c-0000-7001-8003-000000000003', 'Demo Admin');

-- ── listings ──────────────────────────────────────────────────────────────────
-- seller_id = user_profiles.subject (Keycloak sub, not internal id)
INSERT INTO listings (id, seller_id, title, description, window_starts_at, window_ends_at, capacity, status) VALUES
    ('0193a41c-0000-7002-8001-000000000001',
     '0193a41c-0000-7001-8001-000000000001',
     'Mountain cabin — Tatry',
     'Cosy wooden cabin with fireplace and panoramic views of the Tatra mountains.',
     now() + interval '2 days', now() + interval '5 days', 4, 'ACTIVE'),

    ('0193a41c-0000-7002-8002-000000000002',
     '0193a41c-0000-7001-8001-000000000001',
     'Hotel room — Kraków Old Town',
     'Double room with en-suite bathroom, 5 min walk from the Main Square.',
     now() + interval '3 days', now() + interval '4 days', 2, 'ACTIVE'),

    ('0193a41c-0000-7002-8003-000000000003',
     '0193a41c-0000-7001-8001-000000000001',
     'Beach house — Hel Peninsula',
     'Spacious house 50 m from the beach, private garden and parking.',
     now() + interval '14 days', now() + interval '21 days', 6, 'ACTIVE'),

    ('0193a41c-0000-7002-8004-000000000004',
     '0193a41c-0000-7001-8001-000000000001',
     'Ski resort room — Zakopane',
     'Slope-side studio with ski storage. Includes breakfast.',
     now() + interval '60 days', now() + interval '63 days', 2, 'ACTIVE'),

    ('0193a41c-0000-7002-8005-000000000005',
     '0193a41c-0000-7001-8001-000000000001',
     'Safari tent — Bieszczady',
     'Glamping tent in the Bieszczady wilderness. Includes dinner around the fire.',
     now() + interval '30 days', now() + interval '32 days', 2, 'ACTIVE'),

    ('0193a41c-0000-7002-8006-000000000006',
     '0193a41c-0000-7001-8001-000000000001',
     'City apartment — Warsaw Centre',
     'Modern studio near Warsaw Central station, fully equipped kitchen.',
     now() + interval '7 days', now() + interval '10 days', 2, 'ACTIVE'),

    ('0193a41c-0000-7002-8007-000000000007',
     '0193a41c-0000-7001-8001-000000000001',
     'Luxury villa — Mazury Lakes',
     'Private villa with boat dock and sauna. Still being configured.',
     now() + interval '90 days', now() + interval '97 days', 8, 'DRAFT'),

    ('0193a41c-0000-7002-8008-000000000008',
     '0193a41c-0000-7001-8001-000000000001',
     'Hostel bunk — Wrocław',
     'Withdrawn after venue closed.',
     now() + interval '1 day', now() + interval '2 days', 10, 'CLOSED'),

    ('0193a41c-0000-7002-8009-000000000009',
     '0193a41c-0000-7001-8001-000000000001',
     'Loft — Gdańsk Old Town',
     'Renovated loft steps from the Long Market. River view from the terrace.',
     now() + interval '10 days', now() + interval '13 days', 2, 'ACTIVE'),

    ('0193a41c-0000-7002-8010-000000000010',
     '0193a41c-0000-7001-8001-000000000001',
     'Pension — Karkonosze',
     'Mountain guesthouse on the Polish-Czech border. Full board included.',
     now() + interval '20 days', now() + interval '24 days', 4, 'ACTIVE'),

    ('0193a41c-0000-7002-8011-000000000011',
     '0193a41c-0000-7001-8001-000000000001',
     'Villa — Karpacz',
     'Large private villa with pool and mountain views, 10 min from ski lifts.',
     now() + interval '45 days', now() + interval '50 days', 10, 'ACTIVE');

-- ── auctions — RUNNING ENGLISH ────────────────────────────────────────────────
-- 3 running English auctions, two with active bids.
-- current_price_amount and highest_bidder_id reflect seed bids inserted below.

INSERT INTO auctions (id, listing_id, seller_id, type, status,
                      starts_at, ends_at,
                      start_price_amount, start_price_currency,
                      current_price_amount, current_price_currency,
                      min_increment_amount, min_increment_currency,
                      reserve_price_amount, reserve_price_currency,
                      highest_bidder_id, bid_count)
VALUES
    -- Tatry cabin: started 1 h ago, 2 bids placed (500 PLN → 550 PLN)
    ('0193a41c-0000-7003-8001-000000000001',
     '0193a41c-0000-7002-8001-000000000001',
     '0193a41c-0000-7001-8001-000000000001',
     'ENGLISH', 'RUNNING',
     now() - interval '1 hour', now() + interval '2 days',
     50000, 'PLN', 55000, 'PLN', 5000, 'PLN', 40000, 'PLN',
     '0193a41c-0000-7001-8002-000000000002', 2),

    -- Kraków hotel: started 30 min ago, 1 bid placed (300 PLN)
    ('0193a41c-0000-7003-8002-000000000002',
     '0193a41c-0000-7002-8002-000000000002',
     '0193a41c-0000-7001-8001-000000000001',
     'ENGLISH', 'RUNNING',
     now() - interval '30 minutes', now() + interval '3 days',
     30000, 'PLN', 30000, 'PLN', 2000, 'PLN', 25000, 'PLN',
     '0193a41c-0000-7001-8002-000000000002', 1),

    -- Warsaw apt: started 3 h ago, 1 bid placed (250 PLN)
    ('0193a41c-0000-7003-8006-000000000006',
     '0193a41c-0000-7002-8006-000000000006',
     '0193a41c-0000-7001-8001-000000000001',
     'ENGLISH', 'RUNNING',
     now() - interval '3 hours', now() + interval '5 days',
     25000, 'PLN', 25000, 'PLN', 2000, 'PLN', 20000, 'PLN',
     '0193a41c-0000-7001-8002-000000000002', 1);

-- ── auctions — RUNNING DUTCH ──────────────────────────────────────────────────
-- current_price_amount seeded at start_price; the app computes effective price
-- dynamically as: start - floor(elapsed_seconds / step_seconds) * decrement.

INSERT INTO auctions (id, listing_id, seller_id, type, status,
                      starts_at, ends_at,
                      start_price_amount, start_price_currency,
                      current_price_amount, current_price_currency,
                      decrement_amount, decrement_currency,
                      step_seconds,
                      floor_amount, floor_currency,
                      reserve_price_amount, reserve_price_currency)
VALUES
    -- Hel beach: started 2 h ago, drops 50 PLN/h, floor 700 PLN
    ('0193a41c-0000-7003-8003-000000000003',
     '0193a41c-0000-7002-8003-000000000003',
     '0193a41c-0000-7001-8001-000000000001',
     'DUTCH', 'RUNNING',
     now() - interval '2 hours', now() + interval '1 day',
     120000, 'PLN', 120000, 'PLN', 5000, 'PLN', 3600, 70000, 'PLN', 65000, 'PLN'),

    -- Zakopane ski: started 45 min ago, drops 20 PLN/30 min, floor 500 PLN
    ('0193a41c-0000-7003-8004-000000000004',
     '0193a41c-0000-7002-8004-000000000004',
     '0193a41c-0000-7001-8001-000000000001',
     'DUTCH', 'RUNNING',
     now() - interval '45 minutes', now() + interval '6 hours',
     80000, 'PLN', 80000, 'PLN', 2000, 'PLN', 1800, 50000, 'PLN', 48000, 'PLN');

-- ── auctions — SCHEDULED ──────────────────────────────────────────────────────
-- Four upcoming auctions spread over the next 3 days.

INSERT INTO auctions (id, listing_id, seller_id, type, status,
                      starts_at, ends_at,
                      start_price_amount, start_price_currency,
                      current_price_amount, current_price_currency,
                      min_increment_amount, min_increment_currency,
                      reserve_price_amount, reserve_price_currency)
VALUES
    -- Bieszczady safari: starts in ~1 hour
    ('0193a41c-0000-7003-8005-000000000005',
     '0193a41c-0000-7002-8005-000000000005',
     '0193a41c-0000-7001-8001-000000000001',
     'ENGLISH', 'SCHEDULED',
     now() + interval '1 hour', now() + interval '4 days',
     20000, 'PLN', 20000, 'PLN', 1000, 'PLN', 15000, 'PLN'),

    -- Gdańsk loft: starts in 1 day
    ('0193a41c-0000-7003-8007-000000000007',
     '0193a41c-0000-7002-8009-000000000009',
     '0193a41c-0000-7001-8001-000000000001',
     'ENGLISH', 'SCHEDULED',
     now() + interval '1 day', now() + interval '7 days',
     35000, 'PLN', 35000, 'PLN', 2000, 'PLN', 30000, 'PLN'),

    -- Karpacz villa: starts in 3 days
    ('0193a41c-0000-7003-8009-000000000009',
     '0193a41c-0000-7002-8011-000000000011',
     '0193a41c-0000-7001-8001-000000000001',
     'ENGLISH', 'SCHEDULED',
     now() + interval '3 days', now() + interval '10 days',
     60000, 'PLN', 60000, 'PLN', 5000, 'PLN', 50000, 'PLN');

-- Karkonosze pension: Dutch, starts in 2 days
INSERT INTO auctions (id, listing_id, seller_id, type, status,
                      starts_at, ends_at,
                      start_price_amount, start_price_currency,
                      current_price_amount, current_price_currency,
                      decrement_amount, decrement_currency,
                      step_seconds,
                      floor_amount, floor_currency,
                      reserve_price_amount, reserve_price_currency)
VALUES
    ('0193a41c-0000-7003-8008-000000000008',
     '0193a41c-0000-7002-8010-000000000010',
     '0193a41c-0000-7001-8001-000000000001',
     'DUTCH', 'SCHEDULED',
     now() + interval '2 days', now() + interval '9 days',
     45000, 'PLN', 45000, 'PLN', 3000, 'PLN', 3600, 25000, 'PLN', 22000, 'PLN');

-- ── bids ──────────────────────────────────────────────────────────────────────
-- Accepted bids on running English auctions.
-- bidder_id = user_profiles.subject (Keycloak sub).

INSERT INTO bids (id, auction_id, bidder_id, amount, currency, sequence, placed_at, idempotency_key) VALUES
    -- Tatry cabin — bid 1: opening bid at start price
    ('0193a41c-0000-7004-8001-000000000001',
     '0193a41c-0000-7003-8001-000000000001',
     '0193a41c-0000-7001-8002-000000000002',
     50000, 'PLN', 1,
     now() - interval '55 minutes',
     'seed-bid-tatry-1'),

    -- Tatry cabin — bid 2: raised by 50 PLN
    ('0193a41c-0000-7004-8002-000000000002',
     '0193a41c-0000-7003-8001-000000000001',
     '0193a41c-0000-7001-8002-000000000002',
     55000, 'PLN', 2,
     now() - interval '40 minutes',
     'seed-bid-tatry-2'),

    -- Kraków hotel — opening bid
    ('0193a41c-0000-7004-8003-000000000003',
     '0193a41c-0000-7003-8002-000000000002',
     '0193a41c-0000-7001-8002-000000000002',
     30000, 'PLN', 1,
     now() - interval '20 minutes',
     'seed-bid-krakow-1'),

    -- Warsaw apt — opening bid
    ('0193a41c-0000-7004-8004-000000000004',
     '0193a41c-0000-7003-8006-000000000006',
     '0193a41c-0000-7001-8002-000000000002',
     25000, 'PLN', 1,
     now() - interval '2 hours 30 minutes',
     'seed-bid-warsaw-1');

-- ── proxy_bids ────────────────────────────────────────────────────────────────
-- Demo Buyer set a proxy on Tatry cabin up to 750 PLN.

INSERT INTO proxy_bids (id, auction_id, bidder_id, max_amount, currency, created_at) VALUES
    ('0193a41c-0000-7005-8001-000000000001',
     '0193a41c-0000-7003-8001-000000000001',
     '0193a41c-0000-7001-8002-000000000002',
     75000, 'PLN',
     now() - interval '50 minutes');
