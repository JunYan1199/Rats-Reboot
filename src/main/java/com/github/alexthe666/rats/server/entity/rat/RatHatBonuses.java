package com.github.alexthe666.rats.server.entity.rat;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.registry.RatsItemRegistry;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = RatsMod.MODID)
public class RatHatBonuses {
    private static final AttributeModifier POWER = new AttributeModifier(
            UUID.fromString("8afdaba9-42e2-48c1-8fc4-accd9882ea5f"),
            "rats.magic_hat_power", 1.0, AttributeModifier.Operation.MULTIPLY_TOTAL);

    @SubscribeEvent
    public static void onModifiers(ItemAttributeModifierEvent event) {
        if (event.getSlotType() != EquipmentSlot.HEAD) return;
        var stack = event.getItemStack();
        ResourceLocation attribute;
        if (stack.is(RatsItemRegistry.IRON_MAGE_HAT.get())) attribute = RatSpellAttributes.IRON;
        else if (stack.is(RatsItemRegistry.GOETY_WIZARD_HAT.get())) attribute = RatSpellAttributes.GOETY;
        else return;
        var nativePower = ForgeRegistries.ATTRIBUTES.getValue(attribute);
        if (nativePower != null) event.addModifier(nativePower, POWER);
    }
}
