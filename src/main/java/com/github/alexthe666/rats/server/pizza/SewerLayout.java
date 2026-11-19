package com.github.alexthe666.rats.server.pizza;

import java.util.*;

/**
 * 下水道副本布局（纯 Java，可离线出图）：一条圆拱顶的主通道从入口一路通到底部的鼠头头大厅，
 * 中途经过两个穹顶圆厅，从圆厅和通道中段分出几条较窄的圆拱支路，尽头是死胡同小房间。
 * 纽约式：下半截石砖竖墙、上半截红砖半圆拱顶、每隔几格一道深色拱肋、墙上通长铜管、中间宽水渠、拱顶挂吊灯。
 * 坐标：x 0..SX-1，z 0..SZ-1（入口在 z 小的一头），y 0..H-1（游戏里整体抬到 y=BASE_Y）。
 */
public final class SewerLayout {
    public static final int SX = 112, SZ = 210, H = 27, BASE_Y = 40;
    public static final byte VOID = 0, AIR = 1, BRICK = 2, MOSSY = 3, CRACKED = 4, FLOOR = 5, WATER = 6, MUD = 7, PILLAR = 8, CEIL = 9,
            LANTERN = 10, COBWEB = 11, BARS = 12, BARREL = 13, CHAIN = 14, EXIT = 15, PIPE = 16, GRATE = 17, PIZZA_BOX = 18, CANDLE = 19, SLIME = 20,
            VAULT = 21, VAULT_D = 22, RIB = 23;

    public final byte[] cells = new byte[SX * H * SZ];
    private final byte[] owner = new byte[SX * H * SZ];          // 0 无 / 1 房间内部 / 2 房间外壳
    public final List<int[]> spawnPoints = new ArrayList<>();
    public int[] start, exit, boss;
    private final Random r;

    public static int walkY() { return 4; }

    // 主通道与几个节点的位置
    static final int MX = 56, START_Z = 10, HALL1_Z = 60, HALL2_Z = 116, BOSS_Z0 = 158, BOSS_Z1 = 204;
    static final int MAIN_R = 9, MAIN_S = 6, BRANCH_R = 5, BRANCH_S = 5, HALL_R = 17, HALL_S = 6, BOSS_R = 15, BOSS_S = 6;

    public SewerLayout(long seed) {
        r = new Random(seed);
        // 1. 房间（先放，通道再从它们中间穿过去开门）
        rotunda(MX, START_Z, 7, 5, 1);                                   // 入口房
        rotunda(MX, HALL1_Z, HALL_R, HALL_S, 2);
        rotunda(MX, HALL2_Z, HALL_R, HALL_S, 2);
        vaultRoom(true, MX, BOSS_Z0, BOSS_Z1, BOSS_R, BOSS_S, 3);        // 鼠头头大厅（沿 z 的筒拱）
        // 支路尽头的小房间（沿 x 的筒拱，死胡同）
        int[][] branches = {{HALL1_Z, -1}, {HALL1_Z, 1}, {88, 1}, {HALL2_Z, -1}, {140, -1}};
        List<int[]> rooms = new ArrayList<>();
        for (int[] b : branches) {
            if (r.nextInt(5) == 0 && rooms.size() >= 3) continue;        // 偶尔少一条
            int side = b[1], cz = b[0], cx = MX + side * 40;
            vaultRoom(false, cz, cx - 7, cx + 7, 6, 5, 4);
            rooms.add(new int[]{cx, cz, side});
        }
        // 2. 通道
        corridor(true, MX, START_Z + 4, BOSS_Z0 + 2, MAIN_R, MAIN_S);   // 从入口房前半截开始，水渠不流到返回鼠洞底下
        for (int[] rm : rooms) {
            int cx = rm[0], cz = rm[1], side = rm[2];
            corridor(false, cz, Math.min(MX, cx), Math.max(MX, cx), BRANCH_R, BRANCH_S);
        }
        // 3. 入口、鼠头头、刷怪点
        set(MX, 3, START_Z, EXIT);
        exit = new int[]{MX, 3, START_Z};
        start = new int[]{MX + 4, walkY(), START_Z + 1};
        int bz = (BOSS_Z0 + BOSS_Z1) / 2 + 4;
        for (int x = MX - 5; x <= MX + 5; x++) for (int z = bz - 5; z <= bz + 5; z++) {
            double d = Math.hypot(x - MX, z - bz);
            if (d > 5.4) continue;
            set(x, 4, z, FLOOR);
            if (d > 4.4 && r.nextInt(2) == 0) set(x, 5, z, PIZZA_BOX);
        }
        boss = new int[]{MX, walkY() + 1, bz};
        for (int z = START_Z + 22; z < BOSS_Z0; z += 16) spawnPoints.add(new int[]{MX + ((z / 16) % 2 == 0 ? 6 : -6), walkY(), z});
        for (int[] rm : rooms) spawnPoints.add(new int[]{rm[0], walkY(), rm[1] + 2});
        for (int hz : new int[]{HALL1_Z, HALL2_Z}) { spawnPoints.add(new int[]{MX + 8, walkY(), hz + 6}); spawnPoints.add(new int[]{MX - 8, walkY(), hz - 6}); }
        spawnPoints.add(new int[]{MX + 10, walkY(), BOSS_Z0 + 8}); spawnPoints.add(new int[]{MX - 10, walkY(), BOSS_Z0 + 8});
    }

