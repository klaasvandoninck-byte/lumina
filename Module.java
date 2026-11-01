package com.lumina.module;

import com.lumina.setting.Setting;
import com.lumina.ui.Notifications;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/** Basisklasse voor elke feature. */
public abstract class Module {
	protected static final Minecraft mc = Minecraft.getInstance();

	public final String name;
	public final String description;
	public final Category category;
	public final List<Setting> settings = new ArrayList<>();

	/** Toggle-keybind (instelbaar in Opties > Besturing). Wordt door ModuleManager gezet. */
	public KeyMapping key;

	// Animatiestate voor HUD en ClickGUI
	public boolean expanded;
	public float openAnim;
	public float hudAnim;

	private boolean enabled;

	protected Module(String name, String description, Category category) {
		this.name = name;
		this.description = description;
		this.category = category;
	}

	protected <T extends Setting> T add(T setting) {
		settings.add(setting);
		return setting;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void toggle() {
		setEnabled(!enabled);
	}

	public void setEnabled(boolean value) {
		if (value == enabled) {
			return;
		}
		enabled = value;
		if (value) {
			onEnable();
		} else {
			onDisable();
		}
		Notifications.push(name, value);
	}

	/** Gebruikt bij het laden van de config: zet de state zonder notificatie. */
	public void restoreEnabled(boolean value) {
		if (value == enabled) {
			return;
		}
		enabled = value;
		if (value) {
			onEnable();
		}
	}

	/** Moet null-safe zijn: kan al tijdens het opstarten worden aangeroepen. */
	protected void onEnable() {
	}

	protected void onDisable() {
	}

	/** Elke client tick, alleen als de module aan staat en je in een wereld zit. */
	public void onTick() {
	}
}
