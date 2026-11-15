package dev.dmzhideout.zone;

import com.mojang.brigadier.CommandDispatcher;
import dev.dmzhideout.DMZHideout;
import dev.dmzhideout.config.HideoutConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Read-only command for verifying ownership and radius in game. */
@Mod.EventBusSubscriber(modid = DMZHideout.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HideoutCommands {
    private HideoutCommands() { }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("hideout")
                .then(Commands.literal("status").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    SafeZoneSavedData data = SafeZoneSavedData.of(player.serverLevel());
                    SafeZoneSavedData.Zone home = data.activeCore(player);
                    SafeZoneSavedData.Zone here = data.at(player.serverLevel(),
                            player.blockPosition());
                    String message = home == null ? "登録済みの本拠地はありません" :
                            "本拠地: " + home.dimension() + " " + home.center().toShortString();
                    message += " / 現在地: " + (here == null ? "通常区域" :
                            (data.isOwner(player, here) ? "所属チームのセーフゾーン" :
                                    "他チームのセーフゾーン"));
                    message += " / 半径 " + HideoutConfig.horizontal() +
                            ", 上下 " + HideoutConfig.vertical();
                    String result = message;
                    ctx.getSource().sendSuccess(() -> Component.literal(result), false);
                    return 1;
                })));
    }
}
