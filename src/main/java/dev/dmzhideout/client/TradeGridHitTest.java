package dev.dmzhideout.client;

/** Pure click mapping: a noninteractive background cannot steal basket input. */
public final class TradeGridHitTest {
    private TradeGridHitTest() { }
    public static int slot(double x,double y,int left,int top,
                           int columns,int rows,int cell) {
        double dx=x-left,dy=y-top;
        if(dx<0||dy<0||dx>=columns*cell||dy>=rows*cell)return -1;
        return (int)(dy/cell)*columns+(int)(dx/cell);
    }
}
