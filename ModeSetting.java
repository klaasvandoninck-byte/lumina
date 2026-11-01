package com.lumina.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class ModeSetting extends Setting {
	public final String[] options;
	private int index;

	public ModeSetting(String name, String def, String... options) {
		super(name);
		this.options = options;
		for (int i = 0; i < options.length; i++) {
			if (options[i].equalsIgnoreCase(def)) {
				index = i;
			}
		}
	}

	public String get() {
		return options[index];
	}

	public boolean is(String option) {
		return options[index].equalsIgnoreCase(option);
	}

	public void cycle(int dir) {
		index = Math.floorMod(index + dir, options.length);
	}

	@Override
	public JsonElement save() {
		return new JsonPrimitive(options[index]);
	}

	@Override
	public void load(JsonElement element) {
		try {
			String s = element.getAsString();
			for (int i = 0; i < options.length; i++) {
				if (options[i].equalsIgnoreCase(s)) {
					index = i;
				}
			}
		} catch (Exception ignored) {
		}
	}
}
