package com.github.alexthe666.rats.server.world;

import com.github.alexthe666.rats.RatsMod;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

public class RatProtectedTreeBlocksData
extends SavedData {
    private static final String IDENTIFIER = "rats_protected_tree_blocks";
    private final Set<Long> protectedBlocks = new HashSet<Long>();

    @Nullable
    public static RatProtectedTreeBlocksData get(Level level) {
        try {
            if (!(level instanceof ServerLevel)) {
                return null;
            }
            ServerLevel serverLevel = (ServerLevel)level;
            return (RatProtectedTreeBlocksData)serverLevel.getDataStorage().computeIfAbsent(RatProtectedTreeBlocksData::load, RatProtectedTreeBlocksData::new, IDENTIFIER);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u043e\u043b\u0443\u0447\u0438\u0442\u044c \u0441\u043f\u0438\u0441\u043e\u043a \u0437\u0430\u0449\u0438\u0449\u0451\u043d\u043d\u044b\u0445 \u0434\u0440\u0435\u0432\u0435\u0441\u043d\u044b\u0445 \u0431\u043b\u043e\u043a\u043e\u0432 \u0434\u043b\u044f {}", level.dimension().location(), exception);
            throw exception;
        }
    }

    public static RatProtectedTreeBlocksData load(CompoundTag tag) {
        try {
            RatProtectedTreeBlocksData data = new RatProtectedTreeBlocksData();
            Arrays.stream(tag.getLongArray("ProtectedTreeBlocks")).forEach(data.protectedBlocks::add);
            return data;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0437\u0430\u0433\u0440\u0443\u0437\u0438\u0442\u044c \u0441\u043f\u0438\u0441\u043e\u043a \u0437\u0430\u0449\u0438\u0449\u0451\u043d\u043d\u044b\u0445 \u0434\u0440\u0435\u0432\u0435\u0441\u043d\u044b\u0445 \u0431\u043b\u043e\u043a\u043e\u0432", (Throwable)exception);
            throw exception;
        }
    }

    public void protect(BlockPos pos) {
        try {
            this.protectedBlocks.add(pos.asLong());
            this.setDirty();
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0437\u0430\u0449\u0438\u0442\u0438\u0442\u044c \u0434\u0440\u0435\u0432\u0435\u0441\u043d\u044b\u0439 \u0431\u043b\u043e\u043a {}", pos, exception);
            throw exception;
        }
    }

    public void unprotect(BlockPos pos) {
        try {
            if (this.protectedBlocks.remove(pos.asLong())) {
                this.setDirty();
            }
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u043d\u044f\u0442\u044c \u0437\u0430\u0449\u0438\u0442\u0443 \u0441 \u0434\u0440\u0435\u0432\u0435\u0441\u043d\u043e\u0433\u043e \u0431\u043b\u043e\u043a\u0430 {}", pos, exception);
            throw exception;
        }
    }

    public boolean isProtected(BlockPos pos) {
        try {
            return this.protectedBlocks.contains(pos.asLong());
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u0437\u0430\u0449\u0438\u0442\u0443 \u0434\u0440\u0435\u0432\u0435\u0441\u043d\u043e\u0433\u043e \u0431\u043b\u043e\u043a\u0430 {}", pos, exception);
            throw exception;
        }
    }

    public CompoundTag save(CompoundTag tag) {
        try {
            long[] blocks = this.protectedBlocks.stream().mapToLong(Long::longValue).toArray();
            tag.putLongArray("ProtectedTreeBlocks", blocks);
            return tag;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c \u0441\u043f\u0438\u0441\u043e\u043a \u0437\u0430\u0449\u0438\u0449\u0451\u043d\u043d\u044b\u0445 \u0434\u0440\u0435\u0432\u0435\u0441\u043d\u044b\u0445 \u0431\u043b\u043e\u043a\u043e\u0432", (Throwable)exception);
            throw exception;
        }
    }
}

