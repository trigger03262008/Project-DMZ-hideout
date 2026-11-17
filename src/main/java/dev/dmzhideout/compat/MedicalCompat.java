package dev.dmzhideout.compat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * Optional Health (dmzcore) + P.D.A cash integration.
 *
 * Prefer the v1.1.4+ public Health API. Earlier Health v1.1.x builds do not
 * provide the two treatment methods; use their existing public state storage
 * APIs as a backwards-compatible bridge instead of silently returning -1.
 * All balance changes and injury mutations happen on the logical server.
 */
public final class MedicalCompat {
    private static final Logger LOG = LogUtils.getLogger();
    private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();
    private static final String API="dev.dmzcore.api.HealthIntegrationApi";
    private static final String HEALTH="dev.dmzcore.";
    private MedicalCompat() { }

    private static void note(String key,String message,Throwable problem) {
        if(!LOGGED.add(key)) return;
        if(problem==null) LOG.warn(message);
        else LOG.error(message,problem);
    }
    private static Object invoke(String cls,String method,Class<?>[] signature,
                                 Object... args) throws ReflectiveOperationException {
        return Class.forName(cls).getMethod(method,signature).invoke(null,args);
    }
    private static Object read(String cls,ServerPlayer player)
            throws ReflectiveOperationException {
        return invoke(cls,"read",new Class<?>[]{Player.class},player);
    }

    public static int quote(ServerPlayer player) {
        if(player==null||player.level().isClientSide||!ModList.get().isLoaded("dmzcore"))
            return -1;
        try {
            Object value=invoke(API,"quoteFullTreatment",
                    new Class<?>[]{ServerPlayer.class},player);
            return value instanceof Number amount?amount.intValue():-1;
        }catch(ClassNotFoundException|NoSuchMethodException missing) {
            note("legacy","Hideout Med Station: older Health API detected; "+
                    "using compatibility treatment bridge.",null);
            try {
                return legacyQuote(player);
            }catch(ReflectiveOperationException|RuntimeException error){
                note("legacy-quote","Hideout: Health quote compatibility failed",error);
                return -1;
            }
        }catch(ReflectiveOperationException|RuntimeException failed) {
            note("health-quote","Hideout: Health quoteFullTreatment failed. "+
                    "Check the installed Project DMZ: Health JAR/version.",failed);
            return -1;
        }
    }

    /**
     * Only for older Health variants whose public HealthIntegrationApi has
     * no quoteFullTreatment yet. Same formula as Health v1.1.4 API.
     */
    private static int legacyQuote(ServerPlayer player)
            throws ReflectiveOperationException {
        Object body=read(HEALTH+"health.BodyHealthStorage",player);
        Object injury=read(HEALTH+"injury.InjuryStorage",player);
        Object vital=read(HEALTH+"vital.VitalStateStorage",player);
        Class<?> part=Class.forName(HEALTH+"health.BodyPart");
        Class<?> bleed=Class.forName(HEALTH+"injury.BleedType");
        Class<?> bodyClass=body.getClass();
        Class<?> injuryClass=injury.getClass();
        double maxBlood=Class.forName(HEALTH+"health.BodyHealth")
                .getField("MAX_BLOOD_ML").getDouble(null);
        double blood=((Number)bodyClass.getMethod("getBloodMl").invoke(body)).doubleValue();
        double pain=((Number)bodyClass.getMethod("getPain").invoke(body)).doubleValue();
        double amount=(maxBlood-blood)/20.0D+pain*2.0D;
        for(Object partValue:part.getEnumConstants()){
            double max=((Number)part.getMethod("maxHealth").invoke(partValue)).doubleValue();
            double hp=((Number)bodyClass.getMethod("getHealth",part)
                    .invoke(body,partValue)).doubleValue();
            amount+=(max-hp)*4.0D;
            for(Object type:bleed.getEnumConstants()){
                amount+=((Number)injuryClass.getMethod("bleedCount",part,bleed)
                        .invoke(injury,partValue,type)).doubleValue()*75.0D;
            }
            Object fracture=injuryClass.getMethod("fracture",part)
                    .invoke(injury,partValue);
            if(!"NONE".equals(((Enum<?>)fracture).name())) {
                amount+=200.0D;
                if(Boolean.TRUE.equals(injuryClass.getMethod("openFracture",part)
                        .invoke(injury,partValue)))amount+=70.0D;
            }
        }
        if(Boolean.TRUE.equals(vital.getClass().getMethod("downed").invoke(vital)))
            amount+=300.0D;
        if(!Double.isFinite(amount))return -1;
        return (int)Math.max(0,Math.min(1_000_000D,Math.ceil(amount)));
    }

