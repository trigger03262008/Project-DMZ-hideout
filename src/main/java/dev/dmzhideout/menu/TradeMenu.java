package dev.dmzhideout.menu;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.dmzhideout.api.StashApi;
import dev.dmzhideout.block.HideoutBlocks;
import dev.dmzhideout.block.StashBlockEntity;
import dev.dmzhideout.compat.TradingCompat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * A non-destructive sell basket and a window over an adjacent authorized
 * Stash. Items remain in the Stash until SELL is explicitly confirmed; the
 * Traders API validates the entire basket and credits CASH atomically.
 */
public final class TradeMenu extends AbstractContainerMenu {
    public static final int STASH_VISIBLE=72,STASH_COLS=12,STASH_ROWS=6;
    public static final int BASKET_VISIBLE=24,BASKET_COLS=8;
    private final BlockPos center;
    private final Inventory inventory;
    // scroll row, max scroll, connected Stash, quote low/high, basket line count,
    // 24 per-line quantities.
    private final SimpleContainerData state=new SimpleContainerData(6+BASKET_VISIBLE+STASH_VISIBLE);
    private final int[] mapped=new int[STASH_VISIBLE];
    private final ItemStackHandler clientStash=new ItemStackHandler(STASH_VISIBLE);
    private final ItemStackHandler clientBasket=new ItemStackHandler(BASKET_VISIBLE);
    private final List<SaleLine> basket=new ArrayList<>();
    private int scrollRow,maxScroll,revision=-1;
    private String query="",selectedTrader="";
    private boolean basketVisible=true;

    private record SaleLine(String offer,String itemId,int count,ItemStack preview) { }

    public TradeMenu(int id,Inventory inventory,FriendlyByteBuf buffer) {
        this(id,inventory,buffer.readBlockPos());
    }
    public TradeMenu(int id,Inventory inventory,BlockPos center) {
        super(HideoutBlocks.TRADE_MENU.get(),id);
        this.inventory=inventory;
        this.center=center;
        java.util.Arrays.fill(mapped,-1);
        IItemHandlerModifiable stashWindow=new IItemHandlerModifiable() {
            public int getSlots(){return STASH_VISIBLE;}
            public ItemStack getStackInSlot(int index){
                if(!(TradeMenu.this.inventory.player instanceof ServerPlayer))
                    return clientStash.getStackInSlot(index);
                StashBlockEntity stash=selectedStash();
                return stash!=null&&mapped[index]>=0?
                        stash.storage().getStackInSlot(mapped[index]):ItemStack.EMPTY;
            }
            public ItemStack insertItem(int i,ItemStack stack,boolean sim){return stack;}
            public ItemStack extractItem(int i,int count,boolean sim){return ItemStack.EMPTY;}
            public int getSlotLimit(int i){return 64;}
            public boolean isItemValid(int i,ItemStack item){return false;}
            public void setStackInSlot(int i,ItemStack stack){
                clientStash.setStackInSlot(i,stack);
            }
        };
        for(int i=0;i<STASH_VISIBLE;i++)
            addSlot(new SlotItemHandler(stashWindow,i,
                    303+i%STASH_COLS*18,109+i/STASH_COLS*18){
                @Override public boolean mayPlace(ItemStack item){return false;}
                @Override public boolean mayPickup(Player player){return false;}
            });
        IItemHandlerModifiable basketWindow=new IItemHandlerModifiable() {
            public int getSlots(){return BASKET_VISIBLE;}
            public ItemStack getStackInSlot(int i){
                if(!(TradeMenu.this.inventory.player instanceof ServerPlayer))
                    return clientBasket.getStackInSlot(i);
                if(i>=basket.size())return ItemStack.EMPTY;
                SaleLine line=basket.get(i);
                ItemStack sample=line.preview().copy();
                sample.setCount(Math.min(line.count(),sample.getMaxStackSize()));
                return sample;
            }
            public ItemStack insertItem(int i,ItemStack stack,boolean sim){return stack;}
            public ItemStack extractItem(int i,int count,boolean sim){return ItemStack.EMPTY;}
            public int getSlotLimit(int i){return 64;}
            public boolean isItemValid(int i,ItemStack item){return false;}
            public void setStackInSlot(int i,ItemStack stack){clientBasket.setStackInSlot(i,stack);}
        };
        for(int i=0;i<BASKET_VISIBLE;i++)
            addSlot(new SlotItemHandler(basketWindow,i,22+i%8*18,110+i/8*18){
                @Override public boolean mayPlace(ItemStack item){return false;}
                @Override public boolean mayPickup(Player player){return false;}
                @Override public boolean isActive(){return TradeMenu.this.basketVisible;}
            });
        addDataSlots(state);
        refresh();
    }

