package com.github.alexthe666.rats.client.events;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.client.render.HeldRatSliceModel;
import com.github.alexthe666.rats.registry.RatVariantRegistry;
import com.github.alexthe666.rats.server.items.RatSliceItem;
import com.github.alexthe666.rats.server.misc.RatVariant;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 第一人称主手拿着鼠片时：玩家手臂从右下方抬起，握住老鼠的脖子，老鼠吊在画面中间、正对玩家。
 * 只接管主手；物品栏、掉落物、第三人称、副手都保持原来的扁鼠片。
 * 坐标来自离线预览（愚者\鼠片手持预览），单位：格，相机空间（x 右、y 上、-z 向前）。
 */
@Mod.EventBusSubscriber(modid = RatsMod.MODID, value = Dist.CLIENT)
public class HeldRatSliceHandEvents {
	/** 拳头（手臂末端）位置 */
	private static final Vector3f FIST = new Vector3f(0.055F, -0.115F, -0.86F);
	/** 手臂方向：肩膀 → 拳头（单位向量） */
	private static final Vector3f ARM_DIR = new Vector3f(-0.4549F, 0.8048F, -0.3849F).normalize();
	/** 老鼠脖子（握点）位置 */
	private static final Vector3f GRIP = new Vector3f(-0.0219F, 0.1094F, -0.9375F);
	private static final float ARM_LENGTH = 12.0F / 16.0F;
	/** 整组（手臂 + 老鼠）往屏幕边缘挪，少挡视野：右手往右下，左手往左下 */
	private static final float SHIFT_X = 0.30F, SHIFT_Y = -0.13F;
	/** 以握点为中心整体缩小 */
	private static final float SCALE = 0.8F;

	@SubscribeEvent
	public static void onRenderHand(RenderHandEvent event) {
		if (event.getHand() != InteractionHand.MAIN_HAND) return;
		ItemStack stack = event.getItemStack();
		if (!(stack.getItem() instanceof RatSliceItem)) return;
		Minecraft mc = Minecraft.getInstance();
		AbstractClientPlayer player = mc.player;
		if (player == null) return;
		boolean right = player.getMainArm() == HumanoidArm.RIGHT;
		float side = right ? 1.0F : -1.0F;
		PoseStack pose = event.getPoseStack();
		MultiBufferSource buffers = event.getMultiBufferSource();
		int light = event.getPackedLight();
		boolean ok = false;
		pose.pushPose();
		try {
			pose.translate(0.0F, -0.6F * event.getEquipProgress(), 0.0F);   // 切换物品时的上下过渡，和原版一致
			pose.translate(SHIFT_X * side, SHIFT_Y, 0.0F);
			pose.translate(GRIP.x * side, GRIP.y, GRIP.z);
			pose.scale(SCALE, SCALE, SCALE);
			pose.translate(-GRIP.x * side, -GRIP.y, -GRIP.z);
			if (!player.isInvisible()) renderArm(pose, buffers, light, player, right, side);
			renderRat(pose, buffers, light, stack, side, event.getPartialTick());
			ok = true;
		} catch (Throwable t) {
			// 出任何问题都退回原版手持渲染，不影响游戏
			RatsMod.LOGGER.debug("Rats: held rat slice render failed", t);
		} finally {
			pose.popPose();
		}
		if (ok) event.setCanceled(true);
	}

	private static void renderArm(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, boolean right, float side) {
		EntityRenderer<? super AbstractClientPlayer> r = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
		if (!(r instanceof PlayerRenderer renderer)) return;
		PlayerModel<AbstractClientPlayer> model = renderer.getModel();
		ModelPart arm = right ? model.rightArm : model.leftArm;
		ModelPart sleeve = right ? model.rightSleeve : model.leftSleeve;
		boolean slim = "slim".equals(player.getModelName());
		// 手臂方块的中心轴（模型像素）：宽臂 x=-1（左臂 +1），细臂 x=-0.5（左臂 +0.5），从 y=-2（肩）到 y=10（手）
		float axisX = (slim ? 0.5F : 1.0F) * -side;

		Vector3f fist = new Vector3f(FIST.x * side, FIST.y, FIST.z);
		Vector3f dir = new Vector3f(ARM_DIR.x * side, ARM_DIR.y, ARM_DIR.z);
		Vector3f shoulder = new Vector3f(fist).sub(new Vector3f(dir).mul(ARM_LENGTH));
		// 局部 +Y（肩→手）对准手臂方向；局部 +X 尽量朝向屏幕右侧（左手镜像）
		Vector3f ry = new Vector3f(dir);
		Vector3f hint = new Vector3f(side, 0, 0);
		Vector3f rx = new Vector3f(hint).sub(new Vector3f(ry).mul(hint.dot(ry))).normalize();
		Vector3f rz = new Vector3f(rx).cross(ry).normalize();
		Quaternionf q = new Quaternionf().setFromNormalized(new Matrix3f(rx.x, rx.y, rx.z, ry.x, ry.y, ry.z, rz.x, rz.y, rz.z));

		pose.pushPose();
		pose.translate(shoulder.x, shoulder.y, shoulder.z);
		pose.mulPose(q);
		pose.translate(-axisX / 16.0F, 2.0F / 16.0F, 0.0F);
		ResourceLocation skin = player.getSkinTextureLocation();
		renderPartAtOrigin(arm, pose, buffers.getBuffer(RenderType.entitySolid(skin)), light);
		renderPartAtOrigin(sleeve, pose, buffers.getBuffer(RenderType.entityTranslucent(skin)), light);
		pose.popPose();
	}

