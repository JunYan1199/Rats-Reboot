package com.github.alexthe666.rats.server.entity.rat;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.registry.RatsEntityRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.List;
import java.util.Comparator;
import net.minecraft.world.entity.ai.attributes.Attribute;

@Mod.EventBusSubscriber(modid = RatsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class RatSpellAttributes {
    static final ResourceLocation GOETY = new ResourceLocation("goety", "spell_potency");
    static final ResourceLocation IRON = new ResourceLocation("irons_spellbooks", "spell_power");

    private static List<Attribute> profileAttributes = List.of();

    public static List<Attribute> profileAttributes() { return profileAttributes; }

    public static boolean isSchoolPower(ResourceLocation key) {
        return key.getPath().endsWith("_spell_power") || key.getPath().endsWith("_potency");
    }

    public static double currentGoety(TamedRat rat) { return current(rat, GOETY, rat.grownSpellPower(rat.getGoetyPowerGene())); }
    public static double currentIron(TamedRat rat) { return current(rat, IRON, rat.grownSpellPower(rat.getIronPowerGene())); }

    private static double current(TamedRat rat, ResourceLocation key, float fallback) {
        Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(key);
        return attr != null && rat.getAttributes().hasAttribute(attr) ? rat.getAttributeValue(attr) : fallback;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void register(EntityAttributeModificationEvent event) {
        profileAttributes = ForgeRegistries.ATTRIBUTES.getEntries().stream()
                .filter(entry -> {
                    var key = entry.getKey().location();
                    return key.getNamespace().equals("goety") || key.getNamespace().equals("irons_spellbooks") || isSchoolPower(key);
                })
                .sorted(Comparator.comparing(entry -> entry.getKey().location()))
                .map(java.util.Map.Entry::getValue).toList();
        for (Attribute attribute : profileAttributes) {
            var type = RatsEntityRegistry.TAMED_RAT.get();
            if (!event.has(type, attribute)) event.add(type, attribute);
        }
    }

    static void apply(TamedRat rat, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
        var attr = ForgeRegistries.ATTRIBUTES.getValue(id);
        if (attr == null || rat.level().isClientSide()) return;
        var instance = rat.getAttribute(attr);
        if (instance == null) return;
        String name = "rats:gene/" + id;
        UUID uuid = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
        instance.removeModifier(uuid);
        instance.addTransientModifier(new AttributeModifier(uuid, name, amount, operation));
    }
}
