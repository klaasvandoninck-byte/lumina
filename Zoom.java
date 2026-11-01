package com.lumina.module.impl;

import com.lumina.module.Category;
import com.lumina.module.Module;
import com.lumina.setting.NumSetting;
import net.minecraft.client.Minecraft;

/** Zoom door tijdelijk de FOV-optie te verlagen. Zonder mixin, dus robuust. */
public class Zoom extends Module {
	private static int savedFov = -1;

	private final NumSetting zoomFov = add(new NumSetting("Zoom FOV", 30, 30, 60, 1));

	public Zoom() {
		super("Zoom", "Inzoomen (zet aan om te zoomen, uit om terug te gaan).", Category.VISUALS);
	}

	@Override
	protected void onEnable() {
		if (mc.options != null && savedFov < 0) {
			savedFov = mc.options.fov().get();
		}
	}

	@Override
	public void onTick() {
		if (savedFov < 0) {
			savedFov = mc.options.fov().get();
		}
		mc.options.fov().set(zoomFov.getI());
	}

	@Override
	protected void onDisable() {
		restore();
	}

	/** Zet de originele FOV terug. Ook aangeroepen bij afsluiten van het spel. */
	public static void restore() {
		Minecraft mcInstance = Minecraft.getInstance();
		if (savedFov >= 0 && mcInstance.options != null) {
			mcInstance.options.fov().set(savedFov);
		}
		savedFov = -1;
	}

	/** Zoom mag nooit automatisch aan staan na het opstarten. */
	@Override
	public void restoreEnabled(boolean value) {
	}
}
