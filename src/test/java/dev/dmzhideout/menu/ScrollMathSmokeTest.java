package dev.dmzhideout.menu;

public final class ScrollMathSmokeTest {
    public static void main(String[] args) {
        assert ScrollMath.maxScroll(270,9,6)==24;
        assert ScrollMath.maxScroll(1350,9,6)==144;
        assert ScrollMath.maxScroll(0,9,6)==0;
        assert ScrollMath.maxScroll(54,9,6)==0;
        assert ScrollMath.maxScroll(55,9,6)==1;
        assert ScrollMath.clamp(-5,144)==0;
        assert ScrollMath.clamp(200,144)==144;
        assert ScrollMath.mappedIndex(10,0,9,270)==90;
        assert ScrollMath.mappedIndex(24,53,9,270)==269;
        assert ScrollMath.mappedIndex(24,54,9,270)==-1;
        assert ScrollMath.maxScroll(73,12,6)==1;
        System.out.println("Storage virtual scroll math: OK");
    }
}
