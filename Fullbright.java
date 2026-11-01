package com.lumina.module.impl;

import com.lumina.module.Category;
import com.lumina.module.Module;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Client-side night vision. Verandert niets op de server. */
public class Fullbright extends Module {
	public Fullbright() {
		super("Fullbright", "Alles helder zien, ook in het donker.", Category.VISUALS);
	}

	@Override
	public void onTick() {
		// Lange duur zodat het effect niet gaat knipperen.
		mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0, false, false, false));
	}

	@Override
	protected void onDisable() {
		if (mc.player != null) {
			mc.player.removeEffect(MobEffects.NIGHT_VISION);
		}
	}
}
