package dev.dmzhideout.client;

import dev.dmzhideout.menu.StashMenu;
import dev.dmzhideout.network.HideoutNetwork;
import dev.dmzhideout.network.HideoutNetwork.Action;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class StashScreen extends AbstractContainerScreen<StashMenu> {
    private EditBox search;
    public StashScreen(StashMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth=176;
        imageHeight=270;
        inventoryLabelY=145;
    }
    @Override protected void init() {
        super.init();
        search=new EditBox(font,leftPos+9,topPos+19,158,16,Component.literal("検索"));
        search.setMaxLength(48);
        search.setHint(Component.literal("名称・MOD IDで検索"));
        search.setResponder(s -> HideoutNetwork.request(Action.STASH_QUERY,menu.pos(),s));
        addRenderableWidget(search);
        addRenderableWidget(Button.builder(Component.literal("<"),
                b -> HideoutNetwork.request(Action.STASH_PREV,menu.pos()))
                .bounds(leftPos+7,topPos+238,27,20).build());
        addRenderableWidget(Button.builder(Component.literal(">"),
                b -> HideoutNetwork.request(Action.STASH_NEXT,menu.pos()))
                .bounds(leftPos+36,topPos+238,27,20).build());
        addRenderableWidget(Button.builder(Component.literal("並替"),
                b -> HideoutNetwork.request(Action.STASH_SORT,menu.pos()))
                .bounds(leftPos+66,topPos+238,39,20).build());
        addRenderableWidget(Button.builder(Component.literal("共有"),
                b -> HideoutNetwork.request(Action.STASH_TOGGLE,menu.pos()))
                .bounds(leftPos+108,topPos+238,28,20).build());
        addRenderableWidget(Button.builder(Component.literal("強化"),
                b -> HideoutNetwork.request(Action.STASH_UPGRADE,menu.pos()))
                .bounds(leftPos+139,topPos+238,31,20).build());
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my) {
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xFF15211E);
        g.fill(leftPos+4,topPos+4,leftPos+imageWidth-4,topPos+imageHeight-4,0xFF25332E);
        for(int i=0;i<menu.slots.size();i++) {
            var slot=menu.slots.get(i);
            int x=leftPos+slot.x-1,y=topPos+slot.y-1;
            g.fill(x,y,x+18,y+18,i<StashMenu.VISIBLE?0xFF3B5248:0xFF3A4540);
            g.fill(x+1,y+1,x+17,y+17,0xFF1B2924);
        }
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my) {
        g.drawString(font,"STASH / Lv."+menu.tier()+" / "+
                (menu.shared()?"TEAM":"PRIVATE"),8,5,0xFF95E5BC,false);
        g.drawString(font,(menu.page()+1)+"/"+menu.pages(),10,262,0xFFE0EAE5,false);
        g.drawString(font,"INVENTORY",9,145,0xFF95E5BC,false);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float delta) {
        renderBackground(g);
        super.render(g,mx,my,delta);
        renderTooltip(g,mx,my);
    }
}
