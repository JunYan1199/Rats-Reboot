package com.github.alexthe666.rats.server.misc;

import com.github.alexthe666.rats.RatConfig;
import com.github.alexthe666.rats.RatsMod;
import net.minecraft.world.level.Level;

public final class RatDaySchedule {
    public static final int MORNING_WORK_START = 0;
    public static final int DAY_SLEEP_START = 4000;
    public static final int DAY_SLEEP_END = 8000;
    public static final int EVENING_WORK_END = 12000;

    private RatDaySchedule() {
    }

    public static Phase getPhase(Level level) {
        try {
            boolean sleepTime;
            int dayTime = (int)(level.getDayTime() % 24000L);
            boolean bl = sleepTime = dayTime >= 4000 && dayTime < 8000;
            if (RatConfig.diurnalRats) {
                boolean bl2 = sleepTime = dayTime >= 16000 && dayTime < 20000;
            }
            if (sleepTime) {
                return Phase.DAY_SLEEP;
            }
            if (dayTime >= 0 && dayTime < 4000) {
                return Phase.MORNING_WORK;
            }
            if (dayTime >= 8000 && dayTime < 12000) {
                return Phase.EVENING_WORK;
            }
            return Phase.NIGHT_WORK;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043e\u043f\u0440\u0435\u0434\u0435\u043b\u0438\u0442\u044c \u0444\u0430\u0437\u0443 \u0440\u0430\u0441\u043f\u043e\u0440\u044f\u0434\u043a\u0430 \u043a\u0440\u044b\u0441", (Throwable)exception);
            throw exception;
        }
    }

    public static boolean shouldSleep(Level level) {
        try {
            return RatDaySchedule.getPhase(level) == Phase.DAY_SLEEP;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u043e\u043a\u043d\u043e \u0441\u043d\u0430 \u043a\u0440\u044b\u0441", (Throwable)exception);
            throw exception;
        }
    }

    public static boolean canWork(Level level) {
        try {
            return !RatDaySchedule.shouldSleep(level);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u0440\u0430\u0431\u043e\u0447\u0435\u0435 \u043e\u043a\u043d\u043e \u043a\u0440\u044b\u0441", (Throwable)exception);
            throw exception;
        }
    }

    public static enum Phase {
        MORNING_WORK,
        DAY_SLEEP,
        EVENING_WORK,
        NIGHT_WORK;

    }
}

