package com.mrozekma.taut;

public class TautReaction {
	private final TautReactionList list;
	private final TautConnection conn;

	private final String name;
	private final int count;
	private final TautUser[] users;

	/*
	TautReaction(String name, int count, TautUser[] users) {
		this.name = name;
		this.count = count;
		this.users = users;
	}
	*/

	TautReaction(TautReactionList list, JSONObject json) {
		this.list = list;
		this.conn = this.list.conn;

		this.name = json.getString("name");
		this.count = json.getInt("count");
		this.users = json.getJSONArray("users").<String>stream().map(id -> new TautUser(this.conn, id)).toArray(TautUser[]::new);
	}

	public String getName() { return this.name; }
	public int getCount() { return this.count; }
	public TautUser[] getUsers() { return this.users; }
}
