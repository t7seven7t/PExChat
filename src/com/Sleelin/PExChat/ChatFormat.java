package com.Sleelin.PExChat;

public enum ChatFormat {
	BLACK(0, "0"),
	DARK_BLUE(1, "1"),
	DARK_GREEN(2, "2"),
	DARK_AQUA(3, "3"),
	DARK_RED(4, "4"),
	DARK_PURPLE(5, "5"),
	GOLD(6, "6"),
	GRAY(7, "7"),
	DARK_GRAY(8, "8"),
	BLUE(9, "9"),
	GREEN(10, "a"),
	AQUA(11, "b"),
	RED(12, "c"),
	LIGHT_PURPLE(13, "d"),
	YELLOW(14, "e"),
	WHITE(15, "f"),
	MAGIC(16, "k"),
	BOLD(17, "l"),
	STRIKETHROUGH(18, "m"),
	UNDERLINE(19, "n"),
	ITALIC(20, "o"),
	RESET(21, "r");
	
	private static ChatFormat[] byId = new ChatFormat[22];
	
	private int id;
	private String ch;
	
	ChatFormat(final int id, final String ch) {
		this.id = id;
		this.ch = ch;
	}
	
	public static String getChar(int id) {
		if (byId.length > id) {
			return byId[id].ch;
		} else
			return null;
	}
	
	static {
		for (ChatFormat c : values()) {
			if (byId.length > c.id) {
				byId[c.id]= c; 
			}
		}
	}
}
