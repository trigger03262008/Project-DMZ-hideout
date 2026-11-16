package dev.dmzhideout.client;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.fml.ModList;

/** Reads Health's *existing client cache*, with no required Health dependency. */
public final class MedHealthPreview {
    public record Part(String name, int health, int maximum) { }
    private MedHealthPreview() { }

    public static List<Part> parts() {
        if(!ModList.get().isLoaded("dmzcore"))return List.of();
        try {
            Class<?> bodyPart=Class.forName("dev.dmzcore.health.BodyPart");
            Object snapshot=Class.forName("dev.dmzcore.client.ClientBodyCache")
                    .getMethod("current").invoke(null);
            if(snapshot==null)return List.of();
            Method hp=snapshot.getClass().getMethod("health",bodyPart);
            Method max=bodyPart.getMethod("maxHealth");
            Object[] values=bodyPart.getEnumConstants();
            String[] labels={"HEAD","THORAX","STOMACH","LEFT ARM",
                    "RIGHT ARM","LEFT LEG","RIGHT LEG"};
            List<Part> result=new ArrayList<>();
            for(int i=0;i<Math.min(values.length,labels.length);i++) {
                int current=(int)Math.ceil(((Number)hp.invoke(snapshot,values[i])).doubleValue());
                int maximum=(int)Math.ceil(((Number)max.invoke(values[i])).doubleValue());
                result.add(new Part(labels[i],current,maximum));
            }
            return result;
        }catch(ReflectiveOperationException | RuntimeException ignored) {
            return List.of();
        }
    }
}
