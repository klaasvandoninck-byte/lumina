package com.lumina.ui;

import com.lumina.render.Draw;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Toggle-meldingen rechtsonder die in- en uitfaden. */
public final class Notifications {
	private static final long LIFE = 2600L;
	private static final long SLIDE_IN = 250L;
	private static final long FADE_OUT = 350L;

	private record Entry(String name, boolean on, long time) {
	}

	private static final List<Entry> ENTRIES = new ArrayList<>();

	private Notifications() {
	}

	public static void push(String name, boolean on) {
		ENTRIES.add(new Entry(name, on, System.currentTimeMillis()));
		if (ENTRIES.size() > 6) {
			ENTRIES.remove(0);
		}
	}

	public static void render(GuiGraphics g) {
		long now = System.currentTimeMillis();
		ENTRIES.removeIf(e -> now - e.time() > LIFE);

		Minecraft mc = Minecraft.getInstance();
		int sw = mc.getWindow().getGuiScaledWidth();
		int sh = mc.getWindow().getGuiScaledHeight();
		int y = sh - 34;

		for (int i = ENTRIES.size() - 1; i >= 0; i--) {
			Entry e = ENTRIES.get(i);
			long age = now - e.time();

			float slide = Math.min(1f, age / (float) SLIDE_IN);
			slide = 1f - (1f - slide) * (1f - slide);
			float fade = age > LIFE - FADE_OUT ? Math.max(0f, (LIFE - age) / (float) FADE_OUT) : 1f;

			String text = e.name() + (e.on() ? " enabled" : " disabled");
			int w = Draw.width(text) + 18;
			int h = 18;
			int x = sw - 6 - Math.round(w * slide);
			int accent = e.on() ? 0x55FF77 : 0xFF5566;

			Draw.roundedRect(g, x, y, w, h, 4, Draw.argb(Math.round(210 * fade), 0x101018));
			g.fill(x + 2, y + 4, x + 4, y + h - 4, Draw.argb(Math.round(255 * fade), accent));
			g.drawString(mc.font, text, x + 9, y + 5, Draw.argb(Math.round(255 * fade), 0xFFFFFF), true);

			y -= h + 4;
		}
	}
}
