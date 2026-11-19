package com.github.alexthe666.rats.compat.jade;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.block.RatHouseBlock;
import com.github.alexthe666.rats.server.block.entity.RatHouseBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;

import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
public class RatsJadePlugin
implements IWailaPlugin {
    private static final RatHouseProvider RAT_HOUSE_PROVIDER = new RatHouseProvider();

    public void register(IWailaCommonRegistration registration) {
        try {
            registration.registerBlockDataProvider((IServerDataProvider)RAT_HOUSE_PROVIDER, RatHouseBlockEntity.class);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0437\u0430\u0440\u0435\u0433\u0438\u0441\u0442\u0440\u0438\u0440\u043e\u0432\u0430\u0442\u044c \u0441\u0435\u0440\u0432\u0435\u0440\u043d\u044b\u0435 \u0434\u0430\u043d\u043d\u044b\u0435 Jade \u0434\u043b\u044f \u0434\u043e\u043c\u0438\u043a\u0430 \u043a\u0440\u044b\u0441\u044b", (Throwable)exception);
            throw exception;
        }
    }

    public void registerClient(IWailaClientRegistration registration) {
        try {
            registration.addConfig(RatHouseProvider.UID, true);
            registration.registerBlockComponent(RAT_HOUSE_PROVIDER, RatHouseBlock.class);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0437\u0430\u0440\u0435\u0433\u0438\u0441\u0442\u0440\u0438\u0440\u043e\u0432\u0430\u0442\u044c \u043a\u043b\u0438\u0435\u043d\u0442\u0441\u043a\u0438\u0439 \u043a\u043e\u043c\u043f\u043e\u043d\u0435\u043d\u0442 Jade \u0434\u043b\u044f \u0434\u043e\u043c\u0438\u043a\u0430 \u043a\u0440\u044b\u0441\u044b", (Throwable)exception);
            throw exception;
        }
    }

    private static class RatHouseProvider
    implements IBlockComponentProvider,
    IServerDataProvider<BlockAccessor> {
        private static final ResourceLocation UID = new ResourceLocation((String)"rats", (String)"rat_house");
        private static final String COUNT_KEY = "RatHouseRatCount";
        private static final String NAME_KEY = "RatHouseRatName";
        private static final String RESPAWN_KEY = "RatHouseRespawnTicks";

        private RatHouseProvider() {
        }

        public ResourceLocation getUid() {
            return UID;
        }

        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            try {
                BlockEntity blockEntity = accessor.getBlockEntity();
                if (blockEntity instanceof RatHouseBlockEntity) {
                    RatHouseBlockEntity house = (RatHouseBlockEntity)blockEntity;
                    data.putInt(COUNT_KEY, house.getStoredRatCount());
                    data.putString(NAME_KEY, house.getStoredRatName());
                    data.putInt(RESPAWN_KEY, house.getPendingRespawnTicks());
                }
            }
            catch (RuntimeException exception) {
                RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u043e\u0431\u0440\u0430\u0442\u044c \u0434\u0430\u043d\u043d\u044b\u0435 Jade \u0434\u043b\u044f \u0434\u043e\u043c\u0438\u043a\u0430 \u043a\u0440\u044b\u0441\u044b \u0432 {}", accessor.getPosition(), exception);
                throw exception;
            }
        }

        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            try {
                BlockEntity blockEntity;
                CompoundTag data = accessor.getServerData();
                int count = data.getInt(COUNT_KEY);
                String name = data.getString(NAME_KEY);
                int respawnTicks = data.getInt(RESPAWN_KEY);
                if (!data.contains(COUNT_KEY) && (blockEntity = accessor.getBlockEntity()) instanceof RatHouseBlockEntity) {
                    RatHouseBlockEntity house = (RatHouseBlockEntity)blockEntity;
                    count = house.getStoredRatCount();
                    name = house.getStoredRatName();
                    respawnTicks = house.getPendingRespawnTicks();
                }
                if (respawnTicks > 0) {
                    tooltip.add((Component)Component.translatable((String)"jade.rats.rat_house.respawn", (Object[])new Object[]{Math.max(1, (respawnTicks + 19) / 20)}));
                    return;
                }
                if (count <= 0) {
                    tooltip.add((Component)Component.translatable((String)"jade.rats.rat_house.empty"));
                    return;
                }
                if (name.isBlank()) {
                    tooltip.add((Component)Component.translatable((String)"jade.rats.rat_house.occupied", (Object[])new Object[]{count, 1}));
                    return;
                }
                tooltip.add((Component)Component.translatable((String)"jade.rats.rat_house.rat", (Object[])new Object[]{name}));
            }
            catch (RuntimeException exception) {
                RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0432\u044b\u0432\u0435\u0441\u0442\u0438 \u043f\u043e\u0434\u0441\u043a\u0430\u0437\u043a\u0443 Jade \u0434\u043b\u044f \u0434\u043e\u043c\u0438\u043a\u0430 \u043a\u0440\u044b\u0441\u044b \u0432 {}", accessor.getPosition(), exception);
                throw exception;
            }
        }
    }
}

