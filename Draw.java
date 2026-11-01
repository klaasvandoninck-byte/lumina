package com.lumina.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Kleine tekenhelpers bovenop GuiGraphics (alleen fill + drawString). */
public final class Draw {
	private Draw() {
	}

	/** Combineert alpha (0..255) met een 0xRRGGBB kleur tot ARGB. */
	public static int argb(int alpha, int rgb) {
		return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
	}

	/** Afgerond rechthoekje, opgebouwd uit fills. Geen overlap, dus alpha-kleuren blijven schoon. */
	public static void roundedRect(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
		r = Math.min(r, Math.min(w, h) / 2);
		if (r <= 0) {
			g.fill(x, y, x + w, y + h, color);
			return;
		}
		g.fill(x + r, y, x + w - r, y + h, color);
		g.fill(x, y + r, x + r, y + h - r, color);
		g.fill(x + w - r, y + r, x + w, y + h - r, color);
		for (int i = 0; i < r; i++) {
			double dy = r - i - 0.5;
			int inset = r - (int) Math.round(Math.sqrt(r * r - dy * dy));
			g.fill(x + inset, y + i, x + r, y + i + 1, color);
			g.fill(x + w - r, y + i, x + w - inset, y + i + 1, color);
			g.fill(x + inset, y + h - i - 1, x + r, y + h - i, color);
			g.fill(x + w - r, y + h - i - 1, x + w - inset, y + h - i, color);
		}
	}

	public static void text(GuiGraphics g, String s, int x, int y, int rgb) {
		g.drawString(Minecraft.getInstance().font, s, x, y, argb(255, rgb), true);
	}

	public static int width(String s) {
		return Minecraft.getInstance().font.width(s);
	}
}
