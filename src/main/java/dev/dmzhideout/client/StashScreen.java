package dev.dmzhideout.client;

import dev.dmzhideout.menu.StashMenu;
import dev.dmzhideout.network.HideoutNetwork;
import dev.dmzhideout.network.HideoutNetwork.Action;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Tarkov-inspired three columns: reserved equipment / actual player /
 * virtualized scrollable Stash. Equipment add-on slots are visual-only until
 * Inventory and Combat publish real slot APIs (no fake inventories).
 */
public final class StashScreen extends AbstractContainerScreen<StashMenu> {
    private static final int GREEN=0xFF95DBB9, TEXT=0xFFE3F0E8,
            GRAY=0xFFA0B1A9, BG=0xFF192622;
    private static final String[] CATEGORIES={"ALL","BLOCK","FOOD","GEAR","MED"};
    private EditBox search;
    private Button filterButton;
    private boolean scrollDrag;
    private int rigChoice;
    private int layoutSelection=-1;

    public StashScreen(StashMenu menu,Inventory inventory,Component title){
        super(menu,inventory,title);
        imageWidth=574;
        imageHeight=282;
        inventoryLabelY=151;
    }

    @Override protected void init(){
        super.init();
        search=new EditBox(font,leftPos+385,topPos+19,98,17,Component.literal("SEARCH"));
        search.setMaxLength(48);
        search.setHint(Component.literal("検索..."));
        search.setResponder(s->HideoutNetwork.request(Action.STASH_QUERY,menu.pos(),s));
        addRenderableWidget(search);
        filterButton=addRenderableWidget(Button.builder(Component.literal("ALL"),
                b->HideoutNetwork.request(Action.STASH_FILTER,menu.pos()))
                .bounds(leftPos+487,topPos+18,39,20).build());
        addRenderableWidget(Button.builder(Component.literal("SORT"),
                b->HideoutNetwork.request(Action.STASH_SORT,menu.pos()))
                .bounds(leftPos+529,topPos+18,39,20).build());
        addRenderableWidget(Button.builder(Component.literal("共有切替"),
                b->HideoutNetwork.request(Action.STASH_TOGGLE,menu.pos()))
                .bounds(leftPos+12,topPos+242,68,22).build());
        addRenderableWidget(Button.builder(Component.literal("容量強化"),
                b->HideoutNetwork.request(Action.STASH_UPGRADE,menu.pos()))
                .bounds(leftPos+85,topPos+242,72,22).build());
    }

    @Override protected void renderBg(GuiGraphics g,float dt,int mx,int my){
        int x=leftPos,y=topPos;
        g.fill(x,y,x+imageWidth,y+imageHeight,0xFF101A18);
        g.fill(x+7,y+7,x+165,y+275,0xFF25302D);
        g.fill(x+171,y+7,x+375,y+275,0xFF263531);
        g.fill(x+381,y+7,x+569,y+275,0xFF25362F);
        // Distinct slot wells for actual Minecraft inventory and Stash.
        for(int i=0;i<menu.slots.size();i++){
            var slot=menu.slots.get(i);
            int sx=x+slot.x-1,sy=y+slot.y-1;
            g.fill(sx,sy,sx+18,sy+18,i<StashMenu.VISIBLE?0xFF526858:0xFF53655B);
            g.fill(sx+1,sy+1,sx+17,sy+17,0xFF16241F);
            if(i<StashMenu.VISIBLE && menu.lockedVisible(i))
                g.fill(sx+12,sy+2,sx+16,sy+6,0xFFFFC671);
        }
        // All slots except RIG remain visual-only. RIG is an optional
        // client action surface; the Inventory server validates each request.
        String[] gear={"EAR","HEAD","FACE","RIG","ARMOR","BACKPACK",
                "SLING","HOLSTER","POUCH","SPECIAL","MELEE","AMMO"};
        for(int i=0;i<gear.length;i++){
            int col=i%2,row=i/2;
            int sx=x+15+col*73,sy=y+55+row*29;
            g.fill(sx,sy,sx+67,sy+25,0xFF303D38);
            g.fill(sx+1,sy+1,sx+66,sy+24,0xFF1B2925);
            g.drawString(font,gear[i],sx+5,sy+4,0xFF77877D,false);
            if(i==3 && HideoutRigBridge.installed()){
                g.fill(sx+48,sy+14,sx+61,sy+22,0xFF579B75);
                g.drawString(font,"GO",sx+49,sy+15,0xFFF1FBEA,false);
            }else{
                g.fill(sx+49,sy+15,sx+57,sy+22,0xFF839589);
                g.fill(sx+51,sy+10,sx+55,sy+16,0xFF839589);
                g.fill(sx+52,sy+12,sx+54,sy+16,0xFF1B2925);
            }
        }
        // Player model background.
        g.fill(x+226,y+48,x+332,y+151,0xFF172721);
        if(minecraft!=null&&minecraft.player!=null)
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    g,x+280,y+145,36,x+280-mx,y+98-my,minecraft.player);