    public BlockPos pos(){return center;}
    /** Client presentation only; does not mutate basket contents or authority. */
    public void showBasket(boolean show){basketVisible=show;}
    public int scrollRow(){return state.get(0);}
    public int maxScroll(){return state.get(1);}
    public boolean hasStash(){return state.get(2)!=0;}
    public long quote(){
        int v=(state.get(3)&65535)|((state.get(4)&65535)<<16);
        return v<0?-1:(long)v;
    }
    public int basketSize(){return state.get(5);}
    public boolean lockedVisible(int i){
        return i>=0&&i<STASH_VISIBLE&&
                state.get(6+BASKET_VISIBLE+i)!=0;
    }
    public int basketCount(int i){
        return i>=0&&i<BASKET_VISIBLE?state.get(6+i):0;
    }

    public StashBlockEntity selectedStash(){
        if(!(inventory.player instanceof ServerPlayer player))return null;
        BlockPos pos=StashApi.findAdjacent(player.serverLevel(),center,player);
        return pos==null?null:player.level().getBlockEntity(pos)
                instanceof StashBlockEntity stash?stash:null;
    }

    public void selectTrader(ServerPlayer player,String traderId){
        if(traderId==null||traderId.length()>128||
                !TradingCompat.traderExists(traderId))return;
        if(!traderId.equals(selectedTrader)){
            selectedTrader=traderId;
            basket.clear();
            scrollRow=0;
            refresh();
        }
    }

    public void search(String value){
        query=value==null?"":value.toLowerCase(Locale.ROOT).strip();
        scrollRow=0;refresh();
    }
    public void scroll(int rows){scrollTo(scrollRow+rows);}
    public void scrollTo(int row){
        scrollRow=Math.max(0,Math.min(maxScroll,row));
        refresh();
    }

    private JsonArray encodedBasket(){
        JsonArray out=new JsonArray();
        for(SaleLine line:basket){
            JsonObject item=new JsonObject();
            item.addProperty("offer",line.offer());
            item.addProperty("item",line.itemId());
            item.addProperty("count",line.count());
            out.add(item);
        }
        return out;
    }

    public void addToBasket(ServerPlayer player,int displayed,String mode){
        if(displayed<0||displayed>=STASH_VISIBLE||mapped[displayed]<0||
                selectedTrader.isBlank())return;
        StashBlockEntity stash=selectedStash();
        if(stash==null||stash.isLocked(mapped[displayed]))return;
        ItemStack stack=stash.storage().getStackInSlot(mapped[displayed]);
        if(stack.isEmpty())return;
        String offer=TradingCompat.buybackOffer(selectedTrader,stack);
        if(offer.isBlank()){
            player.displayClientMessage(Component.literal(
                    "この商人はそのアイテムを買い取りません"),true);
            return;
        }
        String item=ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
        int adding="all".equals(mode)?stack.getCount():
                "half".equals(mode)?(stack.getCount()+1)/2:1;
        int existing=-1;
        for(int i=0;i<basket.size();i++){
            SaleLine line=basket.get(i);
            if(line.offer().equals(offer)&&line.itemId().equals(item)){
                existing=i;break;
            }
        }
        List<SaleLine> before=new ArrayList<>(basket);
        if(existing>=0){
            SaleLine old=basket.get(existing);
            basket.set(existing,new SaleLine(offer,item,old.count()+adding,old.preview()));
        } else {
            if(basket.size()>=BASKET_VISIBLE)return;
            basket.add(new SaleLine(offer,item,adding,stack.copy()));
        }
        if(TradingCompat.quoteBasket(player,center,selectedTrader,encodedBasket())<0){
            basket.clear();basket.addAll(before);
            player.displayClientMessage(Component.literal(
                    "売却数量が不足、または買取条件に合いません"),true);
        }
        refresh();
    }

