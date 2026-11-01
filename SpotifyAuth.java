package com.lumina.spotify;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/** OAuth Authorization Code met PKCE: geen client secret in de mod. */
public final class SpotifyAuth {
	public static final String REDIRECT = "http://127.0.0.1:8888/callback";
	private static final String SCOPE = "user-read-currently-playing user-read-playback-state user-modify-playback-state";
	static final HttpClient HTTP = HttpClient.newHttpClient();
	private static final SecureRandom RANDOM = new SecureRandom();

	private static HttpServer server;
	private static String verifier;
	private static String state;

	private SpotifyAuth() {
	}

	private static String enc(String s) {
		return URLEncoder.encode(s, StandardCharsets.UTF_8);
	}

	private static String randomUrlSafe(int bytes) {
		byte[] b = new byte[bytes];
		RANDOM.nextBytes(b);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
	}

	/** Start de lokale callback-server en geeft de login-URL terug. */
	public static synchronized String beginLogin() throws IOException {
		stopServer();
		SpotifyConfig cfg = SpotifyConfig.get();

		verifier = randomUrlSafe(48);
		state = randomUrlSafe(12);
		String challenge;
		try {
			byte[] hash = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
			challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
		} catch (Exception e) {
			throw new IOException(e);
		}

		server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 8888), 0);
		server.createContext("/callback", SpotifyAuth::handleCallback);
		server.setExecutor(Executors.newSingleThreadExecutor(r -> {
			Thread t = new Thread(r, "Lumina-Spotify-Callback");
			t.setDaemon(true);
			return t;
		}));
		server.start();

		// Sluit de server automatisch na 5 minuten.
		Thread timeout = new Thread(() -> {
			try {
				Thread.sleep(5 * 60 * 1000L);
			} catch (InterruptedException ignored) {
			}
			stopServer();
		}, "Lumina-Spotify-Timeout");
		timeout.setDaemon(true);
		timeout.start();

		return "https://accounts.spotify.com/authorize"
			+ "?client_id=" + enc(cfg.clientId.trim())
			+ "&response_type=code"
			+ "&redirect_uri=" + enc(REDIRECT)
			+ "&code_challenge_method=S256"
			+ "&code_challenge=" + enc(challenge)
			+ "&scope=" + enc(SCOPE)
			+ "&state=" + enc(state);
	}

	public static synchronized void stopServer() {
		if (server != null) {
			server.stop(0);
			server = null;
		}
	}

	private static void handleCallback(HttpExchange ex) throws IOException {
		Map<String, String> q = new HashMap<>();
		String raw = ex.getRequestURI().getRawQuery();
		if (raw != null) {
			for (String part : raw.split("&")) {
				int i = part.indexOf('=');
				if (i > 0) {
					q.put(URLDecoder.decode(part.substring(0, i), StandardCharsets.UTF_8),
						URLDecoder.decode(part.substring(i + 1), StandardCharsets.UTF_8));
				}
			}
		}

		boolean ok = false;
		String code = q.get("code");
		if (code != null && state != null && state.equals(q.get("state"))) {
			SpotifyConfig cfg = SpotifyConfig.get();
			ok = tokenRequest("grant_type=authorization_code"
				+ "&code=" + enc(code)
				+ "&redirect_uri=" + enc(REDIRECT)
				+ "&client_id=" + enc(cfg.clientId.trim())
				+ "&code_verifier=" + enc(verifier));
		}

		String html = "<html><body style=\"font-family:sans-serif;background:#101018;color:#fff;text-align:center;padding-top:80px\">"
			+ (ok ? "<h2>Ingelogd bij Spotify</h2><p>Je kunt dit tabblad sluiten en terug naar Minecraft gaan.</p>"
				: "<h2>Inloggen mislukt</h2><p>Probeer het opnieuw in Minecraft.</p>")
			+ "</body></html>";
		byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
		ex.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
		ex.sendResponseHeaders(200, bytes.length);
		try (OutputStream os = ex.getResponseBody()) {
			os.write(bytes);
		}

		if (ok) {
			new Thread(SpotifyAuth::stopServer, "Lumina-Spotify-Stop").start();
		}
	}

	/** Haalt een nieuwe access token op met de refresh token. */
	public static boolean refresh() {
		SpotifyConfig cfg = SpotifyConfig.get();
		if (!cfg.loggedIn()) {
			return false;
		}
		return tokenRequest("grant_type=refresh_token"
			+ "&refresh_token=" + enc(cfg.refreshToken)
			+ "&client_id=" + enc(cfg.clientId.trim()));
	}

	private static boolean tokenRequest(String body) {
		SpotifyConfig cfg = SpotifyConfig.get();
		try {
			HttpRequest req = HttpRequest.newBuilder(URI.create("https://accounts.spotify.com/api/token"))
				.header("Content-Type", "application/x-www-form-urlencoded")
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build();
			HttpResponse<String> r = HTTP.send(req, HttpResponse.BodyHandlers.ofString());

			if (r.statusCode() == 400 && r.body().contains("invalid_grant")) {
				cfg.refreshToken = "";
				cfg.accessToken = "";
				cfg.save();
				return false;
			}
			if (r.statusCode() != 200) {
				return false;
			}
			JsonObject o = JsonParser.parseString(r.body()).getAsJsonObject();
			cfg.accessToken = o.get("access_token").getAsString();
			if (o.has("refresh_token")) {
				cfg.refreshToken = o.get("refresh_token").getAsString();
			}
			cfg.expiresAt = System.currentTimeMillis() + o.get("expires_in").getAsLong() * 1000L;
			cfg.save();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
