package pl.reachsky.backend.auction.domain;

public enum AuctionStatus {

    DRAFT {
        @Override public AuctionStatus schedule() { return SCHEDULED; }
        @Override public AuctionStatus start()    { throw new IllegalAuctionTransition(this, "start"); }
        @Override public AuctionStatus cancel()   { return CANCELLED; }
    },
    SCHEDULED {
        @Override public AuctionStatus schedule() { throw new IllegalAuctionTransition(this, "schedule"); }
        @Override public AuctionStatus start()    { return RUNNING; }
        @Override public AuctionStatus cancel()   { return CANCELLED; }
    },
    RUNNING {
        @Override public AuctionStatus schedule() { throw new IllegalAuctionTransition(this, "schedule"); }
        @Override public AuctionStatus start()    { throw new IllegalAuctionTransition(this, "start"); }
        @Override public AuctionStatus cancel()   { return CANCELLED; }
    },
    SOLD {
        @Override public AuctionStatus schedule() { throw new IllegalAuctionTransition(this, "schedule"); }
        @Override public AuctionStatus start()    { throw new IllegalAuctionTransition(this, "start"); }
        @Override public AuctionStatus cancel()   { throw new IllegalAuctionTransition(this, "cancel"); }
    },
    RESERVE_NOT_MET {
        @Override public AuctionStatus schedule() { throw new IllegalAuctionTransition(this, "schedule"); }
        @Override public AuctionStatus start()    { throw new IllegalAuctionTransition(this, "start"); }
        @Override public AuctionStatus cancel()   { throw new IllegalAuctionTransition(this, "cancel"); }
    },
    CANCELLED {
        @Override public AuctionStatus schedule() { throw new IllegalAuctionTransition(this, "schedule"); }
        @Override public AuctionStatus start()    { throw new IllegalAuctionTransition(this, "start"); }
        @Override public AuctionStatus cancel()   { throw new IllegalAuctionTransition(this, "cancel"); }
    },
    SETTLED {
        @Override public AuctionStatus schedule() { throw new IllegalAuctionTransition(this, "schedule"); }
        @Override public AuctionStatus start()    { throw new IllegalAuctionTransition(this, "start"); }
        @Override public AuctionStatus cancel()   { throw new IllegalAuctionTransition(this, "cancel"); }
    };

    public abstract AuctionStatus schedule();
    public abstract AuctionStatus start();
    public abstract AuctionStatus cancel();
}