    public void removeFromBasket(int index,boolean all){
        if(index<0||index>=basket.size())return;
        SaleLine line=basket.get(index);
        if(all||line.count()==1)basket.remove(index);
        else basket.set(index,new SaleLine(line.offer(),line.itemId(),
                line.count()-1,line.preview()));
        refresh();
    }

    public boolean confirmSale(ServerPlayer player){
        if(selectedTrader.isBlank()||basket.isEmpty())return false;
        long result=TradingCompat.sellBasket(player,center,selectedTrader,encodedBasket());
        if(result<=0){
            refresh();
            return false;
        }
        basket.clear();
        player.displayClientMessage(Component.literal(
                "売却完了： +"+result+" CASH"),true);
        refresh();
        return true;
    }

    public void refresh(){
        StashBlockEntity stash=selectedStash();
        List<Integer> positions=new ArrayList<>();
        if(stash!=null){
            for(int i=0;i<stash.capacity();i++){
                ItemStack item=stash.storage().getStackInSlot(i);
                if(item.isEmpty())continue;
                if(query.isEmpty()||item.getHoverName().getString()
                        .toLowerCase(Locale.ROOT).contains(query)||
                        ForgeRegistries.ITEMS.getKey(item.getItem()).toString().contains(query))
                    positions.add(i);
            }
        }
        maxScroll=ScrollMath.maxScroll(positions.size(),STASH_COLS,STASH_ROWS);
        scrollRow=ScrollMath.clamp(scrollRow,maxScroll);
        for(int i=0;i<STASH_VISIBLE;i++){
            int n=ScrollMath.mappedIndex(scrollRow,i,STASH_COLS,positions.size());
            mapped[i]=n>=0?positions.get(n):-1;
            state.set(6+BASKET_VISIBLE+i,stash!=null&&
                    mapped[i]>=0&&stash.isLocked(mapped[i])?1:0);
        }
        state.set(0,scrollRow);
        state.set(1,maxScroll);
        state.set(2,stash==null?0:1);
        long amount=(inventory.player instanceof ServerPlayer player &&
                !selectedTrader.isBlank() && !basket.isEmpty())?
                TradingCompat.quoteBasket(player,center,selectedTrader,encodedBasket()):0;
        int price=(int)Math.max(-1,Math.min(Integer.MAX_VALUE,amount));
        state.set(3,price&65535);
        state.set(4,(price>>>16)&65535);
        state.set(5,basket.size());
        for(int i=0;i<BASKET_VISIBLE;i++)
            state.set(6+i,i<basket.size()?basket.get(i).count():0);
        revision=stash==null?-1:stash.revision();
        broadcastFullState();
    }
    @Override public void broadcastChanges(){
        StashBlockEntity stash=selectedStash();
        if(inventory.player instanceof ServerPlayer &&
                (stash==null?-1:stash.revision())!=revision)refresh();
        super.broadcastChanges();
    }
    @Override public boolean stillValid(Player player){
        return player.level().getBlockState(center).is(HideoutBlocks.TRADING_CENTER.get())
                &&player.distanceToSqr(center.getX()+.5D,center.getY()+.5D,
                        center.getZ()+.5D)<=64D;
    }
    @Override public ItemStack quickMoveStack(Player player,int slot){
        return ItemStack.EMPTY;
    }
}
