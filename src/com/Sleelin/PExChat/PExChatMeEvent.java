package com.Sleelin.PExChat;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;

public class PExChatMeEvent extends Event {
	private static final long serialVersionUID = -4007312028228633239L;
	private String message;
	private Player player;
	
	public PExChatMeEvent(final Player player, final String message) {
		super("PExChatMeEvent");
		this.player = player;
		this.message = message;
	}
	
	public String getMessage() {
		return message;
	}
	
	public Player getPlayer() {
		return player;
	}
}
