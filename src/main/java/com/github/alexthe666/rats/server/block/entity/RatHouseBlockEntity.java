package com.github.alexthe666.rats.server.block.entity;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.registry.RatsBlockEntityRegistry;
import com.github.alexthe666.rats.registry.RatsEntityRegistry;
import com.github.alexthe666.rats.server.block.RatHouseBlock;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.misc.RatDaySchedule;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class RatHouseBlockEntity
extends BlockEntity {
    @Nullable
    private UUID boundRatUuid;
    @Nullable
    private CompoundTag storedRatTag;
    @Nullable
    private CompoundTag pendingRespawnTag;
    private int pendingRespawnTicks;

    public RatHouseBlockEntity(BlockPos pos, BlockState state) {
        super((BlockEntityType)RatsBlockEntityRegistry.RAT_HOUSE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RatHouseBlockEntity house) {
        try {
            if (level.isClientSide()) {
                return;
            }
            if (house.pendingRespawnTag != null) {
                if (house.pendingRespawnTicks > 0) {
                    --house.pendingRespawnTicks;
                    house.setChangedAndSync();
                    return;
                }
                Vec3 entry = RatHouseBlock.getEntryCenter(state, pos);
                house.releasePendingRespawn(entry.x, entry.y, entry.z);
                return;
            }
            if (house.storedRatTag != null && !RatDaySchedule.shouldSleep(level)) {
                Vec3 entry = RatHouseBlock.getEntryCenter(state, pos);
                house.releaseSleepingRatToWorld(entry.x, entry.y, entry.z);
            }
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043e\u0431\u043d\u043e\u0432\u0438\u0442\u044c \u0434\u043e\u043c\u0438\u043a {}", pos, exception);
            throw exception;
        }
    }

    public boolean hasStoredRat() {
        return this.storedRatTag != null;
    }

    public int getStoredRatCount() {
        try {
            return this.storedRatTag == null ? 0 : 1;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u043e\u043b\u0443\u0447\u0438\u0442\u044c \u0447\u0438\u0441\u043b\u043e \u043a\u0440\u044b\u0441 \u0432 \u0434\u043e\u043c\u0438\u043a\u0435 {}: {}", this.worldPosition, exception.getMessage(), exception);
            throw exception;
        }
    }

    public boolean hasPendingRespawn() {
        try {
            return this.pendingRespawnTag != null;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u043e\u0436\u0438\u0434\u0430\u044e\u0449\u0438\u0439 \u0440\u0435\u0441\u043f\u0430\u0432\u043d \u0434\u043e\u043c\u0438\u043a\u0430 {}", this.worldPosition, exception);
            throw exception;
        }
    }

    public int getPendingRespawnTicks() {
        try {
            return this.pendingRespawnTicks;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u043e\u043b\u0443\u0447\u0438\u0442\u044c \u0442\u0430\u0439\u043c\u0435\u0440 \u0440\u0435\u0441\u043f\u0430\u0432\u043d\u0430 \u0434\u043e\u043c\u0438\u043a\u0430 {}", this.worldPosition, exception);
            throw exception;
        }
    }

    public String getStoredRatName() {
        try {
            if (this.storedRatTag == null) {
                return "";
            }
            String storedName = this.storedRatTag.getString("StoredRatName");
            if (!storedName.isBlank()) {
                return storedName;
            }
            if (this.storedRatTag.contains("CustomName", 8)) {
                return this.storedRatTag.getString("CustomName");
            }
            return this.boundRatUuid == null ? "" : this.boundRatUuid.toString();
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u043e\u043b\u0443\u0447\u0438\u0442\u044c \u0438\u043c\u044f \u043a\u0440\u044b\u0441\u044b \u0432 \u0434\u043e\u043c\u0438\u043a\u0435 {}: {}", this.worldPosition, exception.getMessage(), exception);
            throw exception;
        }
    }

    public boolean canAcceptRat(TamedRat rat) {
        try {
            return this.boundRatUuid == null || this.boundRatUuid.equals(rat.getUUID());
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u043f\u0440\u0438\u0432\u044f\u0437\u043a\u0443 \u0434\u043e\u043c\u0438\u043a\u0430 {} \u0434\u043b\u044f \u043a\u0440\u044b\u0441\u044b {}", this.worldPosition, rat.getUUID(), exception);
            throw exception;
        }
    }

    public boolean bindRat(TamedRat rat, Player player) {
        try {
            if(!rat.isOwnedBy(player)) return false;
            if (this.boundRatUuid != null && !this.boundRatUuid.equals(rat.getUUID())) {
                player.displayClientMessage((Component)Component.translatable((String)"block.rats.rat_house.occupied"), true);
                RatsMod.LOGGER.warn("\u0418\u0433\u0440\u043e\u043a {} \u043f\u044b\u0442\u0430\u043b\u0441\u044f \u043f\u0440\u0438\u0432\u044f\u0437\u0430\u0442\u044c \u043a\u0440\u044b\u0441\u0443 {} \u043a \u0437\u0430\u043d\u044f\u0442\u043e\u043c\u0443 \u0434\u043e\u043c\u0438\u043a\u0443 {} (\u0437\u0430\u043d\u044f\u0442 {})", player.getGameProfile().getName(), rat.getUUID(), this.worldPosition, this.boundRatUuid);
                return false;
            }
            rat.getBoundHousePos().ifPresent(old -> { var oldLevel=player.getServer().getLevel(old.dimension()); if(oldLevel!=null && oldLevel.getBlockEntity(old.pos()) instanceof RatHouseBlockEntity oldHouse && oldHouse!=this) oldHouse.unbindUuid(rat.getUUID()); });
            this.boundRatUuid = rat.getUUID();
            rat.setBoundHousePos(GlobalPos.of((ResourceKey)player.level().dimension(), (BlockPos)this.worldPosition));
            this.setChangedAndSync();
            player.displayClientMessage((Component)Component.translatable((String)"block.rats.rat_house.bind", (Object[])new Object[]{rat.getName()}), true);
            return true;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u0438\u0432\u044f\u0437\u0430\u0442\u044c \u043a\u0440\u044b\u0441\u0443 {} \u043a \u0434\u043e\u043c\u0438\u043a\u0443 {}: {}", rat.getUUID(), this.worldPosition, exception.getMessage(), exception);
            throw exception;
        }
    }

    public boolean storeRat(TamedRat rat) {
        try {
            if (this.storedRatTag != null || this.pendingRespawnTag != null) {
                RatsMod.LOGGER.warn("\u0414\u043e\u043c\u0438\u043a {} \u0443\u0436\u0435 \u0441\u043e\u0434\u0435\u0440\u0436\u0438\u0442 \u043a\u0440\u044b\u0441\u0443, \u043d\u043e\u0432\u0430\u044f \u043a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u043f\u043e\u043c\u0435\u0449\u0435\u043d\u0430", this.worldPosition, rat.getUUID());
                return false;
            }
            if (!this.canAcceptRat(rat)) {
                RatsMod.LOGGER.warn("\u0414\u043e\u043c\u0438\u043a {} \u043e\u0442\u043a\u0430\u0437\u0430\u043b \u043a\u0440\u044b\u0441\u0435 {}: \u043f\u0440\u0438\u0432\u044f\u0437\u0430\u043d UUID {}", this.worldPosition, rat.getUUID(), this.boundRatUuid);
                return false;
            }
            CompoundTag tag = new CompoundTag();
            rat.saveWithoutId(tag);
            tag.putUUID("StoredRatUuid", rat.getUUID());
            tag.putString("StoredRatName", rat.getName().getString());
            this.boundRatUuid = rat.getUUID();
            this.storedRatTag = tag;
            rat.stopRiding();
            rat.getNavigation().stop();
            rat.discard();
            this.setChangedAndSync();
            Level level = this.getLevel();
            if (level != null) {
                level.playSound(null, this.worldPosition, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.7f, 1.1f);
            }
            return true;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u043e\u043c\u0435\u0441\u0442\u0438\u0442\u044c \u043a\u0440\u044b\u0441\u0443 {} \u0432 \u0434\u043e\u043c\u0438\u043a {}: {}", rat.getUUID(), this.worldPosition, exception.getMessage(), exception);
            throw exception;
        }
    }

    public boolean beginRespawn(TamedRat rat, int ticks) {
        try {
            if (this.storedRatTag != null || this.pendingRespawnTag != null) {
                RatsMod.LOGGER.warn("\u0414\u043e\u043c\u0438\u043a {} \u043d\u0435 \u043f\u0440\u0438\u043d\u044f\u043b \u0440\u0435\u0441\u043f\u0430\u0432\u043d \u043a\u0440\u044b\u0441\u044b {}: stored={}, pending={}", this.worldPosition, rat.getUUID(), (this.storedRatTag != null ? 1 : 0), (this.pendingRespawnTag != null ? 1 : 0));
                return false;
            }
            if (!this.canAcceptRat(rat)) {
                RatsMod.LOGGER.warn("\u0414\u043e\u043c\u0438\u043a {} \u043e\u0442\u043a\u0430\u0437\u0430\u043b \u0432 \u0440\u0435\u0441\u043f\u0430\u0432\u043d\u0435 \u043a\u0440\u044b\u0441\u044b {}: \u043f\u0440\u0438\u0432\u044f\u0437\u0430\u043d UUID {}", this.worldPosition, rat.getUUID(), this.boundRatUuid);
                return false;
            }
            CompoundTag tag = new CompoundTag();
            rat.saveWithoutId(tag);
            tag.putUUID("StoredRatUuid", rat.getUUID());
            tag.putString("StoredRatName", rat.getName().getString());
            tag.putShort("HurtTime", (short)0);
            tag.putInt("HurtByTimestamp", 0);
            tag.putShort("DeathTime", (short)0);
            this.boundRatUuid = rat.getUUID();
            this.pendingRespawnTag = tag;
            this.pendingRespawnTicks = Math.max(1, ticks);
            this.setChangedAndSync();
            RatsMod.LOGGER.info("\u0414\u043e\u043c\u0438\u043a {} \u043d\u0430\u0447\u0430\u043b \u0440\u0435\u0441\u043f\u0430\u0432\u043d \u043a\u0440\u044b\u0441\u044b {} \u0447\u0435\u0440\u0435\u0437 {} \u0442\u0438\u043a\u043e\u0432", this.worldPosition, rat.getUUID(), this.pendingRespawnTicks);
            return true;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0437\u0430\u043f\u0443\u0441\u0442\u0438\u0442\u044c \u0440\u0435\u0441\u043f\u0430\u0432\u043d \u043a\u0440\u044b\u0441\u044b {} \u0443 \u0434\u043e\u043c\u0438\u043a\u0430 {}", rat.getUUID(), this.worldPosition, exception);
            throw exception;
        }
    }

    private boolean releasePendingRespawn(double x, double y, double z) {
        try {
            Level level = this.getLevel();
            if (!(level instanceof ServerLevel) || this.pendingRespawnTag == null) {
                return false;
            }
            TamedRat rat = (TamedRat)((EntityType)RatsEntityRegistry.TAMED_RAT.get()).create(level);
            if (rat == null) {
                throw new IllegalStateException("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u043e\u0437\u0434\u0430\u0442\u044c TamedRat \u0438\u0437 \u0440\u0435\u0435\u0441\u0442\u0440\u0430");
            }
            CompoundTag tag = this.pendingRespawnTag.copy();
            rat.load(tag);
            rat.scheduleUpgradeRefreshAfterLoad();
            rat.setHealth(rat.getMaxHealth());
            rat.setRespawnCountdown(0);
            rat.setBoundHousePos(GlobalPos.of((ResourceKey)level.dimension(), (BlockPos)this.worldPosition));
            rat.setPos(x, y, z);
            if (!level.addFreshEntity((Entity)rat)) return false;
            this.boundRatUuid = rat.getUUID();
            this.pendingRespawnTag = null;
            this.pendingRespawnTicks = 0;
            this.setChangedAndSync();
            level.playSound(null, this.worldPosition, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.8f, 1.2f);
            RatsMod.LOGGER.info("\u041a\u0440\u044b\u0441\u0430 {} \u0432\u0435\u0440\u043d\u0443\u043b\u0430\u0441\u044c \u0443 \u0434\u043e\u043c\u0438\u043a\u0430 {}", rat.getUUID(), this.worldPosition);
            return true;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0432\u044b\u043f\u0443\u0441\u0442\u0438\u0442\u044c \u043e\u0436\u0438\u0434\u0430\u044e\u0449\u0443\u044e \u0440\u0435\u0441\u043f\u0430\u0432\u043d\u0430 \u043a\u0440\u044b\u0441\u0443 \u0438\u0437 \u0434\u043e\u043c\u0438\u043a\u0430 {}", this.worldPosition, exception);
            throw exception;
        }
    }

    private boolean releaseSleepingRatToWorld(double x, double y, double z) {
        try {
            Level level = this.getLevel();
            if (!(level instanceof ServerLevel) || this.storedRatTag == null) {
                return false;
            }
            TamedRat rat = (TamedRat)((EntityType)RatsEntityRegistry.TAMED_RAT.get()).create(level);
            if (rat == null) {
                throw new IllegalStateException("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u043e\u0437\u0434\u0430\u0442\u044c TamedRat \u0438\u0437 \u0440\u0435\u0435\u0441\u0442\u0440\u0430");
            }
            CompoundTag tag = this.storedRatTag.copy();
            rat.load(tag);
            rat.scheduleUpgradeRefreshAfterLoad();
            rat.setHealth(Math.max(1.0f, rat.getMaxHealth()));
            rat.setRespawnCountdown(0);
            rat.setBoundHousePos(GlobalPos.of((ResourceKey)level.dimension(), (BlockPos)this.worldPosition));
            rat.setPos(x, y, z);
            if (!level.addFreshEntity((Entity)rat)) return false;
            this.boundRatUuid = rat.getUUID();
            this.storedRatTag = null;
            this.setChangedAndSync();
            level.playSound(null, this.worldPosition, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 0.7f, 1.15f);
            RatsMod.LOGGER.info("\u041a\u0440\u044b\u0441\u0430 {} \u043f\u0440\u043e\u0441\u043d\u0443\u043b\u0430\u0441\u044c \u0438 \u0432\u044b\u0448\u043b\u0430 \u0438\u0437 \u0434\u043e\u043c\u0438\u043a\u0430 {}", rat.getUUID(), this.worldPosition);
            return true;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0432\u044b\u043f\u0443\u0441\u0442\u0438\u0442\u044c \u043f\u0440\u043e\u0441\u043d\u0443\u0432\u0448\u0443\u044e\u0441\u044f \u043a\u0440\u044b\u0441\u0443 \u0438\u0437 \u0434\u043e\u043c\u0438\u043a\u0430 {}", this.worldPosition, exception);
            throw exception;
        }
    }

    public boolean releaseRatToWorld(double x, double y, double z) {
        try {
            Level level = this.getLevel();
            if (level == null) {
                throw new IllegalStateException("\u0423 \u0434\u043e\u043c\u0438\u043a\u0430 \u043d\u0435\u0442 \u0443\u0440\u043e\u0432\u043d\u044f");
            }
            if (level.isClientSide() || this.storedRatTag == null && this.pendingRespawnTag == null) {
                return false;
            }
            TamedRat rat = (TamedRat)((EntityType)RatsEntityRegistry.TAMED_RAT.get()).create(level);
            if (rat == null) {
                throw new IllegalStateException("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u043e\u0437\u0434\u0430\u0442\u044c TamedRat \u0438\u0437 \u0440\u0435\u0435\u0441\u0442\u0440\u0430");
            }
            CompoundTag tag = (this.storedRatTag != null ? this.storedRatTag : this.pendingRespawnTag).copy();
            rat.load(tag);
            rat.scheduleUpgradeRefreshAfterLoad();
            rat.setHealth(Math.max(1.0f, rat.getMaxHealth()));
            rat.setRespawnCountdown(0);
            rat.setBoundHousePos(null);
            rat.setPos(x, y, z);
            if (!level.addFreshEntity((Entity)rat)) return false;
            this.boundRatUuid = null;
            this.storedRatTag = null;
            this.pendingRespawnTag = null;
            this.pendingRespawnTicks = 0;
            this.setChangedAndSync();
            level.playSound(null, this.worldPosition, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 0.8f, 0.9f);
            RatsMod.LOGGER.info("\u041a\u0440\u044b\u0441\u0430 {} \u0432\u044b\u043f\u0443\u0449\u0435\u043d\u0430 \u0438\u0437 \u0440\u0430\u0437\u0440\u0443\u0448\u0435\u043d\u043d\u043e\u0433\u043e \u0434\u043e\u043c\u0438\u043a\u0430 {}", rat.getUUID(), this.worldPosition);
            return true;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0432\u044b\u043f\u0443\u0441\u0442\u0438\u0442\u044c \u043a\u0440\u044b\u0441\u0443 \u0438\u0437 \u0440\u0430\u0437\u0440\u0443\u0448\u0435\u043d\u043d\u043e\u0433\u043e \u0434\u043e\u043c\u0438\u043a\u0430 {}: {}", this.worldPosition, exception.getMessage(), exception);
            throw exception;
        }
    }

    public boolean releaseRatToPlayer(Player player) {
        try {
            Level level = this.getLevel();
            if (level == null) {
                throw new IllegalStateException("\u0423 \u0434\u043e\u043c\u0438\u043a\u0430 \u043d\u0435\u0442 \u0443\u0440\u043e\u0432\u043d\u044f");
            }
            if (this.storedRatTag == null) {
                player.displayClientMessage((Component)Component.translatable((String)"block.rats.rat_house.empty"), true);
                return false;
            }
            if (player.getPassengers().size() >= 3) {
                player.displayClientMessage((Component)Component.translatable((String)"block.rats.rat_house.shoulder_full"), true);
                return false;
            }
            TamedRat rat = (TamedRat)((EntityType)RatsEntityRegistry.TAMED_RAT.get()).create(level);
            if (rat == null) {
                throw new IllegalStateException("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u043e\u0437\u0434\u0430\u0442\u044c TamedRat \u0438\u0437 \u0440\u0435\u0435\u0441\u0442\u0440\u0430");
            }
            CompoundTag tag = this.storedRatTag.copy();
            rat.load(tag);
            rat.scheduleUpgradeRefreshAfterLoad();
            rat.setBoundHousePos(GlobalPos.of((ResourceKey)level.dimension(), (BlockPos)this.worldPosition));
            if(!rat.isOwnedBy(player)) return false;
            rat.setPos(player.getX(), player.getY(), player.getZ());
            if (!level.addFreshEntity((Entity)rat)) return false;
            rat.startRiding((Entity)player, true);
            this.boundRatUuid = rat.getUUID();
            this.storedRatTag = null;
            this.pendingRespawnTag = null;
            this.pendingRespawnTicks = 0;
            this.setChangedAndSync();
            level.playSound(null, this.worldPosition, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 0.7f, 1.2f);
            player.displayClientMessage((Component)Component.translatable((String)"block.rats.rat_house.withdraw", (Object[])new Object[]{rat.getName()}), true);
            return true;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0434\u043e\u0441\u0442\u0430\u0442\u044c \u043a\u0440\u044b\u0441\u0443 \u0438\u0437 \u0434\u043e\u043c\u0438\u043a\u0430 {}: {}", this.worldPosition, exception.getMessage(), exception);
            throw exception;
        }
    }

    public boolean clearBinding(TamedRat rat, Player player) {
        try {
            if (this.storedRatTag != null) {
                RatsMod.LOGGER.warn("\u0414\u043e\u043c\u0438\u043a {} \u043d\u0435 \u0441\u043d\u0438\u043c\u0430\u0435\u0442 \u043f\u0440\u0438\u0432\u044f\u0437\u043a\u0443 \u043a\u0440\u044b\u0441\u044b {}: \u043a\u0440\u044b\u0441\u0430 \u0443\u0436\u0435 \u0432\u043d\u0443\u0442\u0440\u0438", this.worldPosition, rat.getUUID());
                return false;
            }
            if (this.boundRatUuid == null || !this.boundRatUuid.equals(rat.getUUID())) {
                return false;
            }
            this.boundRatUuid = null;
            rat.setBoundHousePos(null);
            this.setChangedAndSync();
            player.displayClientMessage((Component)Component.translatable((String)"block.rats.rat_house.unbind", (Object[])new Object[]{rat.getName()}), true);
            return true;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u043d\u044f\u0442\u044c \u043f\u0440\u0438\u0432\u044f\u0437\u043a\u0443 \u043a\u0440\u044b\u0441\u044b {} \u043e\u0442 \u0434\u043e\u043c\u0438\u043a\u0430 {}: {}", rat.getUUID(), this.worldPosition, exception.getMessage(), exception);
            throw exception;
        }
    }

    public void onRemoved() {
        try {
            RatsMod.LOGGER.debug("\u0423\u0434\u0430\u043b\u0451\u043d block entity rat_house \u0432 {}, storedRat={}", this.worldPosition, (this.storedRatTag != null ? 1 : 0));
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041e\u0448\u0438\u0431\u043a\u0430 \u043f\u0440\u0438 \u0443\u0434\u0430\u043b\u0435\u043d\u0438\u0438 block entity rat_house \u0432 {}: {}", this.worldPosition, exception.getMessage(), exception);
            throw exception;
        }
    }

    protected void saveAdditional(CompoundTag tag) {
        try {
            super.saveAdditional(tag);
            if (this.boundRatUuid != null) {
                tag.putUUID("BoundRatUuid", this.boundRatUuid);
            }
            if (this.storedRatTag != null) {
                tag.put("StoredRat", (Tag)this.storedRatTag.copy());
            }
            if (this.pendingRespawnTag != null) {
                tag.put("PendingRespawnRat", (Tag)this.pendingRespawnTag.copy());
                tag.putInt("PendingRespawnTicks", this.pendingRespawnTicks);
            }
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c NBT rat_house \u0432 {}: {}", this.worldPosition, exception.getMessage(), exception);
            throw exception;
        }
    }

    public void load(CompoundTag tag) {
        try {
            super.load(tag);
            this.boundRatUuid = tag.hasUUID("BoundRatUuid") ? tag.getUUID("BoundRatUuid") : null;
            this.storedRatTag = tag.contains("StoredRat", 10) ? tag.getCompound("StoredRat").copy() : null;
            this.pendingRespawnTag = tag.contains("PendingRespawnRat", 10) ? tag.getCompound("PendingRespawnRat").copy() : null;
            this.pendingRespawnTicks = tag.getInt("PendingRespawnTicks");
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0437\u0430\u0433\u0440\u0443\u0437\u0438\u0442\u044c NBT rat_house \u0432 {}: {}", this.worldPosition, exception.getMessage(), exception);
            throw exception;
        }
    }

    @Nullable
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        try {
            return ClientboundBlockEntityDataPacket.create((BlockEntity)this);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u043e\u0437\u0434\u0430\u0442\u044c \u043f\u0430\u043a\u0435\u0442 \u043e\u0431\u043d\u043e\u0432\u043b\u0435\u043d\u0438\u044f rat_house \u0432 {}: {}", this.worldPosition, exception.getMessage(), exception);
            throw exception;
        }
    }

    public CompoundTag getUpdateTag() {
        try {
            return this.saveWithoutMetadata();
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u043e\u0431\u0440\u0430\u0442\u044c updateTag rat_house \u0432 {}: {}", this.worldPosition, exception.getMessage(), exception);
            throw exception;
        }
    }

    private void setChangedAndSync() {
        try {
            this.setChanged();
            Level level = this.getLevel();
            if (level != null) {
                BlockState state = level.getBlockState(this.worldPosition);
                level.sendBlockUpdated(this.worldPosition, state, state, 3);
            }
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u0438\u043d\u0445\u0440\u043e\u043d\u0438\u0437\u0438\u0440\u043e\u0432\u0430\u0442\u044c rat_house \u0432 {}: {}", this.worldPosition, exception.getMessage(), exception);
            throw exception;
        }
    }

public void unbindUuid(UUID id){if(id.equals(boundRatUuid)&&storedRatTag==null&&pendingRespawnTag==null){boundRatUuid=null;setChangedAndSync();}}
}
