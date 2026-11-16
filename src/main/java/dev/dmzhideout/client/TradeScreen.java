package dev.dmzhideout.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.dmzhideout.menu.TradeMenu;
import dev.dmzhideout.network.HideoutNetwork;
import dev.dmzhideout.network.HideoutNetwork.Action;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
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
 * Left: trader stock and buyback; center: settlement/basket;
 * right: immediately adjacent accessible stash preview.
 * Trading never executes on the client.
 */
public final class TradeScreen extends AbstractContainerScreen<TradeMenu> {
    private static final int GREEN=0xFF88E5B9, GOLD=0xFFFFD58E, TEXT=0xFFE3EFE7;
    private String traderId="", offerId="";
    private int traderIndex, offerPage;
    private boolean fromStash, toStash;
    private EditBox search;
    private Button fromButton,toButton;
    private record Offer(JsonObject data, boolean purchase) { }
    public TradeScreen(TradeMenu menu,Inventory inv,Component title) {
        super(menu,inv,title);
        imageWidth=470;
        imageHeight=251;
    }

    @Override protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("<"),
                b -> changeTrader(-1)).bounds(leftPos+9,topPos+20,26,19).build());
        addRenderableWidget(Button.builder(Component.literal(">"),
                b -> changeTrader(1)).bounds(leftPos+124,topPos+20,26,19).build());
        addRenderableWidget(Button.builder(Component.literal("<"),
                b -> offerPage=Math.max(0,offerPage-1))
                .bounds(leftPos+9,topPos+217,25,19).build());
        addRenderableWidget(Button.builder(Component.literal(">"),
                b -> offerPage++)
                .bounds(leftPos+38,topPos+217,25,19).build());

        fromButton=addRenderableWidget(Button.builder(Component.literal("払：所持品"),
                b -> {fromStash=!fromStash;
                    fromButton.setMessage(Component.literal(fromStash?
                            "払：スタッシュ":"払：所持品"));})
                .bounds(leftPos+168,topPos+158,121,20).build());
        toButton=addRenderableWidget(Button.builder(Component.literal("受：所持品"),
                b -> {toStash=!toStash;
                    toButton.setMessage(Component.literal(toStash?
                            "受：スタッシュ":"受：所持品"));})
                .bounds(leftPos+168,topPos+182,121,20).build());
        addRenderableWidget(Button.builder(Component.literal("取引を確定"),
                b -> executeTrade()).bounds(leftPos+168,topPos+209,121,22).build());

        search=new EditBox(font,leftPos+305,topPos+32,119,17,Component.literal("スタッシュ検索"));
        search.setHint(Component.literal("スタッシュ内を検索"));
        search.setMaxLength(48);
        search.setResponder(s -> HideoutNetwork.request(Action.TRADE_QUERY,menu.pos(),s));
        addRenderableWidget(search);
        addRenderableWidget(Button.builder(Component.literal("<"),
                b -> HideoutNetwork.request(Action.TRADE_PREV,menu.pos()))
                .bounds(leftPos+307,topPos+175,29,19).build());
        addRenderableWidget(Button.builder(Component.literal(">"),
                b -> HideoutNetwork.request(Action.TRADE_NEXT,menu.pos()))
                .bounds(leftPos+339,topPos+175,29,19).build());
    }

    @SuppressWarnings("unchecked")
    private List<JsonObject> traders() {
        if(!ModList.get().isLoaded("dmztraders"))return List.of();
        try {
            Class<?> api=Class.forName("dev.dmztraders.client.ClientTraderState");
            Object value=api.getMethod("traders").invoke(null);
            return value instanceof List<?> list ? (List<JsonObject>)list : List.of();
        }catch(ReflectiveOperationException | ClassCastException e){
            return List.of();
        }
    }

    private JsonObject selectedTrader() {
        List<JsonObject> all=traders();
        if(all.isEmpty())return null;
        for(int i=0;i<all.size();i++){
            if(all.get(i).get("id").getAsString().equals(traderId)){
                traderIndex=i;return all.get(i);
            }
        }
        traderIndex=Math.min(traderIndex,all.size()-1);
        JsonObject result=all.get(traderIndex);
        traderId=result.get("id").getAsString();
        return result;
    }

    private List<Offer> offers(JsonObject trader) {
        List<Offer> out=new ArrayList<>();
        if(trader==null)return out;
        for(JsonElement entry:array(trader,"sell_to_player"))
            out.add(new Offer(entry.getAsJsonObject(),true));
        for(JsonElement entry:array(trader,"buy_from_player"))
            out.add(new Offer(entry.getAsJsonObject(),false));
        return out;
    }
    private static JsonArray array(JsonObject obj,String field) {
        return obj.has(field)?obj.getAsJsonArray(field):new JsonArray();
    }
    private static String str(JsonObject obj,String key,String fallback) {
        return obj.has(key)?obj.get(key).getAsString():fallback;
    }

    private void changeTrader(int delta) {
        List<JsonObject> all=traders();
        if(all.isEmpty())return;
        traderIndex=Math.floorMod(traderIndex+delta,all.size());
        traderId=all.get(traderIndex).get("id").getAsString();
        offerId="";offerPage=0;
    }

    private Offer selectedOffer() {
        var list=offers(selectedTrader());
        for(Offer o:list) if(str(o.data(),"id","").equals(offerId))return o;
        if(list.isEmpty())return null;
        Offer first=list.get(0);
        offerId=str(first.data(),"id","");
        return first;
    }

    private void executeTrade() {
        Offer offer=selectedOffer();
        if(offer==null)return;
        HideoutNetwork.request(offer.purchase()?Action.TRADE_BUY:Action.TRADE_SELL,
                menu.pos(),traderId,offerId,fromStash,toStash);
    }

    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my) {
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xFF13201E);
        g.fill(leftPos+5,topPos+5,leftPos+158,topPos+imageHeight-5,0xFF273A32);
        g.fill(leftPos+162,topPos+5,leftPos+295,topPos+imageHeight-5,0xFF20342D);
        g.fill(leftPos+300,topPos+5,leftPos+imageWidth-5,topPos+imageHeight-5,0xFF273A32);
        for(var slot:menu.slots) {
            int x=leftPos+slot.x-1,y=topPos+slot.y-1;
            g.fill(x,y,x+18,y+18,0xFF425A4F);
            g.fill(x+1,y+1,x+17,y+17,0xFF172620);
        }
    }
    private void text(GuiGraphics g,String value,int x,int y,int color,int max) {
        if(max<=0)return;
        g.drawString(font,font.plainSubstrByWidth(value,max),x,y,color,false);
    }

    @Override protected void renderLabels(GuiGraphics g,int mx,int my) {
        JsonObject trader=selectedTrader();
        text(g,"TRADERS / 商品",12,8,GREEN,143);
        text(g,trader==null?"Tradersが必要":str(trader,"name",traderId),
                37,25,TEXT,82);
        text(g,"TRADE / 決済",171,13,GREEN,120);
        text(g,"STASH / 収納",307,13,GREEN,150);
        text(g,menu.hasStash()?"接続中":"隣接スタッシュなし",307,19,
                menu.hasStash()?GREEN:GOLD,152);
        text(g,menu.page()+1+"/"+menu.pages(),380,179,TEXT,72);
        text(g,"左の商品を選択→中央で決済",307,207,GOLD,156);

        List<Offer> products=offers(trader);
        int pageCount=Math.max(1,(products.size()+6)/7);
        offerPage=Math.min(offerPage,pageCount-1);
        for(int i=0;i<7;i++){
            int n=offerPage*7+i;
            if(n>=products.size())break;
            Offer offer=products.get(n);
            JsonObject o=offer.data();
            int y=48+i*23;
            boolean selected=str(o,"id","").equals(offerId);
            g.fill(9,y,152,y+21,selected?0xFF567858:0xFF354940);
            String label=str(o,"label",str(o,"id",""));
            String flag=offer.purchase()?"買":"売";
            text(g,"["+flag+"] "+label,12,y+2,TEXT,134);
            int price=o.has("price")?o.get("price").getAsInt():0;
            text(g,price+" CASH",13,y+12,GOLD,110);
        }
        text(g,(offerPage+1)+"/"+pageCount,71,222,TEXT,71);

        Offer offer=selectedOffer();
        if(offer!=null){
            JsonObject data=offer.data();
            text(g,str(data,"label",offerId),171,48,GREEN,116);
            text(g,offer.purchase()?"トレーダーから購入":"トレーダーに売却",
                    171,65,TEXT,118);
            text(g,"CASH "+(data.has("price")?data.get("price").getAsInt():0),
                    171,83,GOLD,118);
            JsonArray costs=array(data,"cost_items");
            text(g,costs.size()>0?"交換素材："+costs.size()+"種類":"交換素材：なし",
                    171,103,TEXT,116);
            for(int i=0;i<Math.min(2,costs.size());i++){
                JsonObject cost=costs.get(i).getAsJsonObject();
                String item=cost.has("item")?cost.get("item").toString():"?";
                text(g,item+" ×"+str(cost,"count","1"),171,119+i*13,GOLD,112);
            }
        }
        text(g,"物資元と購入先を選択",171,145,0xFFA7C8B6,119);
        if(!menu.hasStash())text(g,"収納への直接取引は使用不可",305,197,GOLD,156);
    }
    @Override public boolean mouseClicked(double mx,double my,int button) {
        if(super.mouseClicked(mx,my,button))return true;
        if(button!=0)return false;
        int x=(int)mx-leftPos,y=(int)my-topPos;
        if(x>=9&&x<=152&&y>=48&&y<48+7*23){
            int row=(y-48)/23;
            List<Offer> list=offers(selectedTrader());
            int n=offerPage*7+row;
            if(n<list.size()) {
                offerId=str(list.get(n).data(),"id","");
                return true;
            }
        }
        return false;
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial) {
        renderBackground(g);
        super.render(g,mx,my,partial);
        renderTooltip(g,mx,my);
    }
}