        // Vertical Stash scroll track and thumb (the storage itself is virtual).
        int top=y+60,height=108,barX=x+558;
        g.fill(barX,top,barX+6,top+height,0xFF15231D);
        int max=menu.maxScroll();
        int thumbH=Math.max(13,(int)(height*6D/(max+6D)));
        int offset=max==0?0:(int)Math.round((height-thumbH)*
                (double)menu.scrollRow()/max);
        g.fill(barX,top+offset,barX+6,top+offset+thumbH,0xFF82BB96);
    }

    @Override protected void renderLabels(GuiGraphics g,int mx,int my){
        g.drawString(font,"EQUIPMENT",15,15,GREEN,false);
        g.drawString(font,HideoutRigBridge.installed()
                ?"RIG / EQUIPMENT":"RESERVED / LOCKED",15,31,GRAY,false);
        g.drawString(font,"PLAYER",186,17,GREEN,false);
        g.drawString(font,"STASH  LV."+menu.tier(),389,45,GREEN,false);
        g.drawString(font,"ARMOR",186,48,GRAY,false);
        g.drawString(font,"INVENTORY",186,150,GRAY,false);
        g.drawString(font,"OFFHAND",323,106,GRAY,false);
        if(HideoutRigBridge.installed()){
            g.drawString(font,"HOTBAR / SIZE CONFIG",187,207,GRAY,false);
            String[] labels={"M","1","4","6","X"};
            for(int i=0;i<9;i++){
                int x=187+i*18;
                g.fill(x,218,x+16,231,i==layoutSelection?0xFF5F8A70:0xFF354B40);
                String type=HideoutRigBridge.type(i);
                String label=switch(type){
                    case "MULTI"->"M";
                    case "SMALL"->"1";
                    case "MEDIUM"->"2";
                    case "LARGE"->"4";
                    case "EXTRA_LARGE"->"6";
                    default->"X";
                };
                g.drawCenteredString(font,label,x+8,220,TEXT);
            }
        }else g.drawString(font,"HOTBAR",187,221,GRAY,false);
        String unlock=HideoutRigBridge.installed()
                ?"RIG: "+(HideoutRigBridge.equipped().isEmpty()?"未装備":HideoutRigBridge.equipped())
                :"Inventory / Combat 未導入";
        g.drawString(font,font.plainSubstrByWidth(unlock,146),14,227,
                0xFF95A59C,false);
        if(HideoutRigBridge.installed()){
            var items=rigCandidates();
            String description=items.length==0?"左RIG:右クリックで解除":
                    "選択 "+(rigChoice%items.length+1)+"/"+items.length+
                    " / 左:装備 右:解除";
            g.drawString(font,font.plainSubstrByWidth(description,149),
                    14,210,GRAY,false);
        }
        g.drawString(font,(menu.shared()?"TEAM SHARED":"PRIVATE")+
                "  "+menu.occupied()+"/"+menu.capacity(),385,187,TEXT,false);
        g.drawString(font,"SCROLL "+(menu.scrollRow()+1)+"/"+
                (menu.maxScroll()+1),385,202,GRAY,false);
        g.drawString(font,"Shift+右クリック：保護 / LOCK",385,224,GRAY,false);
        g.drawString(font,"マウスホイール・バーで上下移動",385,242,GRAY,false);
    }

    private int[] rigCandidates(){
        if(minecraft==null||minecraft.player==null)return new int[0];
        java.util.ArrayList<Integer> cells=new java.util.ArrayList<>();
        for(int cell=0;cell<27;cell++)
            if(HideoutRigBridge.isRig(minecraft.player.getInventory().getItem(9+cell)))
                cells.add(cell);
        return cells.stream().mapToInt(Integer::intValue).toArray();
    }

    private boolean overRig(double mx,double my){
        return mx>=leftPos+88&&mx<leftPos+155
                &&my>=topPos+84&&my<topPos+109;
    }

    private void scrollTo(double localY){
        int y=(int)Math.round(localY);
        int max=menu.maxScroll();
        int target=(int)Math.round(Math.max(0,Math.min(1,(y-61)/107D))*max);
        HideoutNetwork.request(Action.STASH_SCROLL_TO,menu.pos(),Integer.toString(target));
    }

    @Override public boolean mouseScrolled(double mx,double my,double delta) {
        if(HideoutRigBridge.installed()&&overRig(mx,my)){
            int[] items=rigCandidates();
            if(items.length>0)
                rigChoice=Math.floorMod(rigChoice+(delta>0?-1:1),items.length);
            return true;
        }
        if(mx>=leftPos+380 && mx<=leftPos+570 &&
                my>=topPos+40 && my<=topPos+274){
            int n=delta>0?-3:delta<0?3:0;
            if(n!=0)HideoutNetwork.request(Action.STASH_SCROLL,menu.pos(),
                    Integer.toString(n));
            return true;
        }
        return super.mouseScrolled(mx,my,delta);
    }
    @Override public boolean mouseClicked(double mx,double my,int button){
        if(HideoutRigBridge.installed()&&overRig(mx,my)){
            if(button==1){
                if(hasShiftDown())HideoutRigBridge.cancel();
                else HideoutRigBridge.unequip();
                return true;
            }
            if(button==0){
                int[] cells=rigCandidates();
                if(cells.length>0)HideoutRigBridge.equipCell(cells[
                        Math.floorMod(rigChoice,cells.length)]);
                return true;
            }
        }
        if(HideoutRigBridge.installed()&&button==0
                &&mx>=leftPos+187&&mx<leftPos+349
                &&my>=topPos+218&&my<topPos+231){
            int slot=((int)mx-leftPos-187)/18;
            if(layoutSelection<0)layoutSelection=slot;
            else{
                if(layoutSelection!=slot)
                    HideoutRigBridge.swap(layoutSelection,slot);
                layoutSelection=-1;
            }
            return true;
        }
        if(button==0&&mx>=leftPos+557&&mx<=leftPos+566&&
                my>=topPos+59&&my<=topPos+169){
            scrollDrag=true;
            scrollTo(my-topPos);
            return true;
        }
        if(button==1&&hasShiftDown()&&hoveredSlot!=null){
            int index=menu.slots.indexOf(hoveredSlot);
            if(index>=0&&index<StashMenu.VISIBLE){
                HideoutNetwork.request(Action.STASH_LOCK,menu.pos(),
                        Integer.toString(index));
                return true;
            }
        }
        return super.mouseClicked(mx,my,button);
    }
    @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy){
        if(scrollDrag&&button==0){
            scrollTo(my-topPos);
            return true;
        }
        return super.mouseDragged(mx,my,button,dx,dy);
    }
    @Override public boolean mouseReleased(double mx,double my,int button){
        if(button==0&&scrollDrag){scrollDrag=false;return true;}
        return super.mouseReleased(mx,my,button);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        if(filterButton!=null)filterButton.setMessage(Component.literal(
                CATEGORIES[Math.min(CATEGORIES.length-1,Math.max(0,menu.filterIndex()))]));
        renderBackground(g);
        super.render(g,mx,my,partial);
        renderTooltip(g,mx,my);
    }
}
