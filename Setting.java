package com.lumina.setting;

import com.google.gson.JsonElement;

/** Basisklasse voor alle instellingen van een module. */
public abstract class Setting {
	public final String name;

	/** Verborgen instellingen staan wel in de config, maar niet in de ClickGUI. */
	public boolean hidden;

	protected Setting(String name) {
		this.name = name;
	}

	public abstract JsonElement save();

	public abstract void load(JsonElement element);
}
