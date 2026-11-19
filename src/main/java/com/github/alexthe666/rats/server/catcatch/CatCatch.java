package com.github.alexthe666.rats.server.catcatch;

import com.github.alexthe666.rats.registry.RatsEntityRegistry;
import com.github.alexthe666.rats.registry.RatsSoundRegistry;
import com.github.alexthe666.rats.server.entity.rat.Rat;
import com.github.alexthe666.rats.server.items.RatSliceItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.ItemStack;

/**
 * 猫捕鼠的公共规则。
 * <ul>
 *     <li>只捕获普通成年野生老鼠（实体类型严格为 rats:rat）。</li>
 *     <li>排除：幼鼠、已驯服（tamed_rat 是另一种实体，天然排除）、有主人的、瘟疫鼠、正在变鼠王的、披托加袍的鼠西洲鼠、
 *     鼠王/黑死病召唤的随从、恶魔鼠/鼠西洲生物/Boss（类型不同，天然排除）、刚从鼠片释放的保护期老鼠。</li>
 *     <li>扑咬成功造成伤害后立即活捉成鼠片放进挎包；伤害被限制为不致死，所以不会触发死亡掉落。</li>
 * </ul>
 */
public final class CatCatch {
	/** 搜索半径（格）。 */
	public static final double SEARCH_RADIUS = 16.0D;
	/** 鼠片释放后的保护期（tick）：180 秒，与缓慢 X 同步。 */
	public static final int RELEASE_PROTECT_TICKS = 180 * 20;
	public static final String PROTECT_KEY = "rats_cat_release_protect_until";

	private CatCatch() {
	}

	public static boolean isCatchable(Entity e) {
		if (!(e instanceof Rat rat) || e.getType() != RatsEntityRegistry.RAT.get()) return false;
		if (!rat.isAlive() || rat.isRemoved() || rat.isBaby()) return false;
		if (rat.isTame() || rat.getOwnerUUID() != null) return false;
		if (rat.hasPlague() || rat.isBecomingRatKing() || rat.hasToga()) return false;
		if (rat.isPassenger() || rat.isVehicle()) return false;
		return !isProtected(rat);
	}

	public static boolean isProtected(Rat rat) {
		long until = rat.getPersistentData().getLong(PROTECT_KEY);
		return until > 0 && rat.level().getGameTime() < until;
	}

	public static void protect(Rat rat) {
		rat.getPersistentData().putLong(PROTECT_KEY, rat.level().getGameTime() + RELEASE_PROTECT_TICKS);
	}

	/**
	 * 猫的扑咬：只有成功造成伤害才算咬到，然后立即活捉。
	 * 伤害上限是“目标剩余生命 - 1”，保证不会咬死，也就不会产生死亡掉落或复制。
	 *
	 * @return true = 已活捉并放入挎包
	 */
	public static boolean biteAndCapture(Cat cat, Rat rat, CatSatchel satchel) {
		if (!(cat.level() instanceof ServerLevel level)) return false;
		// 没空位绝对不咬：避免伤害目标后鼠片无处可放
		if (satchel.isFull() || !isCatchable(rat)) return false;
		float attack = (float) Math.max(1.0D, cat.getAttributeValue(Attributes.ATTACK_DAMAGE));
		float damage = Math.min(attack, rat.getHealth() - 1.0F);
		boolean hit;
		if (damage > 0.0F) {
			hit = rat.hurt(cat.damageSources().mobAttack(cat), damage);
		} else {
			// 只剩一点血：扑咬仍然算命中，但不再扣血
			hit = rat.invulnerableTime <= 10;
		}
		if (!hit || !rat.isAlive()) return false;
		ItemStack slice = RatSliceItem.fromRat(rat);
		if (!satchel.insert(slice)) return false;
		cat.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
		level.sendParticles(ParticleTypes.POOF, rat.getX(), rat.getY() + 0.2D, rat.getZ(), 6, 0.15D, 0.1D, 0.15D, 0.01D);
		level.playSound(null, rat.blockPosition(), RatsSoundRegistry.RAT_HURT.get(), SoundSource.NEUTRAL, 0.8F, 1.4F);
		level.playSound(null, cat.blockPosition(), SoundEvents.CAT_PURR, SoundSource.NEUTRAL, 0.8F, 1.0F);
		// 活捉：直接移除实体，不走死亡流程，不掉落任何东西
		rat.stopRiding();
		rat.ejectPassengers();
		rat.discard();
		return true;
	}
}
