package dev.dmzhideout.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.dmzhideout.med.MedClientRecipes;
import dev.dmzhideout.med.MedClientStock;
import dev.dmzhideout.menu.MedMenu;
import dev.dmzhideout.network.HideoutNetwork;
import dev.dmzhideout.network.HideoutNetwork.Action;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Production-first Med Station: searchable crafting list + dedicated heal tab.
 * Crafting is immediate; the server remains authoritative over all resources.
 */
public final class MedScreen extends AbstractContainerScreen<MedMenu> {
    private static final int GREEN=0xFF92D6B9, WHITE=0xFFE5F0E8,
            GRAY=0xFF9EB0A9, RED=0xFFFF7575, GOLD=0xFFFFCE7B;
    private static final String[] FILTERS={"ALL","BLEED","HEAL","PAIN","BLOOD","OTHER"};
    private static final int ROWS=5;
    private boolean healing,stashOutput=true;
    private int filter,offset;
    private EditBox search;
    private Button craftTab,healTab,outputButton,filterButton,healButton;
    private final List<Button> craftButtons=new ArrayList<>();
    private String query="";

    public MedScreen(MedMenu menu,Inventory inv,Component title) {
        super(menu,inv,title);
        imageWidth=520;
        imageHeight=285;
        inventoryLabelY=270;
    }

    @Override protected void init() {
        super.init();
        craftTab=addRenderableWidget(Button.builder(Component.literal("CRAFT / 製造"),
                b->healing=false).bounds(leftPos+13,topPos+16,118,23).build());
        healTab=addRenderableWidget(Button.builder(Component.literal("HEAL / 治療"),
                b->healing=true).bounds(leftPos+135,topPos+16,118,23).build());

        search=new EditBox(font,leftPos+18,topPos+48,234,18,Component.literal("検索"));
        search.setMaxLength(64);
        search.setHint(Component.literal("enter item name / アイテムを検索"));
        search.setResponder(s->{query=s.toLowerCase(Locale.ROOT);offset=0;});
        addRenderableWidget(search);
        filterButton=addRenderableWidget(Button.builder(Component.literal(FILTERS[0]),
                b->{filter=(filter+1)%FILTERS.length;offset=0;})
                .bounds(leftPos+262,topPos+47,79,20).build());

        for(int i=0;i<ROWS;i++){
            final int index=i;
            craftButtons.add(addRenderableWidget(Button.builder(Component.literal("START"),
                    b->craftRecipe(index)).bounds(leftPos+419,topPos+77+i*30,83,21).build()));
        }
        outputButton=addRenderableWidget(Button.builder(
                Component.literal("完成品：STASH優先"),
                b->{stashOutput=!stashOutput;})
                .bounds(leftPos+278,topPos+246,222,22).build());
        healButton=addRenderableWidget(Button.builder(Component.literal("APPLY / 全身治療"),
                b->HideoutNetwork.request(Action.MED_HEAL,menu.pos()))
                .bounds(leftPos+277,topPos+219,222,27).build());
    }

    private String field(JsonObject o,String key,String fallback) {
        return o.has(key)?o.get(key).getAsString():fallback;
    }
    private List<JsonObject> recipes() {
        List<JsonObject> result=new ArrayList<>();
        for(JsonObject recipe:MedClientRecipes.list()){
            String name=field(recipe,"label","").toLowerCase(Locale.ROOT);
            String item=field(recipe,"output","").toLowerCase(Locale.ROOT);
            String category=field(recipe,"category","other").toUpperCase(Locale.ROOT);
            if(!query.isEmpty() && !name.contains(query) && !item.contains(query))continue;
            if(filter>0 && !FILTERS[filter].equals(category))continue;
            result.add(recipe);
        }
        return result;
    }

    private void craftRecipe(int row) {
        List<JsonObject> recipes=recipes();
        int index=offset+row;
        if(index>=recipes.size())return;
        JsonObject recipe=recipes.get(index);
        if(!canCraft(recipe))return;
        HideoutNetwork.request(Action.MED_CRAFT,menu.pos(),
                field(recipe,"id",""),"",true,stashOutput);
    }

