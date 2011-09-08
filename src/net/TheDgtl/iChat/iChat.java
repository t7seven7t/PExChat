package net.TheDgtl.iChat;

/**
 * iChat - A chat formatting plugin for Bukkit.
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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.command.ColouredConsoleSender;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Player;
//import org.bukkit.event.CustomEventListener;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

import ru.tehkode.permissions.*;
import ru.tehkode.permissions.bukkit.PermissionsEx;

public class iChat extends JavaPlugin {
    
	//For storing tracks and their contained groups and priority
	public final class Track {
		public String name = "";
		public Integer priority = 0;
		public List<String> groups = new ArrayList<String>();		
	}
	
	public PermissionManager permissions = null;
	
	private playerListener pListener = new playerListener(this);
	//private customListener cListener = new customListener();
	
	private PluginManager pm;
	//private Logger log;
	public ColouredConsoleSender console = null;
	Configuration config;
	
	// Config variables
	public String censorChar = "*";
	public boolean censorColored = false;
	public String censorColor = "&f";
	public String chatColor = "&f";
	public List<String> censorWords = new ArrayList<String>();
	public String chatFormat = "[+prefix+group+suffix&f] +name: +message";
    public String multigroupFormat = "[+prefix+group+suffix&f]";
	public String meFormat = "* +name +message";
	public String dateFormat = "HH:mm:ss";
	public List<Track> tracks = new ArrayList<Track>();
	public HashMap<String, String> aliases = new HashMap<String, String>();
	
	// External interface
	public static iChat ichat = null;
	
	public void onEnable() {
		pm = getServer().getPluginManager();
		console = new ColouredConsoleSender((CraftServer)getServer());
		config = getConfiguration();
		
		//check for PermissionsEx plugin
		if(pm.isPluginEnabled("PermissionsEx")){
			permissions = PermissionsEx.getPermissionManager();
		} else {
			//not found, disable
			console.sendMessage("[iChat] Permissions plugin not found or wrong version. Disabling");
			pm.disablePlugin(this);
			return;
		}
		
		// Create default config if it doesn't exist.
		if (!(new File(getDataFolder(), "config.yml")).exists()) {
			defaultConfig();
		}
		loadConfig();
		
		// Register events
		pm.registerEvent(Event.Type.PLAYER_CHAT, pListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_COMMAND_PREPROCESS, pListener, Event.Priority.Normal, this);
		//pm.registerEvent(Event.Type.CUSTOM_EVENT, cListener, Event.Priority.Normal, this);
		
		// Setup external interface
		iChat.ichat = this;
		
		console.sendMessage(getDescription().getName() + " (v" + getDescription().getVersion() + ") enabled");
	}
	
	public void onDisable() {
		console.sendMessage("[iChat] iChat Disabled");
	}
	
	private void loadConfig() {
		config.load();
		censorChar = config.getString("censor-char", censorChar);
		censorColored = config.getBoolean("censor-colored", censorColored);
		censorColor = config.getString("censor-color", censorColor);
		chatColor = config.getString("censor-string-color", chatColor);
		censorWords = config.getStringList("censor-list", censorWords);
		chatFormat = config.getString("message-format", chatFormat);
        multigroupFormat = config.getString("multigroup-format", multigroupFormat);
		dateFormat = config.getString("date-format", dateFormat);
		meFormat = config.getString("me-format", meFormat);
		List<String> tracknames = new ArrayList<String>();
		tracknames = config.getKeys("tracks");
		if (tracknames != null){
			for (String track : tracknames){
				//this.console.sendMessage(track);
				Track loadtrack = new Track();
				loadtrack.groups = config.getStringList("tracks."+track+".groups", loadtrack.groups);
				/*for (String group : loadtrack.groups){
					console.sendMessage(group);
				}*/
				loadtrack.priority = config.getInt("tracks."+track+".priority", 0);
				//this.console.sendMessage(loadtrack.priority.toString());
				loadtrack.name = track;
				tracks.add(loadtrack);
			}
		}
		List<String> tmpaliases = new ArrayList<String>();
		tmpaliases = config.getKeys("aliases");
		if (tmpaliases != null){
			for (String alias : tmpaliases){
				aliases.put(alias, config.getString("aliases."+alias));
			}
		}
	}
	
	private void defaultConfig() {
		config.setProperty("censor-char", censorChar);
		config.setProperty("censor-colored", censorColored);
		config.setProperty("censor-color", censorColor);
		config.setProperty("censor-string-color", chatColor);
		config.setProperty("censor-list", censorWords);
		config.setProperty("message-format", chatFormat);
        config.setProperty("multigroup-format", multigroupFormat);
		config.setProperty("date-format", dateFormat);
		config.setProperty("me-format", meFormat);
        HashMap<String, String> aliases = new HashMap<String, String>();
        aliases.put("Admin", "A");
        List<String> track = new ArrayList<String>();
        track.add("Admin");
        track.add("Moderator");
        track.add("Builder");
        config.setProperty("tracks.default.groups", track);
        config.setProperty("tracks.default.priority", 1);
        config.setProperty("aliases", aliases);
		config.save();
	}
	
	/*
	 * Parse given text string for permissions variables
	 */
	public String parseVars(String format, Player p) {
		Pattern pattern = Pattern.compile("\\+\\{(.*?)\\}");
		Matcher matcher = pattern.matcher(format);
		StringBuffer sb = new StringBuffer();
		while (matcher.find()) {
			String var = getVariable(p, matcher.group(1));
			matcher.appendReplacement(sb, Matcher.quoteReplacement(var));
		}
		matcher.appendTail(sb);
		return sb.toString();
	}
	
	/*path
	 * Parse a given text string and replace the variables/color codes.
	 */
	public String replaceVars(String format, String[] search, String[] replace) {
		if (search.length != replace.length) return "";
		for (int i = 0; i < search.length; i++) {
			if (search[i].contains(",")) {
				for (String s : search[i].split(",")) {
					if (s == null || replace[i] == null) continue;
					format = format.replace(s, replace[i]);
				}
			} else {
				format = format.replace(search[i], replace[i]);
			}
		}
		return format.replaceAll("(&([a-f0-9]))", "\u00A7$2");
	}
	
	/*
	 * Replace censored words.
	 */
	public String censor(Player p, String msg) {
		if (censorWords == null || censorWords.size() == 0) {
			if (!hasPerm(p, "ichat.color", true))
				return msg.replaceAll("(&([a-f0-9]))", "");
			else 
				return msg;
		}
		String[] split = msg.split(" ");
		StringBuilder out = new StringBuilder();
		// Loop over all words.
		for (String word : split) {
			for (String cen : censorWords) {
				if (word.equalsIgnoreCase(cen)) {
					word = star(word);
					if (censorColored) {
						word = censorColor + word + chatColor;
					}
					break;
				}
			}
			out.append(word).append(" ");
		}
		if (!hasPerm(p, "ichat.color", true))
			return out.toString().replaceAll("(&([a-f0-9]))", "").trim();
		else 
			return out.toString().trim();
	}
	private String star(String word) {
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < word.length(); i++)
			out.append(censorChar);
		return out.toString();
	}
	
	/**
	 * @param p - Player object for chatting
	 * @param msg - Message to be formatted
	 * @param chatFormat - The requested chat format string
	 * @return - New message format
	 */
	public String parseChat(Player p, String msg, String chatFormat) {
		// Variables we can use in a message
		String prefix = getPrefix(p);
		String suffix = getSuffix(p);
		String group = getGroup(p);
		if (prefix == null) prefix = "";
		if (suffix == null) suffix = "";
		if (group == null) group = "";
		String healthbar = healthBar(p);
		String health = String.valueOf(p.getHealth());
		String world = p.getWorld().getName();
		// Timestamp support
		Date now = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat(this.dateFormat);
		String time = dateFormat.format(now);
		
		// We're sending this to String.format, so we need to escape those pesky % symbols
		msg = msg.replaceAll("%", "%%");
		// Censor message
		msg = censor(p, msg);
		
		String format = parseVars(chatFormat, p);
		
		String groups = "";
		if (format.contains("+groups")) {
			groups = parseGroups(p, multigroupFormat);
		}
		
		// Order is important, this allows us to use all variables in the suffix and prefix! But no variables in the message
		String[] search = new String[] {"+suffix,+s", "+prefix,+p", "+groups,+gs", "+group,+g", "+healthbar,+hb", "+health,+h", "+world,+w", "+time,+t", "+name,+n", "+displayname,+d", "+message,+m"};
		String[] replace = new String[] { suffix, prefix, groups, group, healthbar, health, world, time, p.getName(), p.getDisplayName(), msg };
		return replaceVars(format, search, replace);
	}
	
	/**
	 * @param p - Player object for chatting
	 * @param msg - Message to be formatted
	 * @return - New message format
	 */
	public String parseChat(Player p, String msg) {
		return parseChat(p, msg, this.chatFormat);
	}
    
    /**
     * @param p - Player object for chatting
     * @param msg - Message to be formatted
	 * @param multigroupsFormat - The requested chat format string
	 * @return - replacement for +groups
     */
    public String parseGroups(Player p, String mgFormat){
        String[] groups = permissions.getUser(p).getGroupsNames();
        
        String output = "";
        HashMap<Integer, String> unparsedGroups = new HashMap<Integer, String>();
        int max = 0;
        int key = 0;
        for (String group : groups){
        	for (Track track : tracks){
        		for (String trackgroup : track.groups){
        			if (trackgroup.equalsIgnoreCase(group)){
        				key = track.priority;
        				while (unparsedGroups.containsKey(key)){
        					key++;
        				}
        				unparsedGroups.put(key, group);
        				if (key > max){
        					max = key;
        				}
        			}
        		}
        	}
        }
        
        String format = parseVars(mgFormat, p);        
        
        for (int i=0; i<=max; i++){
        	if (unparsedGroups.containsKey(i)){
	        	String groupname = unparsedGroups.get(i);
	        	String prefix = getGroupPrefix(groupname, p.getWorld().getName());
	        	if (prefix == null){
	        		prefix = "";
	        	}
	        	String suffix = getGroupSuffix(groupname, p.getWorld().getName());
	        	if (suffix == null){
	        		suffix = "";
	        	}
	        	groupname = getAlias(groupname);
	        	String[] search = new String[] {"+suffix,+s", "+prefix,+p", "+group,+g"};
	        	String[] replace = new String[] { suffix, prefix, groupname};
	        	output = output + replaceVars(format, search, replace);
        	}
        }
        
		return output;
    }
	
	private String getAlias(String group) {
		if (aliases.containsKey(group)){
			return aliases.get(group);
		} else {
			return group;
		}
	}

	/*
	 * Return a health bar string.
	 */
	public String healthBar(Player player) {
		float maxHealth = 20;
		float barLength = 10;
		float health = player.getHealth();
		int fill = Math.round( (health / maxHealth) * barLength );
		String barColor = "&2";
		// 0-40: Red  40-70: Yellow  70-100: Green
		if (fill <= 4) barColor = "&4";
		else if (fill <= 7) barColor = "&e";
		else barColor = "&2";

		StringBuilder out = new StringBuilder();
		out.append(barColor);
		for (int i = 0; i < barLength; i++) {
			if (i == fill) out.append("&8");
			out.append("|");
		}
		out.append("&f");
		return out.toString();
	}
	/*
	 * Check whether the player has the given permissions.
	 */
	public boolean hasPerm(Player player, String perm, boolean def) {
		if (permissions != null) {
			return permissions.has(player, perm);
		} else {
			return def;
		}
	}
	
	/*
	 * Get the players group prefix.
	 */
	public String getPrefix(Player player) {
		if (permissions != null) {
			return permissions.getUser(player).getPrefix(player.getWorld().getName());
		}
		console.sendMessage("[iChat::getPrefix] SEVERE: There is no Permissions module, why are we running?!??!?");
		return null;
	}
	
	/*
	 * Get the players group suffix.
	 */
	public String getSuffix(Player player) {
		if (permissions != null) {
			return permissions.getUser(player).getSuffix(player.getWorld().getName());
		}
		console.sendMessage("[iChat::getSuffix] SEVERE: There is no Permissions module, why are we running?!??!?");
		return null;
	}
	
	/*
	 * Get the group's prefix.
	 */	
	
	public String getGroupPrefix(String group, String worldname) {
		if (permissions != null) {
			return permissions.getGroup(group).getPrefix(worldname);
		}
		console.sendMessage("[iChat::getPrefix] SEVERE: There is no Permissions module, why are we running?!??!?");
		return null;
	}
	
	/*
	 * Get the group's suffix.
	 */
	public String getGroupSuffix(String group, String worldname) {
		if (permissions != null) {
			return permissions.getGroup(group).getSuffix(worldname);
		}
		console.sendMessage("[iChat::getSuffix] SEVERE: There is no Permissions module, why are we running?!??!?");
		return null;
	}
	
	/*
	 * Get the players group
	 */
	public String getGroup(Player player) {
		if (permissions != null) {
			String groups[] = permissions.getUser(player).getGroupsNames(player.getWorld().getName());
			return groups[0];
		}
		console.sendMessage("[iChat::getGroup] SEVERE: There is no Permissions module, why are we running?!??!?");
		return null;
	}
	
	/*
	 * Get a user/group specific variable. User takes priority
	 */
	public String getVariable(Player player, String variable) {
		if (permissions != null) {
			// Check for a user variable
			String userVar = permissions.getUser(player).getOption(variable); 
			if (userVar != null && !userVar.isEmpty()) {
				return userVar;
			}
			// Check for a group variable
			String group = permissions.getGroup(getGroup(player)).getName();
					
			if (group == null) return "";
			String groupVar = permissions.getGroup(group).getOption(variable);
					
			if (groupVar == null) return "";
			return groupVar;
		}
		console.sendMessage("[iChat::getVariable] SEVERE: There is no Permissions module, why are we running?!!??!?!");
		return "";
	}

	/*
	private class customListener extends CustomEventListener {
		@Override
		public void onCustomEvent(Event event) {
			if (event.getEventName().equalsIgnoreCase("iChatMeEvent")) {
				console.sendMessage("iChat ME Event");
			}
		}
	}*/
	
	/*
	 * Command Handler
	 */
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!command.getName().equalsIgnoreCase("ichat")) return false;
		if (sender instanceof Player && !hasPerm((Player)sender, "ichat.reload", sender.isOp())) {
			sender.sendMessage("[iChat] Permission Denied");
			return true;
		}
		if (args.length != 1) return false;
		if (args[0].equalsIgnoreCase("reload")) {
			aliases.clear();
			tracks.clear();
			loadConfig();
			sender.sendMessage("[iChat] Config Reloaded");
			return true;
		}
		return false;
	}
}
