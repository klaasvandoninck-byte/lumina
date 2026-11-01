package com.lumina.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.util.Mth;

/** Kleur via een tint-slider (hue). Handig en compact in de ClickGUI. */
public class ColorSetting extends Setting {
	private float hue;
	private final float saturation;

	public ColorSetting(String name, float hue, float saturation) {
		super(name);
		this.hue = hue;
		this.saturation = saturation;
	}

	public float hue() {
		return hue;
	}

	public void setHue(float hue) {
		this.hue = Math.max(0f, Math.min(1f, hue));
	}

	/** RGB zonder alpha (0xRRGGBB). */
	public int rgb() {
		return Mth.hsvToRgb(hue, saturation, 1f) & 0xFFFFFF;
	}

	public int rgbShifted(float offset) {
		float h = (hue + offset) % 1f;
		if (h < 0) {
			h += 1f;
		}
		return Mth.hsvToRgb(h, saturation, 1f) & 0xFFFFFF;
	}

	@Override
	public JsonElement save() {
		return new JsonPrimitive(hue);
	}

	@Override
	public void load(JsonElement element) {
		try {
			setHue(element.getAsFloat());
		} catch (Exception ignored) {
		}
	}
}
