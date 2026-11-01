package com.lumina.module.impl;

import com.lumina.module.Category;
import com.lumina.module.Module;
import com.lumina.setting.BoolSetting;
import com.lumina.setting.ColorSetting;
import com.lumina.setting.NumSetting;
import net.minecraft.util.Mth;

/** Watermark, FPS/ping, coordinaten en de ArrayList van actieve modules. */
public class HudModule extends Module {
	public final BoolSetting watermark = add(new BoolSetting("Watermark", true));
	public final BoolSetting info = add(new BoolSetting("FPS & Ping", true));
	public final BoolSetting coords = add(new BoolSetting("Coordinates", true));
	public final BoolSetting arrayList = add(new BoolSetting("ArrayList", true));
	public final BoolSetting rainbow = add(new BoolSetting("Rainbow", false));
	public final NumSetting rainbowSpeed = add(new NumSetting("Rainbow Speed", 1.0, 0.2, 4.0, 0.1));
	public final ColorSetting color = add(new ColorSetting("Color", 0.55f, 0.65f));

	public HudModule() {
		super("HUD", "Watermark, info en actieve modules.", Category.VISUALS);
		restoreEnabled(true);
	}

	/** Kleur (0xRRGGBB) op een bepaalde plek in een gradient. offset is ongeveer 0..1. */
	public int colorAt(float offset) {
		if (rainbow.get()) {
			float t = (System.currentTimeMillis() % 6000L) / 6000f * rainbowSpeed.getF();
			float h = (t + offset * 0.5f) % 1f;
			if (h < 0) {
				h += 1f;
			}
			return Mth.hsvToRgb(h, 0.65f, 1f) & 0xFFFFFF;
		}
		return color.rgbShifted(offset * 0.12f);
	}
}
