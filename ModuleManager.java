package com.lumina.module;

import com.lumina.module.impl.AutoClicker;
import com.lumina.module.impl.Fly;
import com.lumina.module.impl.Fullbright;
import com.lumina.module.impl.HudModule;
import com.lumina.module.impl.NoFall;
import com.lumina.module.impl.PlayerESP;
import com.lumina.module.impl.Zoom;
import com.lumina.module.impl.Speed;
import com.lumina.module.impl.SpotifyHud;
import com.lumina.module.impl.Sprint;
import com.lumina.ui.ClickGuiScreen;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class ModuleManager {
	private static final List<Module> MODULES = new ArrayList<>();
	private static HudModule hud;
	private static PlayerESP esp;
	private static SpotifyHud spotify;
	private static KeyMapping openGui;

	private ModuleManager() {
	}

	public static void init() {
		KeyMapping.Category keyCategory = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("lumina", "main"));

		openGui = KeyBindingHelper.registerKeyBinding(
			new KeyMapping("key.lumina.clickgui", com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_RIGHT_SHIFT, keyCategory));

		hud = new HudModule();
		esp = new PlayerESP();
		spotify = new SpotifyHud();

		SpotifyHud.playKey = simpleKey("key.lumina.spotify_playpause", keyCategory);
		SpotifyHud.nextKey = simpleKey("key.lumina.spotify_next", keyCategory);
		SpotifyHud.prevKey = simpleKey("key.lumina.spotify_previous", keyCategory);
		SpotifyHud.loginKey = simpleKey("key.lumina.spotify_login", keyCategory);

		MODULES.add(hud);
		MODULES.add(spotify);
		MODULES.add(new Fullbright());
		MODULES.add(esp);
		MODULES.add(new Zoom());
		MODULES.add(new AutoClicker());
		MODULES.add(new Sprint());
		MODULES.add(new Fly());
		MODULES.add(new Speed());
		MODULES.add(new NoFall());

		// Elke module krijgt een toggle-keybind (standaard niet gebonden).
		for (Module m : MODULES) {
			m.key = KeyBindingHelper.registerKeyBinding(
				new KeyMapping("key.lumina.toggle_" + m.name.toLowerCase(Locale.ROOT),
					com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, keyCategory));
		}
	}

	private static KeyMapping simpleKey(String translationKey, KeyMapping.Category category) {
		return KeyBindingHelper.registerKeyBinding(new KeyMapping(translationKey,
			com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, category));
	}

	public static PlayerESP esp() {
		return esp;
	}

	public static SpotifyHud spotify() {
		return spotify;
	}

	public static void tick(Minecraft mc) {
		boolean inWorld = mc.player != null && mc.level != null;

		while (openGui.consumeClick()) {
			if (inWorld && mc.screen == null) {
				mc.setScreen(new ClickGuiScreen());
			}
		}

		for (Module m : MODULES) {
			while (m.key.consumeClick()) {
				if (inWorld && mc.screen == null) {
					m.toggle();
				}
			}
			if (inWorld && m.isEnabled()) {
				m.onTick();
			}
		}
	}

	public static List<Module> all() {
		return MODULES;
	}

	public static List<Module> in(Category category) {
		List<Module> list = new ArrayList<>();
		for (Module m : MODULES) {
			if (m.category == category) {
				list.add(m);
			}
		}
		return list;
	}

	public static HudModule hud() {
		return hud;
	}

	/** Accentkleur (0xRRGGBB) van het hele client. */
	public static int accent() {
		return hud.colorAt(0f);
	}
}
