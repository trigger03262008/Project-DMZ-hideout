package dev.dmzhideout.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.dmzhideout.menu.TradeMenu;
import dev.dmzhideout.network.HideoutNetwork;
import dev.dmzhideout.network.HideoutNetwork.Action;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * BUY: choose a product and CASH/barter source.
 * SELL: queue eligible Stash items (no actual removal) and confirm once.
 * Merchant cards are automatically paged; JSON buy_from_player defines who
 * may buy which items. No client-side sale mutations or trusted price inputs.
 */
public final class TradeScreen extends AbstractContainerScreen<TradeMenu> {
    private static final int GREEN=0xFF9BDBBA,WHITE=0xFFE7EFE9,
            GRAY=0xFF9EB3A8,GOLD=0xFFFFCF80,RED=0xFFF47E72;
    private static final int TRADERS_PER_PAGE=5,BUYS_PER_VIEW=6;
    private boolean sellMode=true,fromStash=false,toStash=false,scrollDrag;
    private int pendingStash=-1;
    private String traderId="",buyOfferId="",sentTrader="";
    private int traderPage,productOffset;
    private EditBox search;
    private Button buyTab,sellTab,paySource,outputTarget,confirmButton;
    private final List<Button> traderButtons=new ArrayList<>();

    public TradeScreen(TradeMenu menu,Inventory inv,Component title) {
        super(menu,inv,title);
        imageWidth=548;
        imageHeight=297;
    }

    @Override protected void init(){
        super.init();
        addRenderableWidget(Button.builder(Component.literal("<"),
                b->traderPage=Math.max(0,traderPage-1))
                .bounds(leftPos+7,topPos+17,24,33).build());
        addRenderableWidget(Button.builder(Component.literal(">"),
                b->traderPage=Math.min(Math.max(0,(traders().size()-1)/TRADERS_PER_PAGE),
                        traderPage+1))
                .bounds(leftPos+518,topPos+17,24,33).build());
        for(int i=0;i<TRADERS_PER_PAGE;i++){
            final int slot=i;
            traderButtons.add(addRenderableWidget(Button.builder(Component.literal("---"),
                    b->chooseTrader(traderPage*TRADERS_PER_PAGE+slot))
                    .bounds(leftPos+33+i*96,topPos+17,92,33).build()));
        }
        buyTab=addRenderableWidget(Button.builder(Component.literal("BUY / 購入"),
                b->sellMode=false).bounds(leftPos+14,topPos+59,124,25).build());
        sellTab=addRenderableWidget(Button.builder(Component.literal("SELL / 売却"),
                b->sellMode=true).bounds(leftPos+143,topPos+59,124,25).build());

        search=new EditBox(font,leftPos+303,topPos+79,148,18,Component.literal("Stash検索"));
        search.setMaxLength(48);
        search.setHint(Component.literal("アイテム検索"));
        search.setResponder(s->HideoutNetwork.request(Action.TRADE_QUERY,menu.pos(),s));
        addRenderableWidget(search);

        addRenderableWidget(Button.builder(Component.literal("↑"),
                b->HideoutNetwork.request(Action.TRADE_SCROLL,menu.pos(),"-3"))
                .bounds(leftPos+461,topPos+79,32,18).build());
        addRenderableWidget(Button.builder(Component.literal("↓"),
                b->HideoutNetwork.request(Action.TRADE_SCROLL,menu.pos(),"3"))
                .bounds(leftPos+495,topPos+79,32,18).build());

        paySource=addRenderableWidget(Button.builder(Component.literal("支払：STASH"),
                b->{fromStash=!fromStash;})
                .bounds(leftPos+284,topPos+255,119,21).build());
        outputTarget=addRenderableWidget(Button.builder(Component.literal("受取：STASH"),
                b->{toStash=!toStash;})
                .bounds(leftPos+406,topPos+255,122,21).build());
        confirmButton=addRenderableWidget(Button.builder(Component.literal("SELL / 売却確定"),
                b->{if(sellMode)HideoutNetwork.request(Action.TRADE_BATCH_SELL,menu.pos());
                    else buySelected();})
                .bounds(leftPos+159,topPos+258,111,27).build());
        addRenderableWidget(Button.builder(Component.literal("<"),
                b->productOffset=Math.max(0,productOffset-1))
                .bounds(leftPos+15,topPos+259,28,22).build());
        addRenderableWidget(Button.builder(Component.literal(">"),
                b->productOffset++)
                .bounds(leftPos+47,topPos+259,28,22).build());
    }

