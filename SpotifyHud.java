package com.lumina.module.impl;

import com.lumina.module.Category;
import com.lumina.module.Module;
import com.lumina.module.ModuleManager;
import com.lumina.render.Draw;
import com.lumina.setting.BoolSetting;
import com.lumina.setting.NumSetting;
import com.lumina.spotify.SpotifyClient;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.Mth;

/** "Now playing"-kaartje met cover, titel, artiest en voortgangsbalk. */
public class SpotifyHud extends Module {
	public static final int W = 176;
	public static final int H = 52;

	public static KeyMapping playKey;
	public static KeyMapping nextKey;
	public static KeyMapping prevKey;
	public static KeyMapping loginKey;

	private final BoolSetting showCover = add(new BoolSetting("Show Cover", true));
	private final BoolSetting showProgress = add(new BoolSetting("Show Progress", true));
	private final NumSetting posX = add(new NumSetting("posX", 6, 0, 8000, 1));
	private final NumSetting posY = add(new NumSetting("posY", 44, 0, 8000, 1));

	public SpotifyHud() {
		super("Spotify", "Toont wat je op Spotify luistert. Sleep het kaartje in de ClickGUI.", Category.VISUALS);
		posX.hidden = true;
		posY.hidden = true;
	}

	@Override
	protected void onEnable() {
		SpotifyClient.start();
	}

	@Override
	protected void onDisable() {
		SpotifyClient.stop();
	}

	@Override
	public void onTick() {
		while (playKey.consumeClick()) {
			SpotifyClient.playPause();
		}
		while (nextKey.consumeClick()) {
			SpotifyClient.next();
		}
		while (prevKey.consumeClick()) {
			SpotifyClient.previous();
		}
		while (loginKey.consumeClick()) {
			SpotifyClient.login();
		}
	}

	public int x() {
		int sw = mc.getWindow().getGuiScaledWidth();
		return Mth.clamp(posX.getI(), 0, Math.max(0, sw - W));
	}

	public int y() {
		int sh = mc.getWindow().getGuiScaledHeight();
		return Mth.clamp(posY.getI(), 0, Math.max(0, sh - H));
	}

	public boolean contains(double mx, double my) {
		return mx >= x() && mx < x() + W && my >= y() && my < y() + H;
	}

	public void move(double dx, double dy) {
		posX.set(x() + dx);
		posY.set(y() + dy);
	}

	public void render(GuiGraphics g) {
		int x = x();
		int y = y();
		int accent = ModuleManager.accent();

		Draw.roundedRect(g, x, y, W, H, 5, Draw.argb(190, 0x101018));
		g.fill(x + 1, y + 6, x + 3, y + H - 6, Draw.argb(255, accent));

		SpotifyClient.Track t = SpotifyClient.track;
		if (t == null) {
			Draw.text(g, "Spotify", x + 10, y + 8, accent);
			String status = SpotifyClient.status;
			Draw.text(g, trim(status, W - 20), x + 10, y + 22, 0xC8C8C8);
			drawNotice(g, x, y);
			return;
		}

		int textX = x + 10;
		if (showCover.get()) {
			int cs = 40;
			int cx = x + 8;
			int cy = y + 6;
			if (SpotifyClient.coverReady && SpotifyClient.coverW > 0) {
				g.blit(RenderPipelines.GUI_TEXTURED, SpotifyClient.COVER_ID, cx, cy, 0f, 0f, cs, cs,
					SpotifyClient.coverW, SpotifyClient.coverH, SpotifyClient.coverW, SpotifyClient.coverH);
			} else {
				g.fill(cx, cy, cx + cs, cy + cs, Draw.argb(255, 0x2A2A36));
			}
			textX = cx + cs + 6;
		}

		int textW = x + W - 6 - textX;
		Draw.text(g, trim(t.title(), textW), textX, y + 8, 0xFFFFFF);
		Draw.text(g, trim(t.artist(), textW), textX, y + 20, 0xA8A8B4);

		if (showProgress.get() && t.durationMs() > 0) {
			long prog = t.progressMs() + (t.playing() ? System.currentTimeMillis() - t.fetchedAt() : 0);
			prog = Math.max(0, Math.min(t.durationMs(), prog));

			int bx = textX;
			int bw = textW;
			int by = y + 36;
			g.fill(bx, by, bx + bw, by + 3, Draw.argb(255, 0x2A2A36));
			g.fill(bx, by, bx + Math.round(bw * (prog / (float) t.durationMs())), by + 3, Draw.argb(255, accent));

			String time = fmt(prog) + " / " + fmt(t.durationMs());
			Draw.text(g, time, bx, by + 6, 0x8C8C98);
		}
		drawNotice(g, x, y);
	}

	private void drawNotice(GuiGraphics g, int x, int y) {
		if (System.currentTimeMillis() < SpotifyClient.noticeUntil) {
			Draw.roundedRect(g, x, y + H + 3, W, 14, 4, Draw.argb(210, 0x301018));
			Draw.text(g, trim(SpotifyClient.notice, W - 12), x + 6, y + H + 6, 0xFFB0B8);
		}
	}

	private static String fmt(long ms) {
		long s = ms / 1000;
		return (s / 60) + ":" + String.format(java.util.Locale.ROOT, "%02d", s % 60);
	}

	private static String trim(String s, int maxPx) {
		if (Draw.width(s) <= maxPx) {
			return s;
		}
		String t = s;
		while (t.length() > 1 && Draw.width(t + "..") > maxPx) {
			t = t.substring(0, t.length() - 1);
		}
		return t + "..";
	}
}
