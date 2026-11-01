package com.lumina.ui;

import com.lumina.config.ConfigManager;
import com.lumina.module.Category;
import com.lumina.module.Module;
import com.lumina.module.ModuleManager;
import com.lumina.module.impl.SpotifyHud;
import com.lumina.render.Draw;
import com.lumina.spotify.SpotifyClient;
import com.lumina.setting.BoolSetting;
import com.lumina.setting.ColorSetting;
import com.lumina.setting.ModeSetting;
import com.lumina.setting.NumSetting;
import com.lumina.setting.Setting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/**
 * ClickGUI: een paneel per categorie.
 * Linkermuisknop op een module = aan/uit, rechtermuisknop = instellingen open/dicht.
 * Sleep een titelbalk om een paneel te verplaatsen, rechtsklik op de titelbalk om in te klappen.
 */
public class ClickGuiScreen extends Screen {
	private static final int W = 114;
	private static final int HEAD = 18;
	private static final int ROW = 15;

	private static final int TYPE_HEADER = 0;
	private static final int TYPE_MODULE = 1;
	private static final int TYPE_SETTING = 2;

	private static final class Panel {
		final Category category;
		int x;
		int y = 22;
		boolean collapsed;

		Panel(Category category, int x) {
			this.category = category;
			this.x = x;
		}
	}

	/** Klikbaar gebied, elke frame opnieuw opgebouwd tijdens het tekenen. */
	private record Row(int type, int x, int y, int w, int h, Module module, Setting setting, Panel panel) {
		boolean contains(double mx, double my) {
			return mx >= x && mx < x + w && my >= y && my < y + h;
		}
	}

	private static final List<Panel> PANELS = new ArrayList<>();

	static {
		int x = 20;
		for (Category c : Category.values()) {
			PANELS.add(new Panel(c, x));
			x += W + 10;
		}
	}

	private final List<Row> rows = new ArrayList<>();
	private Panel draggingPanel;
	private Row dragRow;
	private boolean mouseDown;
	private boolean draggingSpotify;

	public ClickGuiScreen() {
		super(Component.literal("Lumina"));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.render(g, mouseX, mouseY, delta);
		rows.clear();

		int accent = ModuleManager.accent();

		for (Panel p : PANELS) {
			Draw.roundedRect(g, p.x, p.y, W, HEAD, 4, Draw.argb(235, 0x101018));
			Draw.text(g, p.category.title, p.x + 7, p.y + 5, accent);
			Draw.text(g, p.collapsed ? "+" : "-", p.x + W - 12, p.y + 5, 0xAAAAAA);
			rows.add(new Row(TYPE_HEADER, p.x, p.y, W, HEAD, null, null, p));

			if (p.collapsed) {
				continue;
			}

			int cy = p.y + HEAD + 2;
			for (Module m : ModuleManager.in(p.category)) {
				boolean hover = mouseX >= p.x && mouseX < p.x + W && mouseY >= cy && mouseY < cy + ROW;
				int bg = m.isEnabled()
					? Draw.argb(hover ? 200 : 170, accent)
					: Draw.argb(hover ? 215 : 190, hover ? 0x242430 : 0x15151D);
				g.fill(p.x, cy, p.x + W, cy + ROW, bg);
				Draw.text(g, m.name, p.x + 7, cy + 4, m.isEnabled() ? 0xFFFFFF : 0xB0B0B0);
				if (!m.settings.isEmpty()) {
					Draw.text(g, m.expanded ? "v" : ">", p.x + W - 11, cy + 4, 0x888888);
				}
				rows.add(new Row(TYPE_MODULE, p.x, cy, W, ROW, m, null, p));
				cy += ROW;

				// Animatie van het uitklappen
				float target = m.expanded ? 1f : 0f;
				m.openAnim += (target - m.openAnim) * 0.3f;
				if (Math.abs(target - m.openAnim) < 0.01f) {
					m.openAnim = target;
				}

				int full = settingsHeight(m);
				int visible = Math.round(full * m.openAnim);
				if (visible > 0) {
					g.enableScissor(p.x, cy, p.x + W, cy + visible);
					g.fill(p.x, cy, p.x + W, cy + visible, Draw.argb(215, 0x0A0A10));
					int sy = cy;
					for (Setting s : m.settings) {
						int h = heightOf(s);
						drawSetting(g, s, p.x, sy, W, h, accent);
						if (m.expanded && m.openAnim > 0.95f && h > 0) {
							rows.add(new Row(TYPE_SETTING, p.x, sy, W, h, m, s, p));
						}
						sy += h;
					}
					g.disableScissor();
				}
				cy += visible;
			}
		}

		if (dragRow != null && mouseDown) {
			updateSlider(dragRow, mouseX);
		}
	}

