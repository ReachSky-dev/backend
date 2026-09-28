-- Konwencje schematu obowiązujące w całej bazie:
--   * Identyfikatory: UUID v7 generowane w aplikacji, nie przez bazę (uuid_generate_v7 niedostępne w Postgresie 17 bez rozszerzenia — generujemy w Javie)
--   * Czas: timestamptz w UTC zawsze; nigdy timestamp bez strefy
--   * Kwoty pieniężne: bigint (grosze) + char(3) waluta (np. 'PLN'); nigdy float/double/numeric
--   * Nazwy tabel: liczba mnoga, snake_case (auctions, bids, wallet_entries)
--   * Nazwy kolumn: snake_case
--   * Każda tabela ma kolumnę created_at timestamptz NOT NULL DEFAULT now()

CREATE TABLE schema_conventions (
    id         uuid        NOT NULL,
    note       text,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT schema_conventions_pk PRIMARY KEY (id)
);
