package dev.dmzhideout.menu;

import dev.dmzhideout.block.HideoutBlocks;
import dev.dmzhideout.block.StashBlockEntity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 3-column Stash. Only 54 visible server-owned cells are synced and their
 * mappings are rebuilt at SCROLL ROW (not page) offsets. Existing world NBT
 * and inventory data remain untouched.
 */
public final class StashMenu extends AbstractContainerMenu {
    public static final int VISIBLE=54, COLUMNS=9, VISIBLE_ROWS=6;
    private final Inventory inventory;
    private final BlockPos pos;
    private final StashBlockEntity stash;
    private final ItemStackHandler clientMirror=new ItemStackHandler(VISIBLE);
    private final int[] mapped=new int[VISIBLE];
    // scroll row, max scroll, tier, shared, filter, 54 lock indicators,
    // occupied slots, capacity.
    private final SimpleContainerData info=new SimpleContainerData(5+VISIBLE+2);
    private int scrollRow,maxScroll,sort,filter,revision=-1;
    private String search="";

    public StashMenu(int id,Inventory inventory,FriendlyByteBuf buffer) {
        this(id,inventory,buffer.readBlockPos());
    }
    private StashMenu(int id,Inventory inventory,BlockPos pos) {
        this(id,inventory,pos,inventory.player.level().getBlockEntity(pos)
                instanceof StashBlockEntity entity?entity:null);
    }
    public StashMenu(int id,Inventory inventory,StashBlockEntity stash) {
        this(id,inventory,stash.getBlockPos(),stash);
    }
    private StashMenu(int id,Inventory inventory,BlockPos pos,StashBlockEntity stash) {
        super(HideoutBlocks.STASH_MENU.get(),id);
        this.inventory=inventory;
        this.pos=pos;
        this.stash=stash;
        java.util.Arrays.fill(mapped,-1);

        IItemHandlerModifiable view=new IItemHandlerModifiable() {
            public int getSlots(){return VISIBLE;}
            private int index(int visible) {
                return visible<0||visible>=VISIBLE?-1:mapped[visible];
            }
            public ItemStack getStackInSlot(int n){
                if(!(StashMenu.this.inventory.player instanceof ServerPlayer))
                    return clientMirror.getStackInSlot(n);
                int slot=index(n);
                return slot<0||StashMenu.this.stash==null?ItemStack.EMPTY:
                        StashMenu.this.stash.storage().getStackInSlot(slot);
            }
            public ItemStack insertItem(int n,ItemStack item,boolean simulate) {
                if(!(StashMenu.this.inventory.player instanceof ServerPlayer)
                        ||StashMenu.this.stash==null)return item;
                int slot=index(n);
                return slot<0?item:StashMenu.this.stash.storage()
                        .insertItem(slot,item,simulate);
            }
            public ItemStack extractItem(int n,int amount,boolean simulate) {
                if(!(StashMenu.this.inventory.player instanceof ServerPlayer)
                        ||StashMenu.this.stash==null)return ItemStack.EMPTY;
                int slot=index(n);
                return slot<0||StashMenu.this.stash.isLocked(slot)?ItemStack.EMPTY:
                        StashMenu.this.stash.storage().extractItem(slot,amount,simulate);
            }
            public int getSlotLimit(int n){return 64;}
            public boolean isItemValid(int n,ItemStack item){return true;}
            public void setStackInSlot(int n,ItemStack item){
                if(!(StashMenu.this.inventory.player instanceof ServerPlayer))
                    clientMirror.setStackInSlot(n,item);
                else {
                    int index=index(n);
                    if(index>=0 && StashMenu.this.stash!=null)
                        StashMenu.this.stash.storage().setStackInSlot(index,item);
                }
            }
        };
        // Right: 9x6 visible window into up to 1350 stored item stacks.
        for(int i=0;i<VISIBLE;i++){
            final int visible=i;
            addSlot(new SlotItemHandler(view,i,391+i%9*18,61+i/9*18) {
                @Override public boolean mayPickup(Player player){
                    return StashMenu.this.stash==null ||
                            !(player instanceof ServerPlayer) ||
                            mapped[visible]<0 || !StashMenu.this.stash.isLocked(mapped[visible]);
                }
            });
        }
        // Center: vanilla 27-slot inventory + 9-slot hotbar.
        for(int y=0;y<3;y++)for(int x=0;x<9;x++)
            addSlot(new Slot(inventory,x+y*9+9,187+x*18,162+y*18));
        for(int i=0;i<9;i++)
            addSlot(new Slot(inventory,i,187+i*18,234));

        // Center: normal armor and offhand backed by vanilla Inventory.
        EquipmentSlot[] slots={EquipmentSlot.HEAD,EquipmentSlot.CHEST,
                EquipmentSlot.LEGS,EquipmentSlot.FEET};
        for(int i=0;i<4;i++){
            EquipmentSlot target=slots[i];
            int inventoryIndex=39-i;
            addSlot(new Slot(inventory,inventoryIndex,187,64+i*18){
                @Override public boolean mayPlace(ItemStack stack) {
                    return Mob.getEquipmentSlotForItem(stack)==target;
                }
                @Override public int getMaxStackSize(){return 1;}
            });
        }
        addSlot(new Slot(inventory,40,341,118));
        addDataSlots(info);
        rebuild();
    }

