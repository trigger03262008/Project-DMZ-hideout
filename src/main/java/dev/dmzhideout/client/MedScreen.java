package dev.dmzhideout.client;

import dev.dmzhideout.menu.MedMenu;
import dev.dmzhideout.network.HideoutNetwork;
import dev.dmzhideout.network.HideoutNetwork.Action;
import dev.dmzhideout.med.MedClientRecipes;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.fml.ModList;

public final class MedScreen extends AbstractContainerScreen<MedMenu> {
    private boolean useStash;
    private int recipeIndex;
    private Button sourceButton;

    public MedScreen(MedMenu menu, Inventory inv, Component title) {
        super(menu,inv,title);
        imageWidth=258;
        imageHeight=190;
    }
    @Override protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("全身治療 / HEAL"),
                b -> HideoutNetwork.request(Action.MED_HEAL,menu.pos()))
                .bounds(leftPos+13,topPos+62,232,22).build());
        addRenderableWidget(Button.builder(Component.literal("<"),
                b -> recipeIndex=Math.max(0,recipeIndex-1))
                .bounds(leftPos+13,topPos+135,28,20).build());
        addRenderableWidget(Button.builder(Component.literal(">"),
                b -> recipeIndex=Math.min(Math.max(0,MedClientRecipes.list().size()-1),
                        recipeIndex+1))
                .bounds(leftPos+44,topPos+135,28,20).build());
        sourceButton=addRenderableWidget(Button.builder(Component.literal("素材：所持品"),
                b -> { useStash=!useStash;
                    sourceButton.setMessage(Component.literal(useStash?
                            "素材：スタッシュ":"素材：所持品")); })
                .bounds(leftPos+78,topPos+135,112,20).build());
        addRenderableWidget(Button.builder(Component.literal("製造"),
                b -> {
                    List<com.google.gson.JsonObject> recipes=MedClientRecipes.list();
                    if (recipeIndex < recipes.size()) {
                        String id=recipes.get(recipeIndex).get("id").getAsString();
                        HideoutNetwork.request(Action.MED_CRAFT,menu.pos(),id,"",useStash,useStash);
                    }
                }).bounds(leftPos+195,topPos+135,50,20).build());
    }
    @Override protected void renderBg(GuiGraphics g,float delta,int mx,int my) {
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xFF1B2A2B);
        g.fill(leftPos+7,topPos+9,leftPos+imageWidth-7,topPos+imageHeight-9,0xFF253C3E);
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my) {
        g.drawString(font,"MED STATION",13,15,0xFF8CE2CF,false);
        boolean ready=ModList.get().isLoaded("dmzcore")&&ModList.get().isLoaded("dmzpda");
        String text=ready?(menu.quote()<=0?"全身状態：治療不要":
                "全身治療の料金： "+menu.quote()+" CASH"):"Health + P.D.Aが必要";
        g.drawString(font,text,13,43,0xFFFFD38B,false);
        g.drawString(font,"MEDICAL CRAFTING / 医療品製造",13,98,0xFF8CE2CF,false);
        var recipes=MedClientRecipes.list();
        String name=recipes.isEmpty()?"有効な製造レシピがありません" :
                recipes.get(Math.min(recipeIndex,recipes.size()-1))
                    .get("label").getAsString();
        g.drawString(font,font.plainSubstrByWidth(name,230),13,114,0xFFE4ECE7,false);
        g.drawString(font,"必要素材はレシピJSONで管理 / Loot拡張可",13,168,0xFF9DBCB6,false);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float delta) {
        renderBackground(g);
        super.render(g,mx,my,delta);
        renderTooltip(g,mx,my);
    }
}
