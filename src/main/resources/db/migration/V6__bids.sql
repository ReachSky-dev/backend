-- Bid state on auctions (updated by each accepted bid)
ALTER TABLE auctions
    ADD COLUMN current_price_amount   bigint      NOT NULL DEFAULT 0,
    ADD COLUMN current_price_currency varchar(3)  NOT NULL DEFAULT 'PLN',
    ADD COLUMN highest_bidder_id      uuid,
    ADD COLUMN bid_count              integer     NOT NULL DEFAULT 0,
    ADD COLUMN extensions_used        integer     NOT NULL DEFAULT 0;

-- Seed the initial current_price from start_price so existing rows are consistent
UPDATE auctions SET current_price_amount = start_price_amount,
                    current_price_currency = start_price_currency;

-- Remove bootstrap defaults (columns now populated)
ALTER TABLE auctions
    ALTER COLUMN current_price_amount DROP DEFAULT,
    ALTER COLUMN current_price_currency DROP DEFAULT;

-- Accepted bids (append-only)
CREATE TABLE bids (
    id               uuid        NOT NULL,
    auction_id       uuid        NOT NULL,
    bidder_id        uuid        NOT NULL,
    amount           bigint      NOT NULL,
    currency         varchar(3)  NOT NULL,
    sequence         bigint      NOT NULL,
    placed_at        timestamptz NOT NULL,
    idempotency_key  varchar(255) NOT NULL,

    CONSTRAINT bids_pk              PRIMARY KEY (id),
    CONSTRAINT bids_auction_fk      FOREIGN KEY (auction_id) REFERENCES auctions (id),
    CONSTRAINT bids_seq_unique      UNIQUE (auction_id, sequence),
    CONSTRAINT bids_idem_unique     UNIQUE (idempotency_key)
);

CREATE INDEX bids_auction_seq_idx ON bids (auction_id, sequence DESC);

-- Proxy (automatic) bids — one active proxy per (auction, bidder)
CREATE TABLE proxy_bids (
    id          uuid        NOT NULL,
    auction_id  uuid        NOT NULL,
    bidder_id   uuid        NOT NULL,
    max_amount  bigint      NOT NULL,
    currency    varchar(3)  NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT proxy_bids_pk          PRIMARY KEY (id),
    CONSTRAINT proxy_bids_auction_fk  FOREIGN KEY (auction_id) REFERENCES auctions (id),
    CONSTRAINT proxy_bids_pair_unique UNIQUE (auction_id, bidder_id)
);