    public BlockPos pos(){return pos;}
    /** Current first visible row, NOT a page index. */
    public int scrollRow(){return info.get(0);}
    public int maxScroll(){return info.get(1);}
    public int tier(){return info.get(2);}
    public boolean shared(){return info.get(3)!=0;}
    public int filterIndex(){return info.get(4);}
    public int occupied(){return info.get(5+VISIBLE);}
    public int capacity(){return info.get(6+VISIBLE);}
    public boolean lockedVisible(int n){
        return n>=0&&n<VISIBLE&&info.get(5+n)!=0;
    }
    public void toggleLock(ServerPlayer player,int n){
        if(stash!=null&&n>=0&&n<VISIBLE&&mapped[n]>=0){
            stash.toggleLock(player,mapped[n]);
            rebuild();
        }
    }
    public void search(String term){
        search=term==null?"":term.toLowerCase(Locale.ROOT).strip();
        scrollRow=0;rebuild();
    }
    public void sort(){sort=(sort+1)%3;scrollRow=0;rebuild();}
    public void filter(){filter=(filter+1)%5;scrollRow=0;rebuild();}
    public void scroll(int delta){scrollTo(scrollRow+delta);}
    public void scrollTo(int absoluteRow){
        scrollRow=Math.max(0,Math.min(maxScroll,absoluteRow));
        rebuild();
    }
    public void updated(){rebuild();}

    private boolean category(ItemStack stack) {
        return switch(filter){
            case 1 -> !stack.isEmpty() && stack.getItem() instanceof BlockItem;
            case 2 -> !stack.isEmpty() && stack.isEdible();
            case 3 -> !stack.isEmpty() &&
                    (stack.getItem() instanceof ArmorItem || stack.isDamageableItem());
            case 4 -> !stack.isEmpty() && "dmzcore".equals(
                    ForgeRegistries.ITEMS.getKey(stack.getItem()).getNamespace());
            default -> true;
        };
    }
    private void rebuild(){
        if(stash==null || !(inventory.player instanceof ServerPlayer))return;
        List<Integer> entries=new ArrayList<>();
        int used=0;
        for(int i=0;i<stash.capacity();i++){
            ItemStack item=stash.storage().getStackInSlot(i);
            if(!item.isEmpty())used++;
            if(!category(item))continue;
            if(!search.isEmpty() && (item.isEmpty() ||
                    !item.getHoverName().getString().toLowerCase(Locale.ROOT).contains(search)
                    && !ForgeRegistries.ITEMS.getKey(item.getItem()).toString().contains(search)))
                continue;
            entries.add(i);
        }
        if(sort!=0){
            Comparator<Integer> cmp=sort==1?
                    Comparator.comparing((Integer i)->{
                        ItemStack stack=stash.storage().getStackInSlot(i);
                        return stack.isEmpty()?"~":stack.getHoverName().getString().toLowerCase(Locale.ROOT);
                    }):Comparator.comparingInt((Integer i)->
                        stash.storage().getStackInSlot(i).getCount()).reversed();
            entries.sort(cmp);
        }
        maxScroll=Math.max(0,(entries.size()+COLUMNS-1)/COLUMNS-VISIBLE_ROWS);
        scrollRow=Math.min(maxScroll,scrollRow);
        for(int i=0;i<VISIBLE;i++){
            int at=scrollRow*COLUMNS+i;
            mapped[i]=at<entries.size()?entries.get(at):-1;
            info.set(5+i,mapped[i]>=0&&stash.isLocked(mapped[i])?1:0);
        }
        info.set(0,scrollRow);
        info.set(1,maxScroll);
        info.set(2,stash.tier());
        info.set(3,stash.isShared()?1:0);
        info.set(4,filter);
        info.set(5+VISIBLE,used);
        info.set(6+VISIBLE,stash.capacity());
        revision=stash.revision();
        broadcastFullState();
    }
    @Override public void broadcastChanges(){
        if(stash!=null && inventory.player instanceof ServerPlayer &&
                revision!=stash.revision())rebuild();
        super.broadcastChanges();
    }
    @Override public boolean stillValid(Player player){
        return stash!=null&&!stash.isRemoved()&&
                player.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=64D&&
                (!(player instanceof ServerPlayer sp)||stash.canAccess(sp));
    }
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(stash==null||index<0||index>=slots.size())return ItemStack.EMPTY;
        Slot source=slots.get(index);
        if(!source.hasItem()||index<VISIBLE&&mapped[index]>=0&&
                stash.isLocked(mapped[index]))return ItemStack.EMPTY;
        ItemStack original=source.getItem().copy();
        if(index>=VISIBLE){
            ItemStack remaining=stash.insert(original,false);
            if(remaining.getCount()==original.getCount())return ItemStack.EMPTY;
            source.set(remaining);
            source.setChanged();
        }else{
            ItemStack current=source.getItem();
            if(!moveItemStackTo(current,VISIBLE,VISIBLE+36,true))
                return ItemStack.EMPTY;
            source.setChanged();
            if(current.isEmpty())source.set(ItemStack.EMPTY);
        }
        return original;
    }
}
