package pl.reachsky.backend.catalog.domain;

public enum ListingStatus {

    DRAFT {
        @Override public ListingStatus publish() { return ACTIVE; }
        @Override public ListingStatus renew()   { throw new IllegalListingTransition(this, "renew"); }
    },
    ACTIVE {
        @Override public ListingStatus publish() { throw new IllegalListingTransition(this, "publish"); }
        @Override public ListingStatus renew()   { throw new IllegalListingTransition(this, "renew"); }
    },
    CLOSED {
        @Override public ListingStatus publish() { throw new IllegalListingTransition(this, "publish"); }
        @Override public ListingStatus renew()   { return ACTIVE; }
    };

    public abstract ListingStatus publish();

    public abstract ListingStatus renew();
}
