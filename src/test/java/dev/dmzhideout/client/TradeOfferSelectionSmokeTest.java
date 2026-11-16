package dev.dmzhideout.client;

/** Regression: trader stock must be selectable before vanilla slot handlers. */
public final class TradeOfferSelectionSmokeTest {
    public static void main(String[] args) {
        final int left=113,top=42;
        assert TradeOfferSelection.offerIndex(left+9,top+48,left,top,0,20)==0;
        assert TradeOfferSelection.offerIndex(left+151,top+68,left,top,0,20)==0;
        assert TradeOfferSelection.offerIndex(left+50,top+71,left,top,0,20)==1;
        assert TradeOfferSelection.offerIndex(left+50,top+71,left,top,1,20)==8;
        assert TradeOfferSelection.offerIndex(left+50,top+48+6*23,left,top,1,20)==13;
        assert TradeOfferSelection.offerIndex(left+50,top+48+6*23,left,top,2,15)==-1;
        assert TradeOfferSelection.offerIndex(left+50,top+48,left,top,0,0)==-1;
        assert TradeOfferSelection.offerIndex(left+50,top+48,left,top,-1,20)==-1;
        // Upper/lower ends and the actual 2px gap should not select items.
        assert TradeOfferSelection.offerIndex(left+8,top+48,left,top,0,20)==-1;
        assert TradeOfferSelection.offerIndex(left+152,top+48,left,top,0,20)==-1;
        assert TradeOfferSelection.offerIndex(left+50,top+47,left,top,0,20)==-1;
        assert TradeOfferSelection.offerIndex(left+50,top+69,left,top,0,20)==-1;
        assert TradeOfferSelection.offerIndex(left+50,top+70,left,top,0,20)==-1;
        // Paging button is below the offer rows and must be handled by parent.
        assert TradeOfferSelection.offerIndex(left+20,top+217,left,top,0,20)==-1;
        System.out.println("Trading Center offer selection tests: OK");
    }
}
