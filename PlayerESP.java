package com.lumina.module.impl;

import com.lumina.module.Category;
import com.lumina.module.Module;
import com.lumina.setting.ColorSetting;
import com.lumina.setting.ModeSetting;
import com.lumina.setting.NumSetting;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.effect.MobEffects;

/**
 * Zie spelers door muren heen. Modes:
 * Glow = ingebouwde glow-contour van Minecraft, Box = gevulde doorzichtige box,
 * Outline = alleen de randen van de box. Box en Outline worden getekend in EspRenderer.
 */
public class PlayerESP extends Module {
	public final ModeSetting mode = add(new ModeSetting("Mode", "Box", "Glow", "Box", "Outline"));
	public final NumSetting range = add(new NumSetting("Range", 128, 16, 256, 8));
	public final ColorSetting color = add(new ColorSetting("Color", 0.0f, 0.8f));
	public final NumSetting alpha = add(new NumSetting("Opacity", 0.35, 0.05, 1.0, 0.05));
	public final NumSetting thickness = add(new NumSetting("Line Width", 0.02, 0.01, 0.06, 0.01));

	public PlayerESP() {
		super("PlayerESP", "Zie spelers door muren heen.", Category.VISUALS);
	}

	@Override
	public void onTick() {
		boolean glow = mode.is("Glow");
		double max = range.get();
		for (AbstractClientPlayer p : mc.level.players()) {
			if (p == mc.player) {
				continue;
			}
			p.setGlowingTag(glow ? mc.player.distanceTo(p) <= max : p.hasEffect(MobEffects.GLOWING));
		}
	}

	@Override
	protected void onDisable() {
		if (mc.level == null) {
			return;
		}
		for (AbstractClientPlayer p : mc.level.players()) {
			p.setGlowingTag(p.hasEffect(MobEffects.GLOWING));
		}
	}
}
