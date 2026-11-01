package com.lumina.module.impl;

import com.lumina.module.Category;
import com.lumina.module.Module;

public class Sprint extends Module {
	public Sprint() {
		super("Sprint", "Automatisch sprinten als je vooruit loopt.", Category.MOVEMENT);
	}

	@Override
	public void onTick() {
		if (mc.options.keyUp.isDown()) {
			mc.options.keySprint.setDown(true);
		}
	}

	@Override
	protected void onDisable() {
		if (mc.options != null) {
			mc.options.keySprint.setDown(false);
		}
	}
}
