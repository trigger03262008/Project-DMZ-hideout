package dev.dmzhideout.network;

import dev.dmzhideout.DMZHideout;
import dev.dmzhideout.block.StashBlockEntity;
import dev.dmzhideout.compat.MedicalCompat;
import dev.dmzhideout.compat.TradingCompat;
import dev.dmzhideout.menu.StashMenu;
import dev.dmzhideout.menu.MedMenu;
import dev.dmzhideout.menu.TradeMenu;
import java.util.Optional;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.PacketDistributor;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** Client sends intentions; server verifies open menu, proximity, block and owner. */
public final class HideoutNetwork {
    private static final String VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(DMZHideout.MOD_ID, "facilities"),
            () -> VERSION, VERSION::equals, VERSION::equals);
    public enum Action {
        STASH_QUERY, STASH_NEXT, STASH_PREV, STASH_SORT, STASH_FILTER, STASH_LOCK,
        STASH_TOGGLE, STASH_UPGRADE,
        MED_HEAL, MED_CRAFT, TRADE_QUERY, TRADE_NEXT, TRADE_PREV, TRADE_BUY, TRADE_SELL
    }

    private HideoutNetwork() { }

    public static void init() {
        CHANNEL.registerMessage(0, Request.class, Request::encode, Request::decode,
                Request::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(1, RecipeCatalog.class,
                RecipeCatalog::encode,RecipeCatalog::decode,RecipeCatalog::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void request(Action action, BlockPos pos) {
        request(action, pos, "", "", false, false);
    }
    public static void request(Action action, BlockPos pos, String a) {
        request(action, pos, a, "", false, false);
    }
    public static void request(Action action, BlockPos pos, String a, String b,
                               boolean inputStash, boolean outputStash) {
        if (action == null || pos == null || a == null || b == null ||
                a.length() > 128 || b.length() > 128) return;
        CHANNEL.sendToServer(new Request(action, pos, a, b, inputStash, outputStash));
    }

    public static void sendRecipes(ServerPlayer player) {
        String json=dev.dmzhideout.med.MedRecipes.snapshot().toString();
        if(json.length()<=20000)
            CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new RecipeCatalog(json));
    }

    private record RecipeCatalog(String json) {
        static void encode(RecipeCatalog packet,FriendlyByteBuf buf) {
            buf.writeUtf(packet.json,20000);
        }
        static RecipeCatalog decode(FriendlyByteBuf buf) {
            return new RecipeCatalog(buf.readUtf(20000));
        }
        static void handle(RecipeCatalog packet,Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context ctx=supplier.get();
            ctx.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    ()->()->dev.dmzhideout.med.MedClientRecipes.update(
                            JsonParser.parseString(packet.json).getAsJsonArray())));
            ctx.setPacketHandled(true);
        }
    }

    private record Request(Action action, BlockPos pos, String a, String b,
                           boolean inputStash, boolean outputStash) {
        static void encode(Request r, FriendlyByteBuf buf) {
            buf.writeEnum(r.action);
            buf.writeBlockPos(r.pos);
            buf.writeUtf(r.a,128);
            buf.writeUtf(r.b,128);
            buf.writeBoolean(r.inputStash);
            buf.writeBoolean(r.outputStash);
        }
        static Request decode(FriendlyByteBuf buf) {
            return new Request(buf.readEnum(Action.class),buf.readBlockPos(),
                    buf.readUtf(128),buf.readUtf(128),buf.readBoolean(),buf.readBoolean());
        }
        static void handle(Request req, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player=context.getSender();
                if (player==null || !player.serverLevel().hasChunkAt(req.pos)) return;
                if (player.containerMenu instanceof StashMenu menu &&
                        menu.pos().equals(req.pos) && menu.stillValid(player)) {
                    StashBlockEntity stash = player.level().getBlockEntity(req.pos)
                            instanceof StashBlockEntity s ? s : null;
                    if (stash == null || !stash.canAccess(player)) return;
                    switch (req.action) {
                        case STASH_QUERY -> menu.search(req.a);
                        case STASH_NEXT -> menu.navigate(1);
                        case STASH_PREV -> menu.navigate(-1);
                        case STASH_SORT -> menu.sort();
                        case STASH_FILTER -> menu.filter();
                        case STASH_LOCK -> {
                            try {
                                int n=Integer.parseInt(req.a);
                                menu.toggleLock(player,n);
                            }catch(NumberFormatException ignored) { }
                        }
                        case STASH_TOGGLE -> {
                            if (!stash.toggleShared(player))
                                player.displayClientMessage(Component.literal(
                                        "共有にはスタッシュ所有者とチーム所属が必要です"),true);
                            menu.updated();
                        }
                        case STASH_UPGRADE -> {
                            if (!stash.upgrade(player))
                                player.displayClientMessage(Component.literal(
                                        "アップグレードできません（鉄インゴット不足または権限なし）"),true);
                            menu.updated();
                        }
                        default -> { }
                    }
                } else if (player.containerMenu instanceof MedMenu menu &&
                        menu.pos().equals(req.pos) && menu.stillValid(player)) {
                    if (req.action == Action.MED_HEAL) {
                        boolean success=MedicalCompat.heal(player);
                        player.displayClientMessage(Component.literal(success ?
                                "メッドステーション：治療完了" :
                                "治療できません：症状・CASH・連携MODを確認してください"),true);
                        menu.broadcastChanges();
                    } else if (req.action == Action.MED_CRAFT) {
                        dev.dmzhideout.med.MedRecipes.craft(player,req.pos,req.a,
                                req.inputStash,req.outputStash);
                    }
                } else if (player.containerMenu instanceof TradeMenu menu &&
                        menu.pos().equals(req.pos) && menu.stillValid(player)) {
                    switch (req.action) {
                        case TRADE_QUERY -> menu.search(req.a);
                        case TRADE_NEXT -> menu.navigate(1);
                        case TRADE_PREV -> menu.navigate(-1);
                        case TRADE_BUY, TRADE_SELL -> {
                            if (!TradingCompat.trade(player, req.pos, req.a, req.b,
                                    req.action==Action.TRADE_BUY,
                                    req.inputStash, req.outputStash))
                                player.displayClientMessage(Component.literal(
                                        "取引できません：CASH・物資・収納先を確認してください"),true);
                            menu.refresh();
                        }
                        default -> { }
                    }
                }
            });
            context.setPacketHandled(true);
        }
    }
}
