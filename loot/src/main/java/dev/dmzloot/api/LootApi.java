package dev.dmzloot.api;

import dev.dmzloot.item.ItemCatalog;
import dev.dmzloot.item.LootItem;
import dev.dmzloot.salvage.SalvageRecipes;
import dev.dmzloot.salvage.SalvageService;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Safe optional bridge: Hideout invokes useWorkbench via reflection. */
public final class LootApi {
    private LootApi(){}
    public static double massKg(ItemStack stack){
        return stack.getItem() instanceof LootItem item?
                item.definition().massKg():-1.0;
    }
    public static int gridWidth(ItemStack stack){
        return stack.getItem() instanceof LootItem item?
                item.definition().width():1;
    }
    public static int gridHeight(ItemStack stack){
        return stack.getItem() instanceof LootItem item?
                item.definition().height():1;
    }
    public static int referencePrice(ItemStack stack){
        return stack.getItem() instanceof LootItem item?
                item.definition().baseCash():-1;
    }
    public static boolean salvageable(ItemStack stack){
        return SalvageRecipes.forStack(stack)!=null;
    }

    /**
     * Use the held item against Hideout's dedicated workbench.
     * Only accepts a real loaded workbench within 8 blocks, on the server.
     */
    public static boolean useWorkbench(ServerPlayer player,BlockPos pos){
        return SalvageService.useWorkbench(player,pos);
    }
}
