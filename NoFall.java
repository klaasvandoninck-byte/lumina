package com.lumina.module.impl;

import com.lumina.module.Category;
import com.lumina.module.Module;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

public class NoFall extends Module {
	public NoFall() {
		super("NoFall", "Geen valschade.", Category.PLAYER);
	}

	@Override
	public void onTick() {
		LocalPlayer p = mc.player;
		if (mc.getConnection() == null || p.isFallFlying() || p.getAbilities().flying) {
			return;
		}
		if (p.fallDistance > 2.5) {
			mc.getConnection().send(new ServerboundMovePlayerPacket.StatusOnly(true, p.horizontalCollision));
		}
	}
}
