package dev.dmzhideout.menu;

/** Pure helpers for row-scrolling large storage with a fixed network window. */
public final class ScrollMath {
    private ScrollMath() { }
    public static int maxScroll(int count,int columns,int rows) {
        if(count<0||columns<1||rows<1)throw new IllegalArgumentException();
        return Math.max(0,(count+columns-1)/columns-rows);
    }
    public static int clamp(int row,int max) {
        return Math.max(0,Math.min(Math.max(0,max),row));
    }
    public static int mappedIndex(int firstRow,int visible,int columns,int count) {
        long index=(long)firstRow*columns+visible;
        return firstRow<0||visible<0||index<0||index>=count?-1:(int)index;
    }
}
