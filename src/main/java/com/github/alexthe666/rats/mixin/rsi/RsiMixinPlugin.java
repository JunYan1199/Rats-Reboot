package com.github.alexthe666.rats.mixin.rsi;

import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/** 只有同时装了 rs_integration 和 goety 才应用老鼠共振快递的 mixin。 */
public class RsiMixinPlugin implements IMixinConfigPlugin {
	private boolean enabled;

	@Override
	public void onLoad(String mixinPackage) {
		LoadingModList mods = LoadingModList.get();
		this.enabled = mods != null && mods.getModFileById("rs_integration") != null && mods.getModFileById("goety") != null;
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		return this.enabled;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}
}
