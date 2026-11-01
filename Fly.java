package com.lumina.module.impl;

import com.lumina.module.Category;
import com.lumina.module.Module;
import com.lumina.setting.NumSetting;
import com.lumina.util.MoveUtil;
import net.minecraft.client.player.LocalPlayer;

public class Fly extends Module {
	private final NumSetting speed = add(new NumSetting("Speed", 1.0, 0.1, 5.0, 0.1));

	public Fly() {
		super("Fly", "Vrij vliegen met spatie en shift.", Category.MOVEMENT);
	}

	@Override
	public void onTick() {
		LocalPlayer p = mc.player;
		double sp = speed.get() * 0.5;
		double[] dir = MoveUtil.inputDirection(p.getYRot());
		double dy = 0;
		if (mc.options.keyJump.isDown()) {
			dy += sp;
		}
		if (mc.options.keyShift.isDown()) {
			dy -= sp;
		}
		p.setDeltaMovement(dir[0] * sp, dy, dir[1] * sp);
	}
}