    int idx(int x, int y, int z) { return (y * SZ + z) * SX + x; }
    public byte get(int x, int y, int z) { return x < 0 || y < 0 || z < 0 || x >= SX || y >= H || z >= SZ ? VOID : cells[idx(x, y, z)]; }
    void set(int x, int y, int z, byte b) { if (x >= 0 && y >= 0 && z >= 0 && x < SX && y < H && z < SZ) cells[idx(x, y, z)] = b; }
    byte own(int x, int y, int z) { return x < 0 || y < 0 || z < 0 || x >= SX || y >= H || z >= SZ ? 0 : owner[idx(x, y, z)]; }
    void own(int x, int y, int z, int o) { if (x >= 0 && y >= 0 && z >= 0 && x < SX && y < H && z < SZ) owner[idx(x, y, z)] = (byte) o; }
    byte wallBlock() { int k = r.nextInt(100); return k < 14 ? MOSSY : k < 24 ? CRACKED : BRICK; }
    byte vaultBlock() { return r.nextInt(5) == 0 ? VAULT_D : VAULT; }

    /** 圆拱截面：o = 横向偏移，竖墙到 s 高，s 以上半圆 */
    static boolean inArch(double o, int y, double R, int s) { return y >= 0 && (y < s ? Math.abs(o) <= R : o * o + (y - s + 0.5) * (y - s + 0.5) <= R * R); }

    /** 地面层：水渠 |o|≤cw（y0 淤泥、y1~2 水、y3 空），两边走道 y≤2 实心、y3 地面 */
    byte floorCode(int y, boolean channel) {
        if (channel) return y == 0 ? MUD : y <= 2 ? WATER : AIR;
        return y <= 2 ? BRICK : y == 3 ? FLOOR : AIR;
    }

    /** 通道（alongZ=true 时 c 是 x、a 是 z）；穿过房间时只改地面层（开水渠），房间墙壁直接挖开当门 */
    private void corridor(boolean alongZ, int c, int a0, int a1, int R, int s) {
        int cw = R >= 8 ? 3 : 1;
        for (int a = a0; a <= a1; a++) for (int o = -R - 1; o <= R + 1; o++) for (int y = 0; y < H; y++) {
            int x = alongZ ? c + o : a, z = alongZ ? a : c + o;
            boolean in = inArch(o, y, R, s), shell = !in && inArch(o, y, R + 1.3, s);
            if (!in && !shell) continue;
            int ow = own(x, y, z);
            if (shell) { if (get(x, y, z) == VOID) set(x, y, z, y < s ? wallBlock() : vaultBlock()); continue; }
            if (ow == 1 && y > 3) continue;                               // 房间里面的空间留给房间
            byte b = floorCode(y, Math.abs(o) <= cw);
            if (y > 3 && ((a - a0) % 8 == 0) && !inArch(o, y, R - 1, s) && ow != 1) b = RIB;   // 拱肋
            set(x, y, z, b);
        }
        // 细节：拱顶吊灯、通长铜管、栅栏、蜘蛛网、杂物、水渠格栅
        int top = s + R - 1;
        for (int a = a0 + 3; a <= a1 - 3; a++) {
            int k = a - a0;
            if (k % 9 == 4) { put(alongZ, c, a, 0, top, CHAIN); put(alongZ, c, a, 0, top - 1, CHAIN); put(alongZ, c, a, 0, top - 2, LANTERN); }
            for (int side : new int[]{-R, R}) if (k % 8 != 0) put(alongZ, c, a, side, 5, PIPE);
            if (r.nextInt(11) == 0) put(alongZ, c, a, r.nextBoolean() ? R : -R, 7 + r.nextInt(2), BARS);
            if (r.nextInt(16) == 0) { int o = (r.nextBoolean() ? 1 : -1) * (R / 2); put(alongZ, c, a, o, top - 1, COBWEB); }
            if (r.nextInt(24) == 0) put(alongZ, c, a, (r.nextBoolean() ? 1 : -1) * (R - 1), 4, r.nextInt(3) == 0 ? BARREL : PIZZA_BOX);
            if (r.nextInt(30) == 0) put(alongZ, c, a, 0, 0, GRATE);
        }
    }
    /** 只往空气里放细节（不盖墙） */
    private void put(boolean alongZ, int c, int a, int o, int y, byte b) {
        int x = alongZ ? c + o : a, z = alongZ ? a : c + o;
        byte cur = get(x, y, z);
        if (own(x, y, z) == 1) return;                                   // 通道的装饰不伸进房间里
        if (b == GRATE ? cur == MUD : cur == AIR) set(x, y, z, b);
    }

