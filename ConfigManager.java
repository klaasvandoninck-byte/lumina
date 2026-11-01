package com.lumina.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lumina.module.Module;
import com.lumina.module.ModuleManager;
import com.lumina.setting.Setting;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/** Slaat module-states en instellingen op in .minecraft/config/lumina.json. */
public final class ConfigManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private ConfigManager() {
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("lumina.json");
	}

	public static void save() {
		JsonObject root = new JsonObject();
		for (Module m : ModuleManager.all()) {
			JsonObject mo = new JsonObject();
			mo.addProperty("enabled", m.isEnabled());
			JsonObject so = new JsonObject();
			for (Setting s : m.settings) {
				so.add(s.name, s.save());
			}
			mo.add("settings", so);
			root.add(m.name, mo);
		}
		try (Writer w = Files.newBufferedWriter(file(), StandardCharsets.UTF_8)) {
			GSON.toJson(root, w);
		} catch (IOException e) {
			System.err.println("[Lumina] Config opslaan mislukt: " + e);
		}
	}

	public static void load() {
		Path path = file();
		if (!Files.exists(path)) {
			return;
		}
		try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			JsonObject root = GSON.fromJson(r, JsonObject.class);
			if (root == null) {
				return;
			}
			for (Module m : ModuleManager.all()) {
				if (!root.has(m.name) || !root.get(m.name).isJsonObject()) {
					continue;
				}
				JsonObject mo = root.getAsJsonObject(m.name);
				if (mo.has("settings") && mo.get("settings").isJsonObject()) {
					JsonObject so = mo.getAsJsonObject("settings");
					for (Setting s : m.settings) {
						JsonElement e = so.get(s.name);
						if (e != null) {
							s.load(e);
						}
					}
				}
				if (mo.has("enabled")) {
					m.restoreEnabled(mo.get("enabled").getAsBoolean());
				}
			}
		} catch (Exception e) {
			System.err.println("[Lumina] Config laden mislukt: " + e);
		}
	}
}
