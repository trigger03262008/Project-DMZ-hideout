package dev.dmzhideout.compat;

import java.lang.reflect.Method;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

/** Optional Health and PDA APIs are reflected to avoid mandatory dependencies. */
public final class MedicalCompat {
    private MedicalCompat() { }

    private static Object call(String className, String method, ServerPlayer player)
            throws ReflectiveOperationException {
        Class<?> api = Class.forName(className);
        return api.getMethod(method, ServerPlayer.class).invoke(null, player);
    }
    public static int quote(ServerPlayer player) {
        if (!ModList.get().isLoaded("dmzcore") || !ModList.get().isLoaded("dmzpda"))
            return -1;
        try {
            return (int) call("dev.dmzcore.api.HealthIntegrationApi",
                    "quoteFullTreatment",player);
        } catch (ReflectiveOperationException | ClassCastException e) {
            return -1;
        }
    }
    public static boolean heal(ServerPlayer player) {
        int quote = quote(player);
        if (quote <= 0) return false;
        try {
            Class<?> wallet = Class.forName("dev.dmzpda.cash.CashApi");
            Method spend = wallet.getMethod("trySpend", ServerPlayer.class,long.class);
            Method credit = wallet.getMethod("credit",ServerPlayer.class,long.class);
            if (!Boolean.TRUE.equals(spend.invoke(null,player,(long)quote)))
                return false;
            try {
                if (Boolean.TRUE.equals(call(
                        "dev.dmzcore.api.HealthIntegrationApi","healFully",player)))
                    return true;
            } catch (ReflectiveOperationException | RuntimeException e) {
                credit.invoke(null,player,(long)quote);
                throw e;
            }
            credit.invoke(null,player,(long)quote);
        } catch (ReflectiveOperationException | RuntimeException e) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "治療処理に失敗しました。サーバーログを確認してください。"),false);
        }
        return false;
    }
}
