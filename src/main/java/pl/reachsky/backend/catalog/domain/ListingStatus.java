package pl.reachsky.backend.catalog.domain;

public enum ListingStatus {

    DRAFT {
        @Override public ListingStatus publish() { return ACTIVE; }
        @Override public ListingStatus renew()   { throw new IllegalListingTransition(this, "renew"); }
        @Override public ListingStatus close()   { throw new IllegalListingTransition(this, "close"); }
        @Override public ListingStatus sell()    { throw new IllegalListingTransition(this, "sell"); }
        @Override public ListingStatus expire()  { throw new IllegalListingTransition(this, "expire"); }
    },
    ACTIVE {
        @Override public ListingStatus publish() { throw new IllegalListingTransition(this, "publish"); }
        @Override public ListingStatus renew()   { throw new IllegalListingTransition(this, "renew"); }
        @Override public ListingStatus close()   { return CLOSED; }
        @Override public ListingStatus sell()    { return SOLD; }
        @Override public ListingStatus expire()  { return EXPIRED; }
    },
    CLOSED {
        @Override public ListingStatus publish() { throw new IllegalListingTransition(this, "publish"); }
        @Override public ListingStatus renew()   { return ACTIVE; }
        @Override public ListingStatus close()   { throw new IllegalListingTransition(this, "close"); }
        @Override public ListingStatus sell()    { throw new IllegalListingTransition(this, "sell"); }
        @Override public ListingStatus expire()  { throw new IllegalListingTransition(this, "expire"); }
    },
    SOLD {
        @Override public ListingStatus publish() { throw new IllegalListingTransition(this, "publish"); }
        @Override public ListingStatus renew()   { throw new IllegalListingTransition(this, "renew"); }
        @Override public ListingStatus close()   { throw new IllegalListingTransition(this, "close"); }
        @Override public ListingStatus sell()    { throw new IllegalListingTransition(this, "sell"); }
        @Override public ListingStatus expire()  { throw new IllegalListingTransition(this, "expire"); }
    },
    EXPIRED {
        @Override public ListingStatus publish() { throw new IllegalListingTransition(this, "publish"); }
        @Override public ListingStatus renew()   { throw new IllegalListingTransition(this, "renew"); }
        @Override public ListingStatus close()   { throw new IllegalListingTransition(this, "close"); }
        @Override public ListingStatus sell()    { throw new IllegalListingTransition(this, "sell"); }
        @Override public ListingStatus expire()  { throw new IllegalListingTransition(this, "expire"); }
    };

    public abstract ListingStatus publish();

    public abstract ListingStatus renew();

    public abstract ListingStatus close();

    public abstract ListingStatus sell();

    public abstract ListingStatus expire();
}
