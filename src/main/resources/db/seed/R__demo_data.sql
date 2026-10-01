-- Repeatable Flyway migration — dev/demo data only.
-- Idempotent: delete existing seed rows by fixed IDs, then re-insert.
-- Keycloak user "id" fields match user_profiles.subject values below.

-- ── clean up ────────────────────────────────────────────────────────────────
DELETE FROM auctions WHERE id IN (
    '00000000-0000-4000-d000-000000000001',
    '00000000-0000-4000-d000-000000000002',
    '00000000-0000-4000-d000-000000000003',
    '00000000-0000-4000-d000-000000000004',
    '00000000-0000-4000-d000-000000000005'
);

DELETE FROM listings WHERE id IN (
    '00000000-0000-4000-c000-000000000001',
    '00000000-0000-4000-c000-000000000002',
    '00000000-0000-4000-c000-000000000003',
    '00000000-0000-4000-c000-000000000004',
    '00000000-0000-4000-c000-000000000005',
    '00000000-0000-4000-c000-000000000006',
    '00000000-0000-4000-c000-000000000007',
    '00000000-0000-4000-c000-000000000008'
);

DELETE FROM user_profiles WHERE subject IN (
    '00000000-0000-4000-a000-000000000001',
    '00000000-0000-4000-a000-000000000002',
    '00000000-0000-4000-a000-000000000003'
);

-- ── user_profiles ────────────────────────────────────────────────────────────
-- id             = internal UUID used as seller_id / buyer_id in listings & auctions
-- subject        = Keycloak user "id" (= JWT sub claim)
INSERT INTO user_profiles (id, subject, display_name) VALUES
    ('00000000-0000-4000-b000-000000000001', '00000000-0000-4000-a000-000000000001', 'Demo Seller'),
    ('00000000-0000-4000-b000-000000000002', '00000000-0000-4000-a000-000000000002', 'Demo Buyer'),
    ('00000000-0000-4000-b000-000000000003', '00000000-0000-4000-a000-000000000003', 'Demo Admin');

-- ── listings ────────────────────────────────────────────────────────────────
-- seller_id references user_profiles.id (NOT Keycloak subject)
INSERT INTO listings (id, seller_id, title, description, window_starts_at, window_ends_at, capacity, status) VALUES
    ('00000000-0000-4000-c000-000000000001',
     '00000000-0000-4000-b000-000000000001',
     'Mountain cabin — Tatry',
     'Cosy wooden cabin with fireplace and panoramic views of the Tatra mountains.',
     now() + interval '2 days', now() + interval '5 days', 4, 'ACTIVE'),

    ('00000000-0000-4000-c000-000000000002',
     '00000000-0000-4000-b000-000000000001',
     'Hotel room — Kraków Old Town',
     'Double room with en-suite bathroom, 5 min walk from the Main Square.',
     now() + interval '3 days', now() + interval '4 days', 2, 'ACTIVE'),

    ('00000000-0000-4000-c000-000000000003',
     '00000000-0000-4000-b000-000000000001',
     'Beach house — Hel Peninsula',
     'Spacious house 50 m from the beach, private garden and parking.',
     now() + interval '14 days', now() + interval '21 days', 6, 'ACTIVE'),

    ('00000000-0000-4000-c000-000000000004',
     '00000000-0000-4000-b000-000000000001',
     'Ski resort room — Zakopane',
     'Slope-side studio with ski storage. Includes breakfast.',
     now() + interval '60 days', now() + interval '63 days', 2, 'ACTIVE'),

    ('00000000-0000-4000-c000-000000000005',
     '00000000-0000-4000-b000-000000000001',
     'Safari tent — Bieszczady',
     'Glamping tent in the Bieszczady wilderness. Includes dinner around the fire.',
     now() + interval '30 days', now() + interval '32 days', 2, 'ACTIVE'),

    ('00000000-0000-4000-c000-000000000006',
     '00000000-0000-4000-b000-000000000001',
     'City apartment — Warsaw Centre',
     'Modern studio near Warsaw Central station, fully equipped kitchen.',
     now() + interval '7 days', now() + interval '10 days', 2, 'ACTIVE'),

    ('00000000-0000-4000-c000-000000000007',
     '00000000-0000-4000-b000-000000000001',
     'Luxury villa — Mazury Lakes',
     'Private villa with boat dock and sauna. Still being configured.',
     now() + interval '90 days', now() + interval '97 days', 8, 'DRAFT'),

    ('00000000-0000-4000-c000-000000000008',
     '00000000-0000-4000-b000-000000000001',
     'Hostel bunk — Wrocław',
     'Withdrawn after venue closed.',
     now() + interval '1 day', now() + interval '2 days', 10, 'CLOSED');

-- ── auctions ─────────────────────────────────────────────────────────────────
-- 2 ENGLISH RUNNING
INSERT INTO auctions (id, listing_id, seller_id, type, status,
                      starts_at, ends_at,
                      start_price_amount, start_price_currency,
                      min_increment_amount, min_increment_currency,
                      reserve_price_amount, reserve_price_currency)
VALUES
    ('00000000-0000-4000-d000-000000000001',
     '00000000-0000-4000-c000-000000000001',
     '00000000-0000-4000-b000-000000000001',
     'ENGLISH', 'RUNNING',
     now() - interval '1 hour', now() + interval '2 days',
     50000, 'PLN', 5000, 'PLN', 40000, 'PLN'),

    ('00000000-0000-4000-d000-000000000002',
     '00000000-0000-4000-c000-000000000002',
     '00000000-0000-4000-b000-000000000001',
     'ENGLISH', 'RUNNING',
     now() - interval '30 minutes', now() + interval '3 days',
     30000, 'PLN', 2000, 'PLN', 25000, 'PLN');

-- 2 DUTCH RUNNING
INSERT INTO auctions (id, listing_id, seller_id, type, status,
                      starts_at, ends_at,
                      start_price_amount, start_price_currency,
                      decrement_amount, decrement_currency,
                      step_seconds,
                      floor_amount, floor_currency,
                      reserve_price_amount, reserve_price_currency)
VALUES
    ('00000000-0000-4000-d000-000000000003',
     '00000000-0000-4000-c000-000000000003',
     '00000000-0000-4000-b000-000000000001',
     'DUTCH', 'RUNNING',
     now() - interval '2 hours', now() + interval '1 day',
     120000, 'PLN', 5000, 'PLN', 3600, 70000, 'PLN', 65000, 'PLN'),

    ('00000000-0000-4000-d000-000000000004',
     '00000000-0000-4000-c000-000000000004',
     '00000000-0000-4000-b000-000000000001',
     'DUTCH', 'RUNNING',
     now() - interval '45 minutes', now() + interval '6 hours',
     80000, 'PLN', 2000, 'PLN', 1800, 50000, 'PLN', 48000, 'PLN');

-- 1 SCHEDULED (starts in ~5 minutes)
INSERT INTO auctions (id, listing_id, seller_id, type, status,
                      starts_at, ends_at,
                      start_price_amount, start_price_currency,
                      min_increment_amount, min_increment_currency,
                      reserve_price_amount, reserve_price_currency)
VALUES
    ('00000000-0000-4000-d000-000000000005',
     '00000000-0000-4000-c000-000000000005',
     '00000000-0000-4000-b000-000000000001',
     'ENGLISH', 'SCHEDULED',
     now() + interval '5 minutes', now() + interval '3 days',
     20000, 'PLN', 1000, 'PLN', 15000, 'PLN');