    private static boolean healHealth(ServerPlayer player)
            throws ReflectiveOperationException {
        try {
            return Boolean.TRUE.equals(invoke(API,"healFully",
                    new Class<?>[]{ServerPlayer.class},player));
        }catch(ClassNotFoundException|NoSuchMethodException missing){
            return legacyHeal(player);
        }
    }

    /**
     * Legacy storage writes are confined to Health's own public read/write
     * methods, never direct NBT mutation. Synchronize all Health screens.
     */
    private static boolean legacyHeal(ServerPlayer player)
            throws ReflectiveOperationException {
        Class<?> healthClass=Class.forName(HEALTH+"health.BodyHealth");
        Object body=read(HEALTH+"health.BodyHealthStorage",player);
        healthClass.getMethod("healAll").invoke(body);

        Class<?> injuryClass=Class.forName(HEALTH+"injury.InjuryState");
        Object injury=read(HEALTH+"injury.InjuryStorage",player);
        injuryClass.getMethod("clear").invoke(injury);

        Class<?> vitalClass=Class.forName(HEALTH+"vital.VitalState");
        Object healthy=invoke(HEALTH+"vital.VitalState","healthy",new Class<?>[]{});
        Class<?> staminaClass=Class.forName(HEALTH+"survival.StaminaState");
        Object rested=invoke(HEALTH+"survival.StaminaState","rested",new Class<?>[]{});

        // All methods/classes resolve before the first persistent state write.
        Class<?> bodyStore=Class.forName(HEALTH+"health.BodyHealthStorage");
        Class<?> injuryStore=Class.forName(HEALTH+"injury.InjuryStorage");
        Class<?> vitalStore=Class.forName(HEALTH+"vital.VitalStateStorage");
        Class<?> staminaStore=Class.forName(HEALTH+"survival.StaminaStateStorage");
        Method saveBody=bodyStore.getMethod("write",Player.class,healthClass);
        Method saveInjury=injuryStore.getMethod("write",Player.class,injuryClass);
        Method saveVital=vitalStore.getMethod("write",Player.class,vitalClass);
        Method saveStamina=staminaStore.getMethod("write",Player.class,staminaClass);
        Class<?> network=Class.forName(HEALTH+"network.BodyNetwork");
        Method sync=network.getMethod("sync",ServerPlayer.class);
        Method survival=network.getMethod("syncSurvival",ServerPlayer.class);
        Method medication=network.getMethod("syncMedication",ServerPlayer.class);

        saveBody.invoke(null,player,body);
        saveInjury.invoke(null,player,injury);
        saveVital.invoke(null,player,healthy);
        saveStamina.invoke(null,player,rested);
        player.setHealth(player.getMaxHealth());
        sync.invoke(null,player);
        survival.invoke(null,player);
        medication.invoke(null,player);
        return true;
    }

    private static Throwable root(Throwable exception) {
        return exception instanceof InvocationTargetException target
                && target.getCause()!=null?target.getCause():exception;
    }

    public static boolean heal(ServerPlayer player) {
        int amount=quote(player);
        if(amount<=0)return false;
        if(!ModList.get().isLoaded("dmzpda")){
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "P.D.Aが必要です：治療費を支払えません"),true);
            return false;
        }
        boolean spent=false;
        try {
            Class<?> cash=Class.forName("dev.dmzpda.cash.CashApi");
            Method spend=cash.getMethod("trySpend",ServerPlayer.class,long.class);
            Method credit=cash.getMethod("credit",ServerPlayer.class,long.class);
            if(!Boolean.TRUE.equals(spend.invoke(null,player,(long)amount)))
                return false;
            spent=true;
            try {
                if(healHealth(player))return true;
            }catch(ReflectiveOperationException|RuntimeException error) {
                note("heal-failed","Hideout Med Station: Health treatment failed",
                        root(error));
            }
            if(!Boolean.TRUE.equals(credit.invoke(null,player,(long)amount)))
                LOG.error("CRITICAL: Unable to refund {} CASH after failed treatment",amount);
            return false;
        }catch(ReflectiveOperationException|RuntimeException error) {
            LOG.error("Hideout Med Station: P.D.A cash settlement failed",root(error));
            if(spent){
                try {
                    if(!Boolean.TRUE.equals(Class.forName("dev.dmzpda.cash.CashApi")
                            .getMethod("credit",ServerPlayer.class,long.class)
                            .invoke(null,player,(long)amount)))
                        LOG.error("CRITICAL: Unable to refund {} CASH",amount);
                }catch(ReflectiveOperationException|RuntimeException refundError){
                    LOG.error("CRITICAL: CASH refund failed",root(refundError));
                }
            }
            return false;
        }
    }
}
