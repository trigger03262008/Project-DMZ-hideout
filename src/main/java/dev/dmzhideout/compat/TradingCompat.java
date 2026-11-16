package dev.dmzhideout.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

/** Server passes confirmed intent; Traders validates and settles the trade. */
public final class TradingCompat {
    private TradingCompat() { }
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
