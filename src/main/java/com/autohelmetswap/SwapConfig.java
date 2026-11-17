package com.autohelmetswap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

/** Persistent client-side settings for Auto Swap. */
public final class SwapConfig {
	private static final int DEFAULT_DELAY_MILLIS = 100;
	private static final int MAX_DELAY_MILLIS = 5000;
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("autohelmetswap.properties");
	private static int delayMillis = DEFAULT_DELAY_MILLIS;

	private SwapConfig() {
	}

	public static int getDelayMillis() {
		return delayMillis;
	}

	public static void setDelayMillis(int value) {
		delayMillis = Math.max(0, Math.min(MAX_DELAY_MILLIS, value));
		save();
	}

	public static void load() {
		if (!Files.isRegularFile(FILE)) {
			return;
		}
		Properties properties = new Properties();
		try (InputStream input = Files.newInputStream(FILE)) {
			properties.load(input);
			int value = Integer.parseInt(properties.getProperty("delayMillis", String.valueOf(DEFAULT_DELAY_MILLIS)));
			delayMillis = Math.max(0, Math.min(MAX_DELAY_MILLIS, value));
		} catch (IOException | NumberFormatException ignored) {
			delayMillis = DEFAULT_DELAY_MILLIS;
		}
	}

	private static void save() {
		Properties properties = new Properties();
		properties.setProperty("delayMillis", String.valueOf(delayMillis));
		try {
			Files.createDirectories(FILE.getParent());
			try (OutputStream output = Files.newOutputStream(FILE)) {
				properties.store(output, "Auto Swap settings");
			}
		} catch (IOException exception) {
			System.err.println("[Auto Swap] Could not save settings: " + exception.getMessage());
		}
	}
}