    @SuppressWarnings("unchecked")
    private List<JsonObject> traders(){
        if(!ModList.get().isLoaded("dmztraders"))return List.of();
        try{
            Object val=Class.forName("dev.dmztraders.client.ClientTraderState")
                    .getMethod("traders").invoke(null);
            return val instanceof List<?> list?(List<JsonObject>)list:List.of();
        }catch(ReflectiveOperationException|ClassCastException e){return List.of();}
    }
    private static String val(JsonObject obj,String field,String fallback) {
        return obj!=null&&obj.has(field)?obj.get(field).getAsString():fallback;
    }
    private static JsonArray list(JsonObject obj,String field){
        return obj!=null&&obj.has(field)?obj.getAsJsonArray(field):new JsonArray();
    }
    private JsonObject selectedTrader(){
        List<JsonObject> pool=traders();
        if(pool.isEmpty())return null;
        for(JsonObject t:pool)if(val(t,"id","").equals(traderId))return t;
        traderId=val(pool.get(0),"id","");
        traderPage=0;
        return pool.get(0);
    }
    private void chooseTrader(int absolute){
        List<JsonObject> pool=traders();
        if(absolute<0||absolute>=pool.size())return;
        traderId=val(pool.get(absolute),"id","");
        productOffset=0;
        buyOfferId="";
        syncTrader();
    }
    private void syncTrader(){
        if(!traderId.isBlank()&&!traderId.equals(sentTrader)){
            HideoutNetwork.request(Action.TRADE_SELECT_TRADER,menu.pos(),traderId);
            sentTrader=traderId;
        }
    }
    private List<JsonObject> buyOffers(){
        List<JsonObject> result=new ArrayList<>();
        for(JsonElement element:list(selectedTrader(),"sell_to_player"))
            result.add(element.getAsJsonObject());
        return result;
    }
    private JsonObject selectedBuy(){
        List<JsonObject> products=buyOffers();
        for(JsonObject product:products)
            if(val(product,"id","").equals(buyOfferId))return product;
        if(products.isEmpty())return null;
        buyOfferId=val(products.get(0),"id","");
        return products.get(0);
    }
    private void buySelected(){
        JsonObject offer=selectedBuy();
        if(offer==null||traderId.isBlank())return;
        HideoutNetwork.request(Action.TRADE_BUY,menu.pos(),
                traderId,val(offer,"id",""),fromStash,toStash);
    }

    private boolean isEligible(ItemStack stack) {
        if(stack.isEmpty())return false;
        String id=ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
        for(JsonElement entry:list(selectedTrader(),"buy_from_player")){
            JsonObject offer=entry.getAsJsonObject();
            JsonElement descriptor=offer.get("item");
            if(descriptor==null)continue;
            if(descriptor.isJsonPrimitive()){
                if(id.equals(descriptor.getAsString())&&!stack.hasTag())return true;
            }else if(descriptor.isJsonObject()){
                JsonObject spec=descriptor.getAsJsonObject();
                String adapter=val(spec,"adapter","dmztraders:item");
                boolean nbt=spec.has("allow_nbt")&&spec.get("allow_nbt").getAsBoolean();
                if(stack.hasTag()&&!nbt)continue;
                String target=val(spec,"id","");
                if("dmztraders:item".equals(adapter)&&id.equals(target))return true;
                if("dmztraders:tag".equals(adapter)){
                    ResourceLocation tag=ResourceLocation.tryParse(target);
                    if(tag!=null&&stack.is(TagKey.create(Registries.ITEM,tag)))return true;
                }
            }
        }
        return false;
    }

