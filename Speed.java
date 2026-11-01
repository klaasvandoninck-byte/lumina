package com.lumina.module.impl;

import com.lumina.module.Category;
import com.lumina.module.Module;
import com.lumina.setting.BoolSetting;
import com.lumina.setting.NumSetting;
import com.lumina.util.MoveUtil;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public class Speed extends Module {
	private final NumSetting speed = add(new NumSetting("Speed", 0.35, 0.1, 1.5, 0.05));
	private final BoolSetting onlyGround = add(new BoolSetting("Only on ground", false));

	public Speed() {
		super("Speed", "Sneller lopen.", Category.MOVEMENT);
	}

	@Override
	public void onTick() {
		LocalPlayer p = mc.player;
		if (onlyGround.get() && !p.onGround()) {
			return;
		}
		double[] dir = MoveUtil.inputDirection(p.getYRot());
		if (dir[0] == 0 && dir[1] == 0) {
			return;
		}
		Vec3 cur = p.getDeltaMovement();
		double sp = speed.get();
		p.setDeltaMovement(dir[0] * sp, cur.y, dir[1] * sp);
	}
}
