package com.Sleelin.PExChat;

/**
 * PExChat - A chat formatting plugin for Bukkit.
 * Author: Sleelin 
 * 
 * License:
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
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import ru.tehkode.permissions.*;
import ru.tehkode.permissions.bukkit.PermissionsEx;

public class PExChat extends JavaPlugin {
    
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
	public Logger console = null;
	FileConfiguration config;
	File configFile;
	
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
	public static PExChat pexchat = null;
	
	public void onEnable() {
		pm = getServer().getPluginManager();
		console = Logger.getLogger("Minecraft");
		
		
		//check for PermissionsEx plugin
		if(pm.isPluginEnabled("PermissionsEx")){
			permissions = PermissionsEx.getPermissionManager();
		} else {
			//not found, disable
			console.info("[PExChat] Permissions plugin not found or wrong version. Disabling");
			pm.disablePlugin(this);
			return;
		}
		
		// Create default config if it doesn't exist.
		configFile = new File(getDataFolder() + "/config.yml");
		if (!configFile.exists()){
			defaultConfig();
		}
		loadConfig();
		
		// Register events
		pm.registerEvents(pListener, this);
		pm.registerEvents(pListener, this);
		
		// Setup external interface
		PExChat.pexchat = this;
		
		console.info("["+getDescription().getName() + "] v" + getDescription().getVersion() + " enabled");
	}
	
	public void onDisable() {
		console.info("["+getDescription().getName()+"] PExChat Disabled");
	}
	
	private void loadConfig() {
		config = YamlConfiguration.loadConfiguration(configFile);
		censorChar = config.getString("censor-char");
		censorColored = config.getBoolean("censor-colored");
		censorColor = config.getString("censor-color");
		chatColor = config.getString("censor-string-color");
		censorWords = config.getStringList("censor-list");
		chatFormat = config.getString("message-format");
        multigroupFormat = config.getString("multigroup-format");
		dateFormat = config.getString("date-format");
		meFormat = config.getString("me-format");
		Set<String> tracknames = new HashSet<String>();
	    tracknames = config.getConfigurationSection("tracks").getKeys(false);

		if (tracknames != null){
			for (String track : tracknames){
				Track loadtrack = new Track();
				loadtrack.groups = config.getStringList("tracks."+track+".groups");
				loadtrack.priority = config.getInt("tracks."+track+".priority", 0);
				loadtrack.name = track;
				tracks.add(loadtrack);
			}
		}
		Set<String> tmpaliases = new HashSet<String>();
		tmpaliases = config.getConfigurationSection("aliases").getKeys(false);
		if (tmpaliases != null){
			for (String alias : tmpaliases){
				aliases.put(alias, config.getString("aliases."+alias));
			}
		}
	}
	
	private void defaultConfig() {
		config = YamlConfiguration.loadConfiguration(configFile);
		config.set("censor-char", censorChar);
		config.set("censor-colored", censorColored);
		config.set("censor-color", censorColor);
		config.set("censor-string-color", chatColor);
		config.set("censor-list", censorWords);
		config.set("message-format", chatFormat);
        config.set("multigroup-format", multigroupFormat);
		config.set("date-format", dateFormat);
		config.set("me-format", meFormat);
        HashMap<String, String> aliases = new HashMap<String, String>();
        aliases.put("Admin", "A");
        List<String> track = new ArrayList<String>();
        track.add("Admin");
        track.add("Moderator");
        track.add("Builder");
        config.set("tracks.default.groups", track);
        config.set("tracks.default.priority", 1);
        config.set("aliases", aliases);
		try {
			config.save(configFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
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
	
	/*
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
		format = format.replaceAll("(&([zZ]))", "&z");
		if (format.contains("&z")) {
			StringBuilder ret = new StringBuilder();
			String[] ss = format.split("&z");
			for (int j = 0; j < ss.length; j++) {
				if (j == 0) {
					ret.append(ss[j]);
					continue;
				}
				String s = ss[j];
				int clr = 256;
				clr = indexOfFirstColor(s);
				int index = 0;
				for (int i = 0; i < s.length(); i++) {
					if (clr != -1 && i > clr) {
						ret.append(s.substring(i, i+1));
						continue;
					}
					ret.append("&" + RainbowColours.getColour(index));
					ret.append(s.substring(i, i+1));
					index++;
					
					if (index >= 7)
						index = 0;
				}
			}
			format = ret.toString();
		}
		return format.replaceAll("(&([a-f0-9k-orR]))", "\u00A7$2");
	}
	
	private int indexOfFirstColor(String s) {
		for (int i = 0; i < 22; i++) {
			if (s.contains("&" + ChatFormat.getChar(i)))
				return s.indexOf("&" + ChatFormat.getChar(i));
		}
		return -1;
	}

	/*
	 * Replace censored words.
	 */
	public String censor(Player p, String msg) {
		if (censorWords == null || censorWords.size() == 0) {
			String ret = msg;
			if (!hasPerm(p, "pexchat.color"))
				ret = ret.replaceAll("(&([a-f0-9l-orR]))", "");
			if (!hasPerm(p, "pexchat.magic"))
				ret = ret.replaceAll("(&([kK]))", "");
			if (!hasPerm(p, "pexchat.rainbow") || p.getName().equalsIgnoreCase("meeperme"))
				ret = ret.replaceAll("(&([zZ]))", "");
			return ret;
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
		String ret = out.toString();
		if (!hasPerm(p, "pexchat.color"))
			ret = ret.replaceAll("(&([a-f0-9l-orR]))", "");
		if (!hasPerm(p, "pexchat.magic"))
			ret = ret.replaceAll("(&([kK]))", "");
		if (!hasPerm(p, "pexchat.rainbow"))
			ret = ret.replaceAll("(&([zZ]))", "");
		return ret.trim();
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
	public String parseChat(final Player p, String msg, String chatFormat) {
		// Variables we can use in a message
		String prefix = getPrefix(p);
		String suffix = getSuffix(p);
		String group = getGroup(p);
//		Future<String> prefix = Bukkit.getScheduler().callSyncMethod(this, new Callable<String>() {
//
//			@Override
//			public String call() {
//				if (permissions != null)
//					return permissions.getUser(p).getPrefix(p.getWorld().getName());
//				console.severe("["+getDescription().getName()+"] There is no Permissions module, why are we running?!??!?");
//				return "";
//			}
//			
//		});
//		Future<String> suffix = Bukkit.getScheduler().callSyncMethod(this, new Callable<String>() {
//			
//			@Override
//			public String call() {
//				if (permissions != null)
//					return permissions.getUser(p).getSuffix(p.getWorld().getName());
//				console.severe("["+getDescription().getName()+"] There is no Permissions module, why are we running?!??!?");
//				return "";
//			}
//			
//		});
//		Future<String> group = Bukkit.getScheduler().callSyncMethod(this, new Callable<String>() {
//			
//			@Override
//			public String call() {
//				if (permissions != null) {
//					String groups[] = permissions.getUser(p).getGroupsNames(p.getWorld().getName());
//					return groups[0];
//				}
//				console.severe("["+getDescription().getName()+"] There is no Permissions module, why are we running?!??!?");
//				return null;
//			}
//		});
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
		
		// Add user defined variables into the message 
		String format = parseVars(chatFormat, p);
		
		// Add multigroup formatted text in place of +groups
		String groups = "";
		if (format.contains("+groups")) {
			groups = parseGroups(p, multigroupFormat);
		}
		
		// Add support for track-specific prefix/suffix/groupname
		ArrayList<String> searchlist = new ArrayList<String>();		
		ArrayList<String> replacelist = new ArrayList<String>();
		
		// Get the groups a player is in
		String[] playergroups = permissions.getUser(p).getGroupsNames();
		
		Boolean found;
		// For each track, add new search and replace variables
		for (Track track : tracks){
			found = false;
			// For each group a player is in, see if it is in this track 
			for (String playergroup : playergroups){
				if (track.groups.contains(playergroup)){
					// Add the variables to be replaced
					searchlist.add("+prefix."+track.name);
					searchlist.add("+suffix."+track.name);
					searchlist.add("+group."+track.name);
					// Add the content to put in place of the variables
					replacelist.add(getGroupPrefix(playergroup, p.getWorld().getName()));
					replacelist.add(getGroupSuffix(playergroup, p.getWorld().getName()));
					replacelist.add(getAlias(playergroup));
					// Disable the track variable
					found = true;
				}
			}
			if (found.equals(false)){
				searchlist.add("+prefix."+track.name);
				searchlist.add("+suffix."+track.name);
				searchlist.add("+group."+track.name);
				replacelist.add("");
				replacelist.add("");
				replacelist.add("");
			}
		}
		
		// Add every other variable and replacement into the list
		// Order is important, this allows us to use all variables in the suffix and prefix! But no variables in the message
		String[] search = new String[] {"+suffix,+s", "+prefix,+p", "+groups,+gs", "+group,+g", "+healthbar,+hb", "+health,+h", "+world,+w", "+time,+t", "+name,+n", "+displayname,+d", "+message,+m"};
		String[] replace = new String[] {suffix, prefix, groups, group, healthbar, health, world, time, p.getName(), p.getDisplayName(), msg};
		
			for (int i=0; i<search.length; i++){
				searchlist.add(search[i]);
			}		
			for (int i=0; i<replace.length; i++){
				replacelist.add(replace[i]);
			}
			
			// Convert back to arrays
			search = (String[]) searchlist.toArray(new String[searchlist.size()]);
			replace = (String[]) replacelist.toArray(new String[replacelist.size()]);

			return replaceVars(format, search, replace);
		
//		try {
//			String[] replace = new String[] {suffix.get(), prefix.get(), groups, group.get(), healthbar, health, world, time, p.getName(), p.getDisplayName(), msg};
//		
//			for (int i=0; i<search.length; i++){
//				searchlist.add(search[i]);
//			}		
//			for (int i=0; i<replace.length; i++){
//				replacelist.add(replace[i]);
//			}
//			
//			// Convert back to arrays
//			search = (String[]) searchlist.toArray(new String[searchlist.size()]);
//			replace = (String[]) replacelist.toArray(new String[replacelist.size()]);
//
//			return replaceVars(format, search, replace);
//		} catch (InterruptedException ex) {
//			console.severe("Interrupt triggered while waiting on callable to return.");
//		} catch (ExecutionException ex) {
//			console.severe("Callable task threw an exception");
//			ex.getCause().printStackTrace();
//		}
//		return "";
		
	}
	
	/**
	 * Parse chat method for missing chat format
	 * @param p - Player object for chatting
	 * @param msg - Message to be formatted
	 * @return - New message format
	 */
	public String parseChat(Player p, String msg) {
		return parseChat(p, msg, this.chatFormat);
	}
    
    /**
     * Parse multigroup chat format
     * @param p - Player object for chatting
     * @param msg - Message to be formatted
	 * @param multigroupsFormat - The requested chat format string
	 * @return - replacement for +groups
     */
    public String parseGroups(Player p, String mgFormat){
        // Get all the groups a player is in
    	String[] groups = permissions.getUser(p).getGroupsNames();
        
        String output = "";
        HashMap<Integer, String> unparsedGroups = new HashMap<Integer, String>();
        int max = 0;
        int key = 0;
        
        // Iterate through each group a player is in and add it to the list
        for (String group : groups){
        	// Go through each track to check if the group is in the track
        	for (Track track : tracks){
        		// If track not meant for ordering purposes, skip
        		if (track.priority<1){
        			continue;
        		}
        		// Go through the groups in the track to see if they match the current player group
        		for (String trackgroup : track.groups){
        			if (trackgroup.equalsIgnoreCase(group)){
        				// Add it with the correct priority
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
        
        // Parse user defined variables in the group message
        String format = parseVars(mgFormat, p);
        
        // Add each group in the right order to the message
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
	        	
	        	// Replace the group variables
	        	String[] search = new String[] {"+suffix,+s", "+prefix,+p", "+group,+g"};
	        	String[] replace = new String[] { suffix, prefix, groupname};
	        	output = output + replaceVars(format, search, replace);
        	}
        }
        
		return output;
    }
	
    /**
     * Get the alias of a group, or return the group name if it doesn't have an alias
     * @param group - group to return alias for
     * @return
     */
	private String getAlias(String group) {
		if (aliases.containsKey(group)){
			return aliases.get(group);
		} else {
			return group;
		}
	}

	/**
	 * Return a health bar string.
	 * @param player - who the health bar should generate for
	 * @return - 
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
	
	/**
	 * Check whether the player has the given permissions.
	 * @param player - the player who is being checked
	 * @param perm - what permission to check for
	 * @return - Whether player has the permission, or is an op
	 */
	public boolean hasPerm(Player player, String perm) {
		if (player.hasPermission(perm)) {
			return true;
		} else {
			return player.isOp();
		}
	}
	
	/**
	 * Get the players prefix.
	 * @param player - who to get the prefix for
	 * @return - Player's prefix, direct or inherited
	 */
	public String getPrefix(Player player) {
		if (permissions != null) {
			return permissions.getUser(player).getPrefix(player.getWorld().getName());
		}
		console.severe("[ There is no Permissions module, why are we running?!??!?");
		return "";
	}
	
	/**
	 * Get the players suffix.
	 * @param player - who to get the suffix for
	 * @return - Player's suffix, direct or inherited
	 */
	public String getSuffix(Player player) {
		if (permissions != null) {
			return permissions.getUser(player).getSuffix(player.getWorld().getName());
		}
		console.severe("["+getDescription().getName()+"] There is no Permissions module, why are we running?!??!?");
		return "";
	}
	
	/**
	 * Get the group's prefix.
	 * @param group - group whose prefix to get
	 * @param worldname - what world the prefix should come from
	 * @return - Group's prefix
	 */
	public String getGroupPrefix(String group, String worldname) {
		if (permissions != null) {
			return permissions.getGroup(group).getPrefix(worldname);
		}
		console.severe("["+getDescription().getName()+"] There is no Permissions module, why are we running?!??!?");
		return null;
	}
	
	/**
	 * Get the group's suffix.
	 * @param group - group whose suffix to get
	 * @param worldname - what world the suffix should come from
	 * @return - Group's suffix
	 */
	public String getGroupSuffix(String group, String worldname) {
		if (permissions != null) {
			return permissions.getGroup(group).getSuffix(worldname);
		}
		console.severe("["+getDescription().getName()+"] There is no Permissions module, why are we running?!??!?");
		return null;
	}
	
	/**
	 * Get the players group
	 * @param player - who's group to get
	 * @return - Players primary group
	 */
	public String getGroup(Player player) {
		if (permissions != null) {
			String groups[] = permissions.getUser(player).getGroupsNames(player.getWorld().getName());
			return groups[0];
		}
		console.severe("["+getDescription().getName()+"] There is no Permissions module, why are we running?!??!?");
		return "";
	}
	
	/**
	 * Get a user/group specific variable. User takes priority
	 * @param player - who to get the variable for
	 * @param variable - what variable to look for
	 * @return - Value of variable in permission setup
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
		console.severe("["+getDescription().getName()+"] There is no Permissions module, why are we running?!??!?");
		return "";
	}

	/**
	 * Command Handler
	 */
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!command.getName().equalsIgnoreCase("pexchat")) return false;
		if (sender instanceof Player && !hasPerm((Player)sender, "pexchat.reload")) {
			sender.sendMessage("[PExChat] Permission Denied");
			return true;
		}
		if (args.length != 1) return false;
		if (args[0].equalsIgnoreCase("reload")) {
			aliases.clear();
			tracks.clear();
			loadConfig();
			sender.sendMessage("[PExChat] Config Reloaded");
			return true;
		}
		return false;
	}
}
