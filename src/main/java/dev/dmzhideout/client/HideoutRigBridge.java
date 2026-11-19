package dev.dmzhideout.client;

import java.lang.reflect.Method;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

/** Optional client adapter; Inventory owns the rig ItemStacks and all mutations. */
public final class HideoutRigBridge {
    private static Class<?> state,actions;
    private static boolean resolved;
    private HideoutRigBridge(){}
    public static boolean installed(){return ModList.get().isLoaded("dmzinventory");}
    private static void resolve(){
        if(resolved)return;
        resolved=true;
        if(!installed())return;
        try{
            state=Class.forName("dev.dmzinventory.client.ClientRigState");
            actions=Class.forName("dev.dmzinventory.client.RigClientActions");
        }catch(ClassNotFoundException|LinkageError e){state=null;actions=null;}
    }
    private static Object read(String name,Class<?>[] sig,Object... args){
        resolve();if(state==null)return null;
        try{return state.getMethod(name,sig).invoke(null,args);}
        catch(ReflectiveOperationException|RuntimeException|LinkageError e){return null;}
    }
    private static void send(String name,Class<?>[] sig,Object... args){
        resolve();if(actions==null)return;
        try{actions.getMethod(name,sig).invoke(null,args);}
        catch(ReflectiveOperationException|RuntimeException|LinkageError ignored){}
    }
    public static String equipped(){
        Object o=read("equipped",new Class<?>[0]);
        return o instanceof String s?s:"";
    }
    public static String type(int index){
        Object o=read("type",new Class<?>[]{int.class},index);
        return o==null?"LOCKED":o.toString();
    }
    public static boolean isRig(ItemStack stack){
        if(stack==null||stack.isEmpty()||!installed())return false;
        var id=BuiltInRegistries.ITEM.getKey(stack.getItem());
        if(!"dmzinventory".equals(id.getNamespace()))return false;
        return switch(id.getPath()){
            case "scrap_vest","type57_chikor","spirix_bank_runner","trident_m43b",
                    "wartac_mk3","black_rook","anv_altra","lct_1962a" -> true;
            default -> false;
        };
    }
    public static void equipCell(int cell){send("equipCell",new Class<?>[]{int.class},cell);}
    public static void unequip(){send("unequip",new Class<?>[0]);}
    public static void cancel(){send("cancel",new Class<?>[0]);}
    public static void swap(int a,int b){send("swapPositions",
            new Class<?>[]{int.class,int.class},a,b);}
}
