package dev.dmzhideout.client;

public final class TradeGridHitTestSmokeTest {
    public static void main(String[] args) {
        assert TradeGridHitTest.slot(303,109,303,109,12,6,18)==0;
        assert TradeGridHitTest.slot(518,216,303,109,12,6,18)==71;
        assert TradeGridHitTest.slot(519,109,303,109,12,6,18)==-1;
        assert TradeGridHitTest.slot(22,110,22,110,8,3,18)==0;
        assert TradeGridHitTest.slot(165,163,22,110,8,3,18)==23;
        assert TradeGridHitTest.slot(165,164,22,110,8,3,18)==-1;
        assert TradeGridHitTest.slot(200,150,22,110,8,3,18)==-1;
        System.out.println("Trading Center grids: OK");
    }
}
