package dev.dmzloot.item;

import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import javax.annotation.Nullable;

/** Every Loot item carries stable catalog metadata; tools keep 0-HP shells. */
public final class LootItem extends Item {
    private final ItemCatalog.Definition definition;
    public LootItem(ItemCatalog.Definition def){
        super(def.tool()?new Item.Properties().durability(def.durability()):
                new Item.Properties().stacksTo(def.stackSize()));
        definition=def;
    }
    public ItemCatalog.Definition definition(){return definition;}
    @Override
    public void appendHoverText(ItemStack stack,@Nullable Level level,
                                List<Component> lines,TooltipFlag flags){
        super.appendHoverText(stack,level,lines,flags);
        lines.add(Component.translatable("tooltip.dmzloot.mass",
                String.format(Locale.ROOT,"%.2f",definition.massKg()),
                definition.width(),definition.height()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.dmzloot.cash",
                definition.baseCash()).withStyle(ChatFormatting.DARK_GRAY));
        if(definition.tool()){
            int remain=Math.max(0,stack.getMaxDamage()-stack.getDamageValue());
            lines.add(Component.translatable("tooltip.dmzloot.tier",
                    definition.tier()).withStyle(ChatFormatting.AQUA));
            lines.add(Component.translatable("tooltip.dmzloot.condition",
                    remain,stack.getMaxDamage()).withStyle(remain==0?
                            ChatFormatting.RED:ChatFormatting.GREEN));
            if(remain==0)
                lines.add(Component.translatable("tooltip.dmzloot.broken")
                        .withStyle(ChatFormatting.RED));
        }
    }
}
