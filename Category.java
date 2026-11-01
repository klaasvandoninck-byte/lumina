package com.lumina.module;

public enum Category {
	VISUALS("Visuals"),
	COMBAT("Combat"),
	MOVEMENT("Movement"),
	PLAYER("Player");

	public final String title;

	Category(String title) {
		this.title = title;
	}
}
