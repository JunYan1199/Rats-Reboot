package com.github.alexthe666.rats.server.brick;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid="rats")
public final class KitchenReload {
    @SubscribeEvent public static void sync(OnDatapackSyncEvent e){if(e.getPlayer()==null)KitchenRecipes.invalidate();}
}
