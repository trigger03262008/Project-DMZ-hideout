package dev.dmzhideout.client;

/**
 * Minecraft-independent hit test for the left-hand Trading Center offer list.
 * The actual visible row is 21px high with a 2px gap, and 7 rows per page.
 */
public final class TradeOfferSelection {
    public static final int LEFT=9, RIGHT=152, TOP=48, ROW_HEIGHT=21;
    public static final int ROW_STRIDE=23, ROWS_PER_PAGE=7;
    private TradeOfferSelection() { }

    public static int offerIndex(double mouseX,double mouseY,
                                 int guiLeft,int guiTop,int page,int offerCount) {
        if(page<0||offerCount<=0)return -1;
        double x=mouseX-guiLeft, y=mouseY-guiTop;
        if(x<LEFT||x>=RIGHT||y<TOP||y>=TOP+ROWS_PER_PAGE*ROW_STRIDE)
            return -1;
        int row=(int)((y-TOP)/ROW_STRIDE);
        if(y-(TOP+row*ROW_STRIDE)>=ROW_HEIGHT)return -1;
        long index=(long)page*ROWS_PER_PAGE+row;
        return index<offerCount?(int)index:-1;
    }
}
