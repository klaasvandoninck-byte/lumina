package com.lumina.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class NumSetting extends Setting {
	public final double min;
	public final double max;
	public final double step;
	private double value;

	public NumSetting(String name, double def, double min, double max, double step) {
		super(name);
		this.min = min;
		this.max = max;
		this.step = step;
		this.value = def;
	}

	public double get() {
		return value;
	}

	public float getF() {
		return (float) value;
	}

	public int getI() {
		return (int) Math.round(value);
	}

	public void set(double v) {
		if (step > 0) {
			v = Math.round(v / step) * step;
		}
		value = Math.max(min, Math.min(max, v));
	}

	/** Waarde als 0..1 voor sliders. */
	public double progress() {
		return (value - min) / (max - min);
	}

	public String display() {
		if (step >= 1) {
			return String.valueOf((int) Math.round(value));
		}
		return String.format(java.util.Locale.ROOT, "%.2f", value);
	}

	@Override
	public JsonElement save() {
		return new JsonPrimitive(value);
	}

	@Override
	public void load(JsonElement element) {
		try {
			set(element.getAsDouble());
		} catch (Exception ignored) {
		}
	}
}
