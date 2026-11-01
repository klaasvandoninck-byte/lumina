package com.lumina.spotify;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Client ID en tokens, opgeslagen in config/lumina-spotify.json.
 * Er is bewust GEEN client secret nodig (PKCE). De tokens staan wel leesbaar op schijf,
 * deel dit bestand dus nooit.
 */
public final class SpotifyConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static SpotifyConfig instance;

	public String clientId = "";
	public String accessToken = "";
	public String refreshToken = "";
	public long expiresAt;

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("lumina-spotify.json");
	}

	public static synchronized SpotifyConfig get() {
		if (instance == null) {
			instance = new SpotifyConfig();
			Path p = file();
			if (Files.exists(p)) {
				try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
					SpotifyConfig loaded = GSON.fromJson(r, SpotifyConfig.class);
					if (loaded != null) {
						instance = loaded;
					}
				} catch (Exception e) {
					System.err.println("[Lumina] Spotify-config lezen mislukt: " + e);
				}
			} else {
				instance.save();
			}
		}
		return instance;
	}

	public synchronized void save() {
		Path p = file();
		try (Writer w = Files.newBufferedWriter(p, StandardCharsets.UTF_8)) {
			GSON.toJson(this, w);
		} catch (IOException e) {
			System.err.println("[Lumina] Spotify-config opslaan mislukt: " + e);
			return;
		}
		try {
			Files.setPosixFilePermissions(p, PosixFilePermissions.fromString("rw-------"));
		} catch (Exception ignored) {
			// Windows: geen POSIX-rechten, dat is prima.
		}
	}

	public boolean loggedIn() {
		return refreshToken != null && !refreshToken.isEmpty();
	}

	public boolean hasClientId() {
		return clientId != null && !clientId.isBlank();
	}
}
