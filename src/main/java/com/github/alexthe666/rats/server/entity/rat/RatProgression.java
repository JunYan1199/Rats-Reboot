package com.github.alexthe666.rats.server.entity.rat;

public final class RatProgression {
    public enum Gene { HEALTH, DAMAGE, ARMOR, SPEED, REGEN, GOETY, IRON, COOLDOWN }

    public static final int MAX_LEVEL = 100;
    public static final int MAX_EXPERIENCE = threshold(MAX_LEVEL);

    private RatProgression() {}

    // Level L costs 20 * L experience to advance.
    public static int threshold(int level) {
        return 10 * level * (level - 1);
    }

    public static int levelFor(int total) {
        return Math.min(MAX_LEVEL, (int) ((1.0 + Math.sqrt(1.0 + 0.4 * total)) / 2.0));
    }

    static double growth(int level) {
        double fraction = (level - 1.0) / (MAX_LEVEL - 1.0);
        return fraction * fraction;
    }
}
