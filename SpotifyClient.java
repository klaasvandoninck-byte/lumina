package com.lumina.spotify;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Praat met de Spotify Web API. Alle netwerkverkeer gebeurt op een eigen thread,
 * nooit op de render thread. De HUD leest alleen de volatile velden hieronder.
 */
public final class SpotifyClient {
	public record Track(String title, String artist, long progressMs, long durationMs, boolean playing,
						String coverUrl, long fetchedAt) {
	}

	public static final Identifier COVER_ID = Identifier.fromNamespaceAndPath("lumina", "spotify_cover");

	public static volatile Track track;
	public static volatile String status = "Bezig met laden...";
	public static volatile String notice = "";
	public static volatile long noticeUntil;
	public static volatile boolean coverReady;
	public static volatile int coverW;
	public static volatile int coverH;

	private static ScheduledExecutorService exec;
	private static volatile long backoffUntil;
	private static String loadedCover = "";

	private SpotifyClient() {
	}

	public static synchronized void start() {
		if (exec != null) {
			return;
		}
		exec = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r, "Lumina-Spotify");
			t.setDaemon(true);
			return t;
		});
		exec.scheduleWithFixedDelay(SpotifyClient::pollSafe, 0, 1500, TimeUnit.MILLISECONDS);
	}

	public static synchronized void stop() {
		if (exec != null) {
			exec.shutdownNow();
			exec = null;
		}
	}

	private static void pollSafe() {
		try {
			poll();
		} catch (Exception e) {
			status = "Verbinding met Spotify mislukt";
		}
	}

	private static void poll() throws Exception {
		SpotifyConfig cfg = SpotifyConfig.get();
		if (!cfg.hasClientId()) {
			track = null;
			status = "Vul je client ID in (lumina-spotify.json)";
			return;
		}
		if (!cfg.loggedIn()) {
			track = null;
			status = "Niet ingelogd (rechtsklik hier in de ClickGUI)";
			return;
		}
		if (System.currentTimeMillis() < backoffUntil) {
			return;
		}
		if (System.currentTimeMillis() > cfg.expiresAt - 60_000L && !SpotifyAuth.refresh()) {
			track = null;
			status = cfg.loggedIn() ? "Sessie vernieuwen mislukt" : "Niet ingelogd (rechtsklik hier in de ClickGUI)";
			return;
		}

		HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.spotify.com/v1/me/player/currently-playing?additional_types=track,episode"))
			.header("Authorization", "Bearer " + cfg.accessToken)
			.GET().build();
		HttpResponse<String> r = SpotifyAuth.HTTP.send(req, HttpResponse.BodyHandlers.ofString());

		switch (r.statusCode()) {
			case 200 -> parse(r.body());
			case 204 -> {
				track = null;
				status = "Er speelt niets";
			}
			case 401 -> cfg.expiresAt = 0; // volgende poll ververst de token
			case 429 -> {
				long wait = r.headers().firstValueAsLong("Retry-After").orElse(5);
				backoffUntil = System.currentTimeMillis() + wait * 1000L;
				status = "Rate limit, even wachten";
			}
			default -> status = "Spotify-fout " + r.statusCode();
		}
	}

	private static void parse(String body) throws IOException {
		JsonObject o = JsonParser.parseString(body).getAsJsonObject();
		JsonElement itemEl = o.get("item");
		if (itemEl == null || itemEl.isJsonNull()) {
			track = null;
			status = "Er speelt niets (advertentie?)";
			return;
		}
		JsonObject item = itemEl.getAsJsonObject();

		String title = item.has("name") ? item.get("name").getAsString() : "?";
		String artist = "";
		if (item.has("artists") && item.get("artists").isJsonArray()) {
			StringBuilder sb = new StringBuilder();
			for (JsonElement a : item.getAsJsonArray("artists")) {
				if (sb.length() > 0) {
					sb.append(", ");
				}
				sb.append(a.getAsJsonObject().get("name").getAsString());
			}
			artist = sb.toString();
		} else if (item.has("show") && item.get("show").isJsonObject()) {
			artist = item.getAsJsonObject("show").get("name").getAsString();
		}

		JsonArray images = null;
		if (item.has("album") && item.get("album").isJsonObject()) {
			images = item.getAsJsonObject("album").getAsJsonArray("images");
		} else if (item.has("images")) {
			images = item.getAsJsonArray("images");
		}
		String cover = "";
		if (images != null && images.size() > 0) {
			// De lijst loopt van groot naar klein: pak de kleinste (64x64), dat is genoeg.
			cover = images.get(images.size() - 1).getAsJsonObject().get("url").getAsString();
		}

		long progress = o.has("progress_ms") && !o.get("progress_ms").isJsonNull() ? o.get("progress_ms").getAsLong() : 0;
		long duration = item.has("duration_ms") ? item.get("duration_ms").getAsLong() : 0;
		boolean playing = o.has("is_playing") && o.get("is_playing").getAsBoolean();

		track = new Track(title, artist, progress, duration, playing, cover, System.currentTimeMillis());
		status = "";

		if (!cover.isEmpty() && !cover.equals(loadedCover)) {
			loadedCover = cover;
			loadCover(cover);
		}
	}

	private static void loadCover(String url) {
		try {
			HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
			HttpResponse<byte[]> r = SpotifyAuth.HTTP.send(req, HttpResponse.BodyHandlers.ofByteArray());
			if (r.statusCode() != 200) {
				return;
			}
			// Spotify levert JPEG. We zetten het eerst om naar PNG, dat leest NativeImage zeker.
			BufferedImage img = ImageIO.read(new ByteArrayInputStream(r.body()));
			if (img == null) {
				return;
			}
			ByteArrayOutputStream png = new ByteArrayOutputStream();
			ImageIO.write(img, "png", png);
			NativeImage ni = NativeImage.read(new ByteArrayInputStream(png.toByteArray()));

			Minecraft mc = Minecraft.getInstance();
			mc.execute(() -> {
				DynamicTexture tex = new DynamicTexture(() -> "lumina spotify cover", ni);
				mc.getTextureManager().register(COVER_ID, tex);
				coverW = ni.getWidth();
				coverH = ni.getHeight();
				coverReady = true;
			});
		} catch (Exception e) {
			System.err.println("[Lumina] Cover laden mislukt: " + e);
		}
	}

	/** Start het inlogproces: kopieert de login-link naar je klembord. */
	public static void login() {
		SpotifyConfig cfg = SpotifyConfig.get();
		Minecraft mc = Minecraft.getInstance();
		if (!cfg.hasClientId()) {
			notice("Vul eerst je client ID in (config/lumina-spotify.json)");
			return;
		}
		Thread t = new Thread(() -> {
			try {
				String url = SpotifyAuth.beginLogin();
				mc.execute(() -> {
					mc.keyboardHandler.setClipboard(url);
					if (mc.player != null) {
						mc.player.displayClientMessage(
							Component.literal("[Lumina] Spotify-loginlink staat op je klembord. Plak hem in je browser."), false);
					}
				});
			} catch (IOException e) {
				notice("Login-server starten mislukt (poort 8888 bezet?)");
			}
		}, "Lumina-Spotify-Login");
		t.setDaemon(true);
		t.start();
	}

	public static void playPause() {
		Track t = track;
		command("PUT", t != null && t.playing() ? "pause" : "play");
	}

	public static void next() {
		command("POST", "next");
	}

	public static void previous() {
		command("POST", "previous");
	}

	private static void command(String method, String action) {
		Thread t = new Thread(() -> {
			try {
				SpotifyConfig cfg = SpotifyConfig.get();
				if (!cfg.loggedIn()) {
					notice("Niet ingelogd");
					return;
				}
				if (System.currentTimeMillis() > cfg.expiresAt - 60_000L) {
					SpotifyAuth.refresh();
				}
				HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.spotify.com/v1/me/player/" + action))
					.header("Authorization", "Bearer " + cfg.accessToken)
					.method(method, HttpRequest.BodyPublishers.noBody())
					.build();
				HttpResponse<String> r = SpotifyAuth.HTTP.send(req, HttpResponse.BodyHandlers.ofString());
				if (r.statusCode() == 403) {
					notice("Bedienen vereist Spotify Premium");
				} else if (r.statusCode() == 404) {
					notice("Geen actief Spotify-apparaat");
				} else if (r.statusCode() == 429) {
					notice("Rate limit, probeer zo opnieuw");
				}
				Thread.sleep(400);
				pollSafe();
			} catch (Exception e) {
				notice("Commando mislukt");
			}
		}, "Lumina-Spotify-Command");
		t.setDaemon(true);
		t.start();
	}

	private static void notice(String text) {
		notice = text;
		noticeUntil = System.currentTimeMillis() + 4000L;
	}
}
