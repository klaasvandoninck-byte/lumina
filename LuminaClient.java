package com.lumina;

import com.lumina.config.ConfigManager;
import com.lumina.module.ModuleManager;
import com.lumina.render.HudRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;

public class LuminaClient implements ClientModInitializer {
	public static final String MOD_ID = "lumina";

	@Override
	public void onInitializeClient() {
		ModuleManager.init();
		ConfigManager.load();

		com.lumina.render.EspRenderer.init();
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "hud"), HudRenderer::render);
		ClientTickEvents.END_CLIENT_TICK.register(ModuleManager::tick);
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
			com.lumina.module.impl.Zoom.restore();
			com.lumina.spotify.SpotifyClient.stop();
			com.lumina.render.EspRenderer.close();
			ConfigManager.save();
		});
	}
}