    private int inPlayer(String id) {
        ResourceLocation key=ResourceLocation.tryParse(id);
        if(key==null)return 0;
        Item wanted=ForgeRegistries.ITEMS.getValue(key);
        if(wanted==null||wanted==Items.AIR)return 0;
        int count=0;
        if(minecraft!=null && minecraft.player!=null)
            for(ItemStack stack:minecraft.player.getInventory().items)
                if(stack.is(wanted))count+=stack.getCount();
        return count;
    }
    private int count(String id) {
        return inPlayer(id)+MedClientStock.available(id);
    }
    private boolean canCraft(JsonObject recipe) {
        if(!recipe.has("ingredients"))return false;
        for(JsonElement el:recipe.getAsJsonArray("ingredients")){
            JsonObject ing=el.getAsJsonObject();
            if(count(ing.get("item").getAsString())<ing.get("count").getAsInt())
                return false;
        }
        return true;
    }

    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xFF101C1B);
        g.fill(leftPos+7,topPos+8,leftPos+513,topPos+278,0xFF1C2B2A);
        g.fill(leftPos+10,topPos+42,leftPos+510,topPos+45,0xFF38614F);
        if(!healing){
            for(int i=0;i<ROWS;i++){
                int y=topPos+74+i*30;
                g.fill(leftPos+14,y,leftPos+505,y+27,
                        (i&1)==0?0xFF2C3C3A:0xFF263532);
            }
            g.fill(leftPos+14,topPos+238,leftPos+506,topPos+240,0xFF355649);
        }else{
            g.fill(leftPos+15,topPos+58,leftPos+253,topPos+252,0xFF223332);
            g.fill(leftPos+263,topPos+58,leftPos+504,topPos+252,0xFF2A3937);
        }
    }
    private void label(GuiGraphics g,String txt,int x,int y,int col,int max){
        g.drawString(font,font.plainSubstrByWidth(txt,max),x,y,col,false);
    }

    @Override protected void renderLabels(GuiGraphics g,int mx,int my) {
        label(g,"MED STATION",274,21,GREEN,111);
        label(g,"CASH: "+CashDisplay.format(),392,21,GOLD,115);
        if(!healing){
            List<JsonObject> all=recipes();
            for(int i=0;i<ROWS;i++){
                int index=offset+i;
                if(index>=all.size())continue;
                JsonObject r=all.get(index);
                int y=77+i*30;
                ResourceLocation itemId=ResourceLocation.tryParse(field(r,"output",""));
                Item item=itemId==null?Items.AIR:ForgeRegistries.ITEMS.getValue(itemId);
                if(item!=null && item!=Items.AIR)
                    g.renderItem(new ItemStack(item),21,y+3);
                label(g,field(r,"label",field(r,"output","")),44,y+2,WHITE,211);
                StringBuilder ingredients=new StringBuilder();
                JsonArray list=r.getAsJsonArray("ingredients");
                for(JsonElement element:list) {
                    JsonObject ing=element.getAsJsonObject();
                    String id=ing.get("item").getAsString();
                    int required=ing.get("count").getAsInt();
                    if(ingredients.length()>0)ingredients.append("  |  ");
                    ingredients.append(id.substring(id.indexOf(':')+1))
                            .append(" ").append(count(id)).append("/").append(required);
                }
                label(g,ingredients.toString(),44,y+15,canCraft(r)?GREEN:RED,353);
                label(g,"×"+r.get("count").getAsInt(),364,y+3,GOLD,43);
            }
            label(g,(all.isEmpty()?"製造できるレシピがありません":
                    "PRODUCTION    "+(Math.min(offset+1,all.size()))+
                    "-"+Math.min(all.size(),offset+ROWS)+" / "+all.size()),17,229,GRAY,482);
            label(g,"素材：STASH → PLAYER （不足分を自動補充）",18,253,GRAY,245);
        }else{
            label(g,"BODY CONDITION / 身体状態",24,65,GREEN,222);
            List<MedHealthPreview.Part> parts=MedHealthPreview.parts();
            if(parts.isEmpty())label(g,"Healthの状態を取得できません",24,89,GRAY,222);
            for(int i=0;i<parts.size();i++){
                MedHealthPreview.Part part=parts.get(i);
                int y=86+i*21;
                label(g,part.name(),24,y,WHITE,90);
                int start=116,wide=91;
                g.fill(start,y,start+wide,y+9,0xFF101B1A);
                int width=part.maximum()<=0?0:
                        Math.max(0,Math.min(wide,(int)Math.round(
                        wide*(double)part.health()/part.maximum())));
                g.fill(start,y,start+width,y+9,
                        part.health()>=part.maximum()?0xFF5FB68B:0xFFBC6058);
                label(g,part.health()+"/"+part.maximum(),210,y,WHITE,37);
            }
            label(g,"TREATMENT / 治療内容",273,66,GREEN,222);
            label(g,"General health condition",275,95,WHITE,220);
            label(g,"全身HP・血液・出血・骨折・痛みを回復",275,114,GRAY,219);
            label(g,"治療費",275,152,GRAY,200);
            int amount=menu.quote();
            boolean connected=ModList.get().isLoaded("dmzcore") &&
                    ModList.get().isLoaded("dmzpda");
            String price=!connected?"Health + P.D.Aが必要":
                    amount<0?"治療情報を取得できません":
                    amount==0?"治療不要":String.format("%,d CASH",amount);
            label(g,price,275,171,amount>0?GOLD:GRAY,211);
            long balance=CashDisplay.balance();
            if(amount>0 && balance>=0 && balance<amount)
                label(g,"CASH不足：治療には "+String.format("%,d",amount-balance)+
                        " CASH 必要",275,187,RED,218);
            else if(amount<0 && connected)
                label(g,"Healthのバージョンを確認してください",275,187,RED,218);
            label(g,"確認後に即時治療",275,202,GRAY,203);
        }
    }

    @Override public void render(GuiGraphics g,int mx,int my,float partial) {
        List<JsonObject> list=recipes();
        offset=Math.max(0,Math.min(offset,Math.max(0,list.size()-ROWS)));
        for(int i=0;i<ROWS;i++) {
            Button button=craftButtons.get(i);
            button.visible=!healing && offset+i<list.size();
            button.active=button.visible && canCraft(list.get(offset+i));
        }
        search.visible=!healing;
        filterButton.visible=!healing;
        filterButton.setMessage(Component.literal(FILTERS[filter]));
        outputButton.visible=!healing;
        outputButton.setMessage(Component.literal(stashOutput?
                "完成品：スタッシュ優先":"完成品：所持品優先"));
        healButton.visible=healing;
        healButton.active=menu.quote()>0 && (CashDisplay.balance()<0 || CashDisplay.balance()>=menu.quote());
        renderBackground(g);
        super.render(g,mx,my,partial);
        renderTooltip(g,mx,my);
    }
    @Override public boolean mouseScrolled(double mx,double my,double delta) {
        if(!healing && mx>=leftPos+13 && mx<leftPos+510 &&
                my>=topPos+72 && my<topPos+234) {
            offset=Math.max(0,Math.min(Math.max(0,recipes().size()-ROWS),
                    offset-(int)Math.signum(delta)));
            return true;
        }
        return super.mouseScrolled(mx,my,delta);
    }
}
