package com.github.alexthe666.rats.server.legion;
import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid="rats")
public final class SpellRoles extends SimpleJsonResourceReloadListener {
    private static Map<String,String> roles=Map.of();
    private SpellRoles(){super(new Gson(),"rat_legion");}
    @SubscribeEvent public static void reload(AddReloadListenerEvent e){e.addListener(new SpellRoles());}
    @Override protected void apply(Map<ResourceLocation,JsonElement> files,ResourceManager resources,ProfilerFiller profiler){
        Map<String,String> loaded=new HashMap<>();
        files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(file->file.getValue().getAsJsonObject().entrySet().forEach(row->{
            if(!RatLegion.ROLES.contains(row.getKey()))throw new JsonParseException("Unknown legion role "+row.getKey());
            for(JsonElement id:row.getValue().getAsJsonArray())loaded.put(id.getAsString(),row.getKey());
        }));
        roles=Map.copyOf(loaded);
    }
    static String get(String id){return roles.get(id);}
}
