-- Winner is set when auction transitions to SOLD; null otherwise
ALTER TABLE auctions
    ADD COLUMN winner_id uuid;
