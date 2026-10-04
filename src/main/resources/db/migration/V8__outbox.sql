CREATE TABLE outbox_events (
    id               uuid         NOT NULL,
    aggregate_type   varchar(100) NOT NULL,
    aggregate_id     varchar(255) NOT NULL,
    event_type       varchar(100) NOT NULL,
    payload          text         NOT NULL,
    occurred_at      timestamptz  NOT NULL,
    published_at     timestamptz,
    attempts         integer      NOT NULL DEFAULT 0,

    CONSTRAINT outbox_events_pk PRIMARY KEY (id)
);

-- Partial index for efficient polling of unpublished events
CREATE INDEX outbox_unpublished_idx ON outbox_events (occurred_at)
    WHERE published_at IS NULL;
