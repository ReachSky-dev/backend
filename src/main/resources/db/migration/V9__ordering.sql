CREATE TABLE orders (
    id                uuid        NOT NULL,
    auction_id        uuid        NOT NULL,
    buyer_id          uuid        NOT NULL,
    amount            bigint      NOT NULL,
    currency          varchar(3)  NOT NULL,
    status            varchar(30) NOT NULL,
    payment_deadline  timestamptz NOT NULL,
    created_at        timestamptz NOT NULL,

    CONSTRAINT orders_pk          PRIMARY KEY (id),
    CONSTRAINT orders_auction_uq  UNIQUE (auction_id)
);

CREATE INDEX orders_buyer_idx    ON orders (buyer_id);
CREATE INDEX orders_status_idx   ON orders (status, payment_deadline);