    /** 穹顶圆厅：圆形平面、竖墙到 s、上面半球；一圈柱子；kind 1 = 入口房 */
    private void rotunda(int cx, int cz, int R, int s, int kind) {
        for (int x = cx - R - 2; x <= cx + R + 2; x++) for (int z = cz - R - 2; z <= cz + R + 2; z++) for (int y = 0; y < H; y++) {
            double d = Math.hypot(x - cx, z - cz);
            boolean in = inArch(d, y, R, s), shell = !in && inArch(d, y, R + 1.3, s);
            if (in) { set(x, y, z, floorCode(y, false)); own(x, y, z, 1); }
            else if (shell) { set(x, y, z, y < s ? wallBlock() : vaultBlock()); own(x, y, z, 2); }
        }
        if (kind == 2) {
            for (int i = 0; i < 8; i++) {
                double ang = i * Math.PI / 4 + Math.PI / 8;
                int px = (int) Math.round(cx + Math.cos(ang) * (R - 3)), pz = (int) Math.round(cz + Math.sin(ang) * (R - 3));
                for (int y = 0; y <= s + 4; y++) set(px, y, pz, PILLAR);
                set(px, s + 5, pz, CANDLE);
            }
            set(cx, s + R - 1, cz, CHAIN); set(cx, s + R - 2, cz, CHAIN); set(cx, s + R - 3, cz, CHAIN); set(cx, s + R - 4, cz, LANTERN);
        } else for (int sx : new int[]{-1, 1}) { set(cx + sx * (R - 1), s + 1, cz, LANTERN); }
        decorate(cx - R + 2, cx + R - 2, cz - R + 2, cz + R - 2, R);
    }

    /** 筒拱房间：alongZ 时 c 是 x，沿 z 从 a0 到 a1；两端是平墙；kind 3 = 鼠头头大厅，4 = 支路尽头小房间 */
    private void vaultRoom(boolean alongZ, int c, int a0, int a1, int R, int s, int kind) {
        for (int a = a0 - 1; a <= a1 + 1; a++) for (int o = -R - 2; o <= R + 2; o++) for (int y = 0; y < H; y++) {
            int x = alongZ ? c + o : a, z = alongZ ? a : c + o;
            boolean endWall = a == a0 - 1 || a == a1 + 1;
            boolean in = !endWall && inArch(o, y, R, s), shell = (endWall && inArch(o, y, R + 1.3, s)) || (!in && inArch(o, y, R + 1.3, s));
            if (in) {
                byte b = floorCode(y, false);
                if (y > 3 && (a - a0) % 6 == 0 && !inArch(o, y, R - 1, s)) b = RIB;
                set(x, y, z, b); own(x, y, z, 1);
            } else if (shell) { set(x, y, z, y < s ? wallBlock() : vaultBlock()); own(x, y, z, 2); }
        }
        if (kind == 3) {
            // 两排柱子 + 拱顶一排吊灯
            for (int a = a0 + 4; a <= a1 - 4; a += 8) for (int side : new int[]{-1, 1}) {
                int o = side * (R - 4);
                int x = alongZ ? c + o : a, z = alongZ ? a : c + o;
                for (int y = 0; y <= s + 5; y++) set(x, y, z, PILLAR);
                set(x, s + 6, z, CANDLE);
            }
            for (int a = a0 + 6; a <= a1 - 4; a += 10) {
                int x = alongZ ? c : a, z = alongZ ? a : c;
                for (int k = 1; k <= 4; k++) set(x, s + R - k, z, CHAIN);
                set(x, s + R - 5, z, LANTERN);
            }
        } else {
            int x = alongZ ? c : (a0 + a1) / 2, z = alongZ ? (a0 + a1) / 2 : c;
            set(x, s + R - 2, z, CHAIN); set(x, s + R - 3, z, LANTERN);
        }
        int x0 = alongZ ? c - R + 2 : a0 + 1, x1 = alongZ ? c + R - 2 : a1 - 1, z0 = alongZ ? a0 + 1 : c - R + 2, z1 = alongZ ? a1 - 1 : c + R - 2;
        decorate(x0, x1, z0, z1, kind == 4 ? R * 3 : R);
    }

    /** 地上的杂物：披萨盒、木桶、蜡烛、史莱姆 */
    private void decorate(int x0, int x1, int z0, int z1, int n) {
        for (int i = 0; i < n; i++) {
            int x = x0 + r.nextInt(Math.max(1, x1 - x0 + 1)), z = z0 + r.nextInt(Math.max(1, z1 - z0 + 1));
            if (get(x, 4, z) != AIR || get(x, 3, z) != FLOOR) continue;
            int k = r.nextInt(10);
            set(x, 4, z, k < 4 ? PIZZA_BOX : k < 7 ? BARREL : k < 9 ? CANDLE : SLIME);
            if (k >= 4 && k < 7 && r.nextInt(3) == 0 && get(x, 5, z) == AIR) set(x, 5, z, BARREL);
        }
    }
}