    private int rightVisible(double mx,double my){
        return TradeGridHitTest.slot(mx-leftPos,my-topPos,
                303,109,12,6,18);
    }
    private int basketVisible(double mx,double my){
        return TradeGridHitTest.slot(mx-leftPos,my-topPos,
                22,110,8,3,18);
    }

    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        int x=leftPos,y=topPos;
        g.fill(x,y,x+imageWidth,y+imageHeight,0xFF101A19);
        g.fill(x+7,y+10,x+542,y+54,0xFF26332E);
        g.fill(x+8,y+55,x+278,y+290,0xFF273732);
        g.fill(x+284,y+55,x+540,y+290,0xFF263832);
        for(int i=0;i<menu.slots.size();i++){
            var slot=menu.slots.get(i);
            int sx=x+slot.x-1,sy=y+slot.y-1;
            g.fill(sx,sy,sx+18,sy+18,0xFF4F665B);
            g.fill(sx+1,sy+1,sx+17,sy+17,0xFF192720);
        }
        if(!sellMode)
            for(int i=0;i<BUYS_PER_VIEW;i++){
                int row=y+108+i*24;
                g.fill(x+15,row,x+270,row+22,i%2==0?0xFF334B3C:0xFF2F4239);
            }
        // Slide thumb for the right-hand Stash.
        int trackX=x+530,trackY=y+109,length=108;
        g.fill(trackX,trackY,trackX+6,trackY+length,0xFF17251E);
        int max=menu.maxScroll();
        int thumb=Math.max(12,(int)(length*6D/(max+6D)));
        int at=max==0?0:(int)((length-thumb)*(double)menu.scrollRow()/max);
        g.fill(trackX,trackY+at,trackX+6,trackY+at+thumb,0xFF91CBA6);
        // Highlight the selected merchant's upper tab.
        List<JsonObject> pool=traders();
        for(int i=0;i<TRADERS_PER_PAGE;i++){
            int index=traderPage*TRADERS_PER_PAGE+i;
            if(index<pool.size()&&val(pool.get(index),"id","").equals(traderId))
                g.fill(x+34+i*96,y+49,x+124+i*96,y+52,0xFF77D2A0);
        }
    }

    private void label(GuiGraphics g,String txt,int x,int y,int color,int max){
        g.drawString(font,font.plainSubstrByWidth(txt,max),x,y,color,false);
    }

    @Override protected void renderLabels(GuiGraphics g,int mx,int my){
        JsonObject trader=selectedTrader();
        label(g,trader==null?"Traders未導入":val(trader,"name",traderId),
                12,91,GREEN,259);
        label(g,"STASH / 保管庫",302,62,GREEN,221);
        label(g,menu.hasStash()?"接続中":"スタッシュが隣接していません",
                302,68,menu.hasStash()?GREEN:RED,210);
        label(g,"SCROLL "+(menu.scrollRow()+1)+"/"+(menu.maxScroll()+1),
                301,225,GRAY,210);
        if(sellMode){
            label(g,"SELL / 売却予定アイテム",15,99,WHITE,253);
            long quote=menu.quote();
            label(g,quote<0?"買取条件が変わりました":
                    "受取見積: +"+String.format(Locale.ROOT,"%,d",quote)+" CASH",
                    15,206,quote<0?RED:GOLD,257);
            label(g,"売却対象 "+menu.basketSize()+" 種類",15,222,GRAY,247);
            label(g,"右のアイテムをクリックで追加",15,238,GRAY,253);
            label(g,"Shift+クリック:全数 / 左の品:右クリック除外",85,279,GRAY,194);
            // Item icons remain visible but untradeable items are dimmed.
            for(int i=0;i<TradeMenu.STASH_VISIBLE;i++){
                ItemStack stack=menu.slots.get(i).getItem();
                if(stack.isEmpty())continue;
                if(!isEligible(stack)||menu.lockedVisible(i)){
                    int sx=303+i%12*18,sy=109+i/12*18;
                    g.fill(sx,sy,sx+16,sy+16,0xAA101615);
                    if(menu.lockedVisible(i))
                        label(g,"L",sx+5,sy+4,GOLD,13);
                }
            }
            for(int i=0;i<menu.basketSize() && i<TradeMenu.BASKET_VISIBLE;i++){
                label(g,menu.basketCount(i)+"",23+i%8*18,126+i/8*18,
                        GOLD,18);
            }
        }else{
            label(g,"BUY / 商品一覧",15,99,WHITE,258);
            List<JsonObject> products=buyOffers();
            productOffset=Math.min(productOffset,Math.max(0,products.size()-BUYS_PER_VIEW));
            for(int i=0;i<BUYS_PER_VIEW;i++){
                int n=productOffset+i;
                if(n>=products.size())break;
                JsonObject o=products.get(n);
                int y=111+i*24;
                boolean active=val(o,"id","").equals(buyOfferId);
                if(active)g.fill(15,y-3,270,y+18,0x504EB886);
                label(g,val(o,"label",val(o,"id","")),18,y,WHITE,208);
                int price=o.has("price")?o.get("price").getAsInt():0;
                JsonArray costs=list(o,"cost_items");
                String cost=price==0?"物々交換":price+" CASH";
                if(costs.size()>0)cost+=" + 物資"+costs.size()+"種";
                label(g,cost,18,y+11,GOLD,220);
            }
            label(g,"選択した商品を購入：",16,242,GRAY,239);
            JsonObject selected=selectedBuy();
            if(selected!=null)label(g,val(selected,"label",buyOfferId),16,254,GREEN,137);
        }
    }

    private void clickProduct(double mx,double my){
        double x=mx-leftPos,y=my-topPos;
        if(x<15||x>=271||y<108||y>=108+BUYS_PER_VIEW*24)return;
        int row=(int)((y-108)/24);
        List<JsonObject> products=buyOffers();
        int index=productOffset+row;
        if(index>=0&&index<products.size())
            buyOfferId=val(products.get(index),"id","");
    }

    private void scrollTo(double localY){
        int max=menu.maxScroll();
        int target=(int)Math.round(Math.max(0,Math.min(1,(localY-109)/108D))*max);
        HideoutNetwork.request(Action.TRADE_SCROLL_TO,menu.pos(),Integer.toString(target));
    }

    @Override public boolean mouseClicked(double mx,double my,int button){
        if(button==0&&mx>=leftPos+529&&mx<leftPos+539&&
                my>=topPos+108&&my<topPos+219){
            scrollDrag=true;scrollTo(my-topPos);return true;
        }
        if(sellMode){
            int stash=rightVisible(mx,my);
            if(stash>=0&&button==0){
                ItemStack item=menu.slots.get(stash).getItem();
                if(!item.isEmpty()&&!menu.lockedVisible(stash)&&isEligible(item)){
                    if(hasShiftDown())
                        HideoutNetwork.request(Action.TRADE_BASKET_ADD,menu.pos(),
                                Integer.toString(stash),"all",false,false);
                    else pendingStash=stash; // release after click OR drag-to-basket
                }
                return true;
            }
            int item=basketVisible(mx,my);
            if(item>=0&&(button==1||button==0)){
                HideoutNetwork.request(Action.TRADE_BASKET_REMOVE,menu.pos(),
                        Integer.toString(item),hasShiftDown()?"all":"one",false,false);
                return true;
            }
        }else if(button==0 && mx>=leftPos+15 && mx<=leftPos+272 &&
                my>=topPos+108 && my<=topPos+253){
            clickProduct(mx,my);return true;
        }
        return super.mouseClicked(mx,my,button);
    }
    @Override public boolean mouseScrolled(double mx,double my,double delta){
        if(mx>=leftPos+282 && mx<=leftPos+541 && my>=topPos+91 &&
                my<=topPos+248){
            int rows=delta>0?-3:delta<0?3:0;
            if(rows!=0)HideoutNetwork.request(Action.TRADE_SCROLL,menu.pos(),
                    Integer.toString(rows));
            return true;
        }
        if(!sellMode && mx>=leftPos+8 && mx<=leftPos+280){
            productOffset=Math.max(0,Math.min(Math.max(0,buyOffers().size()-BUYS_PER_VIEW),
                    productOffset-(int)Math.signum(delta)));
            return true;
        }
        return super.mouseScrolled(mx,my,delta);
    }
    @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy){
        if(scrollDrag && button==0){scrollTo(my-topPos);return true;}
        if(pendingStash>=0&&button==0)return true;
        return super.mouseDragged(mx,my,button,dx,dy);
    }
    @Override public boolean mouseReleased(double mx,double my,int button){
        if(scrollDrag&&button==0){scrollDrag=false;return true;}
        if(pendingStash>=0&&button==0){
            int source=pendingStash;
            pendingStash=-1;
            // A click on the Stash OR a drag into the SELL basket adds one.
            if(sellMode&&(rightVisible(mx,my)>=0||basketVisible(mx,my)>=0))
                HideoutNetwork.request(Action.TRADE_BASKET_ADD,menu.pos(),
                        Integer.toString(source),"one",false,false);
            return true;
        }
        return super.mouseReleased(mx,my,button);
    }

    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        List<JsonObject> pool=traders();
        JsonObject current=selectedTrader();
        syncTrader();
        int lastPage=Math.max(0,(pool.size()-1)/TRADERS_PER_PAGE);
        traderPage=Math.min(traderPage,lastPage);
        for(int i=0;i<TRADERS_PER_PAGE;i++){
            int index=traderPage*TRADERS_PER_PAGE+i;
            Button button=traderButtons.get(i);
            button.visible=index<pool.size();
            if(button.visible){
                String name=val(pool.get(index),"name","TRADER");
                button.setMessage(Component.literal(
                        font.plainSubstrByWidth(name,84)));
            }
        }
        buyTab.active=sellMode;
        sellTab.active=!sellMode;
        paySource.visible=!sellMode;
        outputTarget.visible=!sellMode;
        paySource.setMessage(Component.literal(fromStash?"支払：STASH":"支払：所持品"));
        outputTarget.setMessage(Component.literal(toStash?"受取：STASH":"受取：所持品"));
        confirmButton.setMessage(Component.literal(sellMode?"SELL / 一括売却":"BUY / 購入確定"));
        confirmButton.active=traderId!=null&&!traderId.isBlank()&&
                (sellMode?menu.quote()>0:selectedBuy()!=null);
        renderBackground(g);
        super.render(g,mx,my,partial);
        if(pendingStash>=0 && pendingStash<TradeMenu.STASH_VISIBLE)
            g.renderItem(menu.slots.get(pendingStash).getItem(),mx-8,my-8);
        renderTooltip(g,mx,my);
    }
}
