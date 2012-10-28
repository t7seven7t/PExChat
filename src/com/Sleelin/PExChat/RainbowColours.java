package com.Sleelin.PExChat;

public enum RainbowColours {
	RED("c", 0),
	GOLD("6", 1),
	YELLOW("e", 2),
	GREEN("a", 3),
	BLUE("b", 4),
	PINK("d", 5),
	PURPLE("5", 6);

	private static RainbowColours[] byId = new RainbowColours[7];
	
	RainbowColours(final String v, final int id) {
		this.v = v;
		this.id = id;
	}
	
	private String v;
	private int id;
	
	public String getV() {
		return this.v;
	}
	
	public static String getColour(final int id) {
		if (byId.length > id) {
			return byId[id].getV();
		} else
			return null;
	}
	
	static {
		for (RainbowColours r : values()) {
			if (byId.length > r.id) {
				byId[r.id] = r;
			}
		}
	}
}
