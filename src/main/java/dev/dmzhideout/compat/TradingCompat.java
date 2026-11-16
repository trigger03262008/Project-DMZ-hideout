package dev.dmzhideout.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import com.google.gson.JsonArray;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

/** Server passes confirmed intent; Traders validates and settles the trade. */
public final class TradingCompat {
    private TradingCompat() { }
    public static boolean traderExists(String traderId) {
        if(!ModList.get().isLoaded("dmztraders"))return false;
        try {
            return Boolean.TRUE.equals(Class.forName("dev.dmztraders.api.TradersApi")
                    .getMethod("traderExists",String.class).invoke(null,traderId));
        }catch(ReflectiveOperationException | RuntimeException exception){return false;}
    }

    public static String buybackOffer(String traderId,ItemStack item) {
        if(!ModList.get().isLoaded("dmztraders")||item==null||item.isEmpty())return "";
        try {
            return (String)Class.forName("dev.dmztraders.api.TradersApi")
                    .getMethod("findBuybackOffer",String.class,ItemStack.class)
                    .invoke(null,traderId,item);
        }catch(ReflectiveOperationException | RuntimeException exception){return "";}
    }

    public static long quoteBasket(ServerPlayer player,BlockPos center,
                                   String traderId,JsonArray basket) {
        return invokeBasket("quoteBasket",player,center,traderId,basket);
    }
    public static long sellBasket(ServerPlayer player,BlockPos center,
                                  String traderId,JsonArray basket) {
        return invokeBasket("sellBasket",player,center,traderId,basket);
    }
    private static long invokeBasket(String method,ServerPlayer player,
                                    BlockPos center,String traderId,JsonArray basket) {
        if(!ModList.get().isLoaded("dmztraders"))return -1;
        try {
            Object value=Class.forName("dev.dmztraders.api.TradersApi")
                    .getMethod(method,ServerPlayer.class,BlockPos.class,
                            String.class,JsonArray.class)
                    .invoke(null,player,center,traderId,basket);
            return ((Number)value).longValue();
        }catch(ReflectiveOperationException | RuntimeException exception){return -1;}
    }

    public static boolean trade(ServerPlayer player, BlockPos center,
                                String traderId, String offerId, boolean buy,
                                boolean payFromStash, boolean receiveToStash) {
        if (!ModList.get().isLoaded("dmztraders")) return false;
        try {
            return Boolean.TRUE.equals(
                    Class.forName("dev.dmztraders.api.TradersApi")
                            .getMethod("executeCenterTrade",ServerPlayer.class,BlockPos.class,
                                    String.class,String.class,boolean.class,
                                    boolean.class,boolean.class)
                            .invoke(null,player,center,traderId,offerId,buy,
                                    payFromStash,receiveToStash));
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }
}
