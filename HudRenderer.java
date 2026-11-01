package com.lumina.render;

import com.lumina.module.Module;
import com.lumina.module.ModuleManager;
import com.lumina.module.impl.HudModule;
import com.lumina.ui.Notifications;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;

/** Tekent de eigen HUD. Wordt geregistreerd via Fabric's HudElementRegistry. */
public final class HudRenderer {
	private static final String VERSION = "0.1";

	private HudRenderer() {
	}

	public static void render(GuiGraphics g, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.options.hideGui) {
			return;
		}

		HudModule hud = ModuleManager.hud();
		if (hud.isEnabled()) {
			if (hud.watermark.get() || hud.info.get()) {
				drawTopLeft(g, mc, hud);
			}
			if (hud.coords.get()) {
				drawCoords(g, mc, hud);
			}
			if (hud.arrayList.get()) {
				drawArrayList(g, mc, hud);
			}
		}

		if (ModuleManager.spotify().isEnabled()) {
			ModuleManager.spotify().render(g);
		}

		Notifications.render(g);
	}

	private static void drawTopLeft(GuiGraphics g, Minecraft mc, HudModule hud) {
		String title = "Lumina";
		String version = " " + VERSION;
		String info = null;
		if (hud.info.get()) {
			info = "FPS " + mc.getFps() + "  |  Ping " + ping(mc) + "ms";
		}

		int titleW = hud.watermark.get() ? Draw.width(title + version) : 0;
		int infoW = info != null ? Draw.width(info) : 0;
		int w = Math.max(titleW, infoW) + 12;
		int lines = (hud.watermark.get() ? 1 : 0) + (info != null ? 1 : 0);
		int h = 6 + lines * 11;

		Draw.roundedRect(g, 6, 6, w, h, 4, Draw.argb(170, 0x101018));
		g.fill(6, 9, 8, 6 + h - 3, Draw.argb(255, hud.colorAt(0f)));

		int y = 10;
		if (hud.watermark.get()) {
			int x = 12;
			for (int i = 0; i < title.length(); i++) {
				String c = String.valueOf(title.charAt(i));
				Draw.text(g, c, x, y, hud.colorAt(i / (float) title.length()));
				x += Draw.width(c);
			}
			Draw.text(g, version, x, y, 0xAAAAAA);
			y += 11;
		}
		if (info != null) {
			Draw.text(g, info, 12, y, 0xE0E0E0);
		}
	}

	private static void drawCoords(GuiGraphics g, Minecraft mc, HudModule hud) {
		String s = String.format(java.util.Locale.ROOT, "XYZ  %.1f  %.1f  %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
		int w = Draw.width(s) + 12;
		int y = mc.getWindow().getGuiScaledHeight() - 22;
		Draw.roundedRect(g, 6, y, w, 16, 4, Draw.argb(170, 0x101018));
		g.fill(6, y + 3, 8, y + 13, Draw.argb(255, hud.colorAt(0.5f)));
		Draw.text(g, s, 12, y + 4, 0xE0E0E0);
	}

	private static void drawArrayList(GuiGraphics g, Minecraft mc, HudModule hud) {
		List<Module> visible = new ArrayList<>();
		for (Module m : ModuleManager.all()) {
			m.hudAnim += ((m.isEnabled() ? 1f : 0f) - m.hudAnim) * 0.2f;
			if (m.hudAnim < 0.02f) {
				m.hudAnim = 0f;
			}
			if (m != hud && m.hudAnim > 0f) {
				visible.add(m);
			}
		}
		visible.sort(Comparator.comparingInt((Module m) -> Draw.width(m.name)).reversed());

		int sw = mc.getWindow().getGuiScaledWidth();
		float y = 6;
		int index = 0;
		for (Module m : visible) {
			int w = Draw.width(m.name) + 10;
			int h = 12;
			int x = sw - 4 - Math.round(w * m.hudAnim);
			int yy = Math.round(y);
			int color = hud.colorAt(index * 0.08f);

			g.fill(x, yy, sw - 4, yy + h, Draw.argb(Math.round(150 * m.hudAnim), 0x101018));
			g.fill(sw - 6, yy, sw - 4, yy + h, Draw.argb(255, color));
			Draw.text(g, m.name, x + 4, yy + 2, color);

			y += h * m.hudAnim;
			index++;
		}
	}

	private static int ping(Minecraft mc) {
		if (mc.getConnection() == null) {
			return 0;
		}
		PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
		return info == null ? 0 : info.getLatency();
	}
}
