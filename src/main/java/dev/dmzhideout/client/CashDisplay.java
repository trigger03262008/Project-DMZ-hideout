package dev.dmzhideout.client;

import java.lang.reflect.Method;
import java.util.Locale;
import net.minecraftforge.fml.ModList;

/**
 * Read only the existing P.D.A client-side synchronized cash cache.
 * We never create a second currency balance, and no extra packet is required.
 */
public final class CashDisplay {
    private static Method balanceMethod;
    private static boolean resolved;
    private CashDisplay() { }

    public static long balance() {
        if(!ModList.get().isLoaded("dmzpda"))return -1L;
        try {
            if(!resolved){
                balanceMethod=Class.forName("dev.dmzpda.client.cash.ClientCashState")
                        .getMethod("balance");
                resolved=true;
            }
            Object result=balanceMethod.invoke(null);
            return result instanceof Number number?number.longValue():-1L;
        }catch(ReflectiveOperationException | RuntimeException error){
            resolved=false;
            return -1L;
        }
    }

    public static String format() {
        long value=balance();
        if(value<0)return "SYNC...";
        return String.format(Locale.ROOT,"%,d",value);
    }
}