	/** 临时清零部件自身的位置和旋转再渲染，渲染后恢复（不影响玩家模型的其它用途）。 */
	private static void renderPartAtOrigin(ModelPart part, PoseStack pose, com.mojang.blaze3d.vertex.VertexConsumer vc, int light) {
		float x = part.x, y = part.y, z = part.z, xr = part.xRot, yr = part.yRot, zr = part.zRot;
		boolean visible = part.visible;
		part.x = part.y = part.z = 0;
		part.xRot = part.yRot = part.zRot = 0;
		part.visible = true;
		part.render(pose, vc, light, OverlayTexture.NO_OVERLAY);
		part.x = x; part.y = y; part.z = z;
		part.xRot = xr; part.yRot = yr; part.zRot = zr;
		part.visible = visible;
	}

	private static final java.util.Map<ResourceLocation, ResourceLocation> TEXTURES = new java.util.HashMap<>();

	/**
	 * 手持老鼠的贴图：默认就是这只老鼠原本的皮肤（随花色变化）。
	 * 资源包里放了 rats:textures/entity/rat/held/<花色名>.png 时优先用它，方便以后单独做手持皮肤；
	 * 资源重载后会重新检查。
	 */
	public static ResourceLocation texture(ItemStack stack) {
		RatVariant variant = RatSliceItem.variantOrNull(stack);
		if (variant == null) variant = RatVariantRegistry.BLUE.get();
		ResourceLocation base = variant.getTexture();
		return TEXTURES.computeIfAbsent(base, b -> {
			String path = b.getPath();
			String name = path.substring(path.lastIndexOf('/') + 1);
			ResourceLocation held = new ResourceLocation(b.getNamespace(), "textures/entity/rat/held/" + name);
			return Minecraft.getInstance().getResourceManager().getResource(held).isPresent() ? held : b;
		});
	}

	/** 资源重载（F3+T、切换资源包）时调用，重新检测替换贴图。 */
	public static void clearTextures() {
		TEXTURES.clear();
	}

	/** 转鼠鼠时老鼠身体中点架在拳头上的位置（相机空间，右手） */
	private static final Vector3f SPIN_POS = new Vector3f(0.025F, -0.06F, -0.88F);
	/** 转鼠鼠的旋转轴：大致朝向镜头（手心法线），稍微向上倾斜，转起来有立体感 */
	private static final Vector3f SPIN_AXIS = new Vector3f(0.0F, -0.35F, -1.0F).normalize();
	/** 每刻转多少度（约 1.8 圈/秒） */
	private static final float SPIN_SPEED = 32.0F;

	private static void renderRat(PoseStack pose, MultiBufferSource buffers, int light, ItemStack stack, float side, float partialTick) {
		ResourceLocation tex = texture(stack);
		pose.pushPose();
		if (RatSpinController.spinning()) {
			// 像转笔一样：身体中点架在拳头上，头尾轮流甩过去，带一点上下晃动
			float t = RatSpinController.spinTime(partialTick);
			Vector3f c = HeldRatSliceModel.center();
			pose.translate(SPIN_POS.x * side, SPIN_POS.y, SPIN_POS.z);
			pose.scale(1.0F, -1.0F, -1.0F);
			pose.mulPose(new Quaternionf().rotationAxis(t * SPIN_SPEED * side * Mth.DEG_TO_RAD, SPIN_AXIS.x, SPIN_AXIS.y, SPIN_AXIS.z));
			pose.mulPose(Axis.XP.rotationDegrees(Mth.sin(t * 0.35F) * 12.0F));
			pose.scale(0.75F, 0.75F, 0.75F);                      // 转起来扫过的圈小一点，不挡手
			pose.translate(-c.x, -c.y, -c.z);
		} else {
			Vector3f g = HeldRatSliceModel.grip();
			pose.translate(GRIP.x * side, GRIP.y, GRIP.z);
			pose.scale(1.0F, -1.0F, -1.0F);                       // 模型空间（y 向下、+z 远离镜头）→ 相机空间
			pose.mulPose(Axis.YP.rotationDegrees(6.0F * side));  // 身体略朝向画面中央
			pose.mulPose(Axis.XP.rotationDegrees(-4.0F));
			pose.translate(-g.x, -g.y, -g.z);
		}
		HeldRatSliceModel.root().render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(tex)), light, OverlayTexture.NO_OVERLAY);
		pose.popPose();
	}
}
