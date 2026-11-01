package com.lumina.util;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public final class MoveUtil {
	private MoveUtil() {
	}

	/**
	 * Richting (x, z) van de ingedrukte bewegingstoetsen ten opzichte van de kijkrichting.
	 * Genormaliseerd: lengte is 0 (geen input) of 1.
	 */
	public static double[] inputDirection(float yawDegrees) {
		Minecraft mc = Minecraft.getInstance();
		double forward = (mc.options.keyUp.isDown() ? 1 : 0) - (mc.options.keyDown.isDown() ? 1 : 0);
		double strafe = (mc.options.keyLeft.isDown() ? 1 : 0) - (mc.options.keyRight.isDown() ? 1 : 0);

		float yaw = yawDegrees * Mth.DEG_TO_RAD;
		double sin = Mth.sin(yaw);
		double cos = Mth.cos(yaw);

		double x = -sin * forward + cos * strafe;
		double z = cos * forward + sin * strafe;

		double len = Math.sqrt(x * x + z * z);
		if (len < 1.0E-6) {
			return new double[] {0, 0};
		}
		return new double[] {x / len, z / len};
	}
}
