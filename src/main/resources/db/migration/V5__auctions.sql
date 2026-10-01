CREATE TABLE auctions (
    id                       uuid         NOT NULL,
    listing_id               uuid         NOT NULL,
    seller_id                uuid         NOT NULL,
    type                     varchar(20)  NOT NULL,
    status                   varchar(20)  NOT NULL,
    starts_at                timestamptz  NOT NULL,
    ends_at                  timestamptz  NOT NULL,

    -- PricingPolicy common
    start_price_amount       bigint       NOT NULL,
    start_price_currency     varchar(3)   NOT NULL,

    -- EnglishPricing (null for DUTCH)
    min_increment_amount     bigint,
    min_increment_currency   varchar(3),

    -- DutchPricing (null for ENGLISH)
    decrement_amount         bigint,
    decrement_currency       varchar(3),
    step_seconds             bigint,
    floor_amount             bigint,
    floor_currency           varchar(3),

    -- Hidden reserve price — never returned in API responses
    reserve_price_amount     bigint       NOT NULL,
    reserve_price_currency   varchar(3)   NOT NULL,

    -- AntiSnipingPolicy (null if not configured)
    anti_sniping_window_s    bigint,
    anti_sniping_ext_s       bigint,
    anti_sniping_max_ext     integer,

    created_at               timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT auctions_pk         PRIMARY KEY (id),
    CONSTRAINT auctions_listing_fk FOREIGN KEY (listing_id) REFERENCES listings (id)
);

-- Scheduler queries: find SCHEDULED auctions due to start, find RUNNING auctions due to end
CREATE INDEX auctions_status_starts_at_idx ON auctions (status, starts_at);
CREATE INDEX auctions_status_ends_at_idx   ON auctions (status, ends_at);
