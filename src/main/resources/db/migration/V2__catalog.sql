CREATE TABLE listings (
    id               uuid         NOT NULL,
    seller_id        uuid         NOT NULL,
    title            varchar(255) NOT NULL,
    description      text,
    window_starts_at timestamptz  NOT NULL,
    window_ends_at   timestamptz  NOT NULL,
    capacity         integer      NOT NULL,
    status           varchar(20)  NOT NULL,
    created_at       timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT listings_pk PRIMARY KEY (id)
);

CREATE INDEX listings_status_created_at_idx ON listings (status, created_at);
