package dev.dmzloot.item;

import dev.dmzloot.DMZLoot;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** All 32 items registered from one immutable packaged catalog. */
public final class LootItems {
    private static final DeferredRegister<Item> REGISTER=
            DeferredRegister.create(ForgeRegistries.ITEMS,DMZLoot.ID);
    private static final Map<String,RegistryObject<Item>> REFERENCES=new LinkedHashMap<>();
    static {
        for(ItemCatalog.Definition def:ItemCatalog.all())
            REFERENCES.put(def.id(),REGISTER.register(def.id(),()->new LootItem(def)));
    }
    private LootItems(){}
    public static void register(IEventBus bus){
        REGISTER.register(bus);
        bus.addListener(LootItems::creative);
    }
    public static Item item(String id){
        RegistryObject<Item> entry=REFERENCES.get(id);
        return entry==null?null:entry.get();
    }
    private static void creative(BuildCreativeModeTabContentsEvent event){
        boolean tools=event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES);
        if(!tools&&!event.getTabKey().equals(CreativeModeTabs.INGREDIENTS))return;
        for(ItemCatalog.Definition def:ItemCatalog.all())
            if(def.tool()==tools)event.accept(REFERENCES.get(def.id()).get());
    }
}
