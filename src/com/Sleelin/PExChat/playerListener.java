package com.Sleelin.PExChat;

/**
 * PExChat - A chat formatting plugin for Bukkit.
 * Copyright (C) 2011 Steven "Drakia" Scott <Drakia@Gmail.com>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerListener;

public class playerListener extends PlayerListener {
	// Use this for permissions checking.
	PExChat pexchat;
	
	playerListener(PExChat ichat) {
		this.pexchat = ichat;
	}
	
	@Override
	public void onPlayerChat(PlayerChatEvent event) {
		if (pexchat.permissions == null) return;
		if (event.isCancelled()) return;
		Player p = event.getPlayer();
		String msg = event.getMessage();
		
		event.setFormat( pexchat.parseChat(p, msg) + " " );
	}
	
	// Use CommandPreprocess because that's what Justin said.
	@Override
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		if (pexchat.permissions == null) return;
		if (event.isCancelled()) return;
		Player p = event.getPlayer();
		String message = event.getMessage();
		
		if (message.toLowerCase().startsWith("/me ")) {
			String s = message.substring(message.indexOf(" ")).trim();
			String formatted = pexchat.parseChat(p, s, pexchat.meFormat);
			// Call custom event
			PExChatMeEvent meEvent = new PExChatMeEvent(p, s);
			pexchat.getServer().getPluginManager().callEvent(meEvent);
			
			pexchat.console.info(formatted);
			for (Player t : pexchat.getServer().getOnlinePlayers()) {
				t.sendMessage(formatted);
			}
			event.setCancelled(true);
		}
	}
}
