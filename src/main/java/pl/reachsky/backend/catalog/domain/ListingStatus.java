package pl.reachsky.backend.catalog.domain;

public enum ListingStatus {

    DRAFT {
        @Override
        public ListingStatus publish() {
            return ACTIVE;
        }
    },
    ACTIVE {
        @Override
        public ListingStatus publish() {
            throw new IllegalListingTransition(this, "publish");
        }
    },
    CLOSED {
        @Override
        public ListingStatus publish() {
            throw new IllegalListingTransition(this, "publish");
        }
    };

    public abstract ListingStatus publish();
}