	private static int heightOf(Setting s) {
		if (s.hidden) {
			return 0;
		}
		return (s instanceof NumSetting || s instanceof ColorSetting) ? 20 : 14;
	}

	private static int settingsHeight(Module m) {
		int h = 0;
		for (Setting s : m.settings) {
			h += heightOf(s);
		}
		return h;
	}

	private void drawSetting(GuiGraphics g, Setting s, int x, int y, int w, int h, int accent) {
		if (s.hidden) {
			return;
		}
		if (s instanceof BoolSetting b) {
			Draw.text(g, b.name, x + 7, y + 3, 0xD0D0D0);
			Draw.roundedRect(g, x + w - 22, y + 3, 14, 8, 3, Draw.argb(255, b.get() ? accent : 0x3A3A46));
			int knob = b.get() ? x + w - 14 : x + w - 21;
			g.fill(knob, y + 4, knob + 5, y + 10, Draw.argb(255, 0xFFFFFF));
		} else if (s instanceof NumSetting n) {
			Draw.text(g, n.name, x + 7, y + 3, 0xD0D0D0);
			String v = n.display();
			Draw.text(g, v, x + w - 7 - Draw.width(v), y + 3, 0xA0A0A0);
			int bx = x + 7;
			int bw = w - 14;
			int by = y + h - 7;
			g.fill(bx, by, bx + bw, by + 3, Draw.argb(255, 0x2A2A36));
			g.fill(bx, by, bx + Math.round(bw * (float) n.progress()), by + 3, Draw.argb(255, accent));
		} else if (s instanceof ModeSetting m) {
			Draw.text(g, m.name + ": " + m.get(), x + 7, y + 3, 0xD0D0D0);
		} else if (s instanceof ColorSetting c) {
			Draw.text(g, c.name, x + 7, y + 3, 0xD0D0D0);
			int bx = x + 7;
			int bw = w - 14;
			int by = y + h - 8;
			int segments = 28;
			for (int i = 0; i < segments; i++) {
				int x1 = bx + bw * i / segments;
				int x2 = bx + bw * (i + 1) / segments;
				g.fill(x1, by, x2, by + 4, Draw.argb(255, Mth.hsvToRgb(i / (float) segments, 0.65f, 1f)));
			}
			int mx = bx + Math.round(bw * c.hue());
			g.fill(mx - 1, by - 2, mx + 1, by + 6, Draw.argb(255, 0xFFFFFF));
		}
	}

	private void updateSlider(Row r, double mouseX) {
		double t = Mth.clamp((mouseX - (r.x() + 7)) / (r.w() - 14), 0.0, 1.0);
		if (r.setting() instanceof NumSetting n) {
			n.set(n.min + t * (n.max - n.min));
		} else if (r.setting() instanceof ColorSetting c) {
			c.setHue((float) t);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x();
		double my = event.y();
		int button = event.button();

		for (int i = rows.size() - 1; i >= 0; i--) {
			Row r = rows.get(i);
			if (!r.contains(mx, my)) {
				continue;
			}
			switch (r.type()) {
				case TYPE_HEADER -> {
					if (button == 0) {
						draggingPanel = r.panel();
					} else if (button == 1) {
						r.panel().collapsed = !r.panel().collapsed;
					}
				}
				case TYPE_MODULE -> {
					if (button == 0) {
						r.module().toggle();
					} else if (button == 1 && !r.module().settings.isEmpty()) {
						r.module().expanded = !r.module().expanded;
					}
				}
				case TYPE_SETTING -> {
					Setting s = r.setting();
					if (s instanceof BoolSetting b && button == 0) {
						b.toggle();
					} else if (s instanceof ModeSetting m) {
						m.cycle(button == 1 ? -1 : 1);
					} else if ((s instanceof NumSetting || s instanceof ColorSetting) && button == 0) {
						dragRow = r;
						mouseDown = true;
						updateSlider(r, mx);
					}
				}
				default -> {
				}
			}
			return true;
		}

		// Spotify-kaartje: links slepen, rechtsklik = inloggen.
		SpotifyHud spotify = ModuleManager.spotify();
		if (spotify.isEnabled() && spotify.contains(mx, my)) {
			if (button == 0) {
				draggingSpotify = true;
			} else if (button == 1) {
				SpotifyClient.login();
			}
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		mouseDown = false;
		dragRow = null;
		draggingPanel = null;
		draggingSpotify = false;
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (draggingPanel != null) {
			draggingPanel.x += (int) Math.round(dx);
			draggingPanel.y += (int) Math.round(dy);
			return true;
		}
		if (draggingSpotify) {
			ModuleManager.spotify().move(dx, dy);
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == GLFW.GLFW_KEY_RIGHT_SHIFT) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		ConfigManager.save();
		super.onClose();
	}
}
