package com.lumina.module.impl;

import com.lumina.module.Category;
import com.lumina.module.Module;
import com.lumina.setting.NumSetting;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Houd de aanvalsknop ingedrukt om automatisch te slaan. */
public class AutoClicker extends Module {
	private final NumSetting cps = add(new NumSetting("CPS", 10, 1, 20, 1));
	private long nextClick;

	public AutoClicker() {
		super("AutoClicker", "Slaat automatisch terwijl je de aanvalsknop vasthoudt.", Category.COMBAT);
	}

	@Override
	public void onTick() {
		if (mc.screen != null || mc.gameMode == null || !mc.options.keyAttack.isDown()) {
			return;
		}
		long now = System.currentTimeMillis();
		if (now < nextClick) {
			return;
		}
		HitResult hit = mc.hitResult;
		if (hit instanceof EntityHitResult entityHit) {
			mc.gameMode.attack(mc.player, entityHit.getEntity());
			mc.player.swing(InteractionHand.MAIN_HAND);
			double delay = 1000.0 / cps.get();
			nextClick = now + (long) (delay * ThreadLocalRandom.current().nextDouble(0.85, 1.15));
		}
	}
}
