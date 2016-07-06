package com.mrozekma.taut;

public class TautReaction {
	private final String name;
	private final int count;
	private final TautUser[] users;

	TautReaction(String name, int count, TautUser[] users) {
		this.name = name;
		this.count = count;
		this.users = users;
	}

	TautReaction(TautConnection conn, JSONObject json) {
		this.name = json.getString("name");
		this.count = json.getInt("count");
		this.users = json.getJSONArray("users").<String>stream().map(id -> new TautUser(conn, id)).toArray(TautUser[]::new);
	}

	public String getName() { return this.name; }
	public int getCount() { return this.count; }
	public TautUser[] getUsers() { return this.users; }
}
