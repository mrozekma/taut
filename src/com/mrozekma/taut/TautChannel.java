package com.mrozekma.taut;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

// https://api.slack.com/types/channel
public class TautChannel extends LazyLoadedObject {
	private String name;
	private Date created;
	private TautUser creator;
	private boolean isArchived, isGeneral;
	private List<TautUser> members;
	private UserCreatedString topic, purpose;
	private boolean isMember;

	TautChannel(TautConnection conn, String id) {
		super(conn, id);
		if(!id.startsWith("C")) {
			throw new IllegalArgumentException("Invalid channel ID: " + id);
		}
	}

	TautChannel(TautConnection conn, JSONObject json) {
		super(conn, json);
	}

	public String getName() throws TautException { this.checkLoad(); return this.name; }
	public Date getCreated() throws TautException { this.checkLoad(); return this.created; }
	public TautUser getCreator() throws TautException { this.checkLoad(); return this.creator; }
	public boolean isArchived() throws TautException { this.checkLoad(); return this.isArchived; }
	public boolean isGeneral() throws TautException { this.checkLoad(); return this.isGeneral; }
	public List<TautUser> getMembers() throws TautException { this.checkLoad(); return this.members; }
	public UserCreatedString getTopic() throws TautException { this.checkLoad(); return this.topic; }
	public UserCreatedString getPurpose() throws TautException { this.checkLoad(); return this.purpose; }
	public boolean isMember() throws TautException { this.checkLoad(); return this.isMember; }

	@Override protected JSONObject load(String id) throws TautException {
		return this.conn.post("channels.info", new JSONObject().put("channel", id)).getJSONObject("channel");
	}

	@Override protected void populate(JSONObject json) {
		this.name = json.getString("name");
		this.created = this.conn.resolveDate(json.getLong("created"));
		this.creator = this.conn.getUserById(json.getString("creator"));
		this.isArchived = json.getBoolean("is_archived");
		this.isGeneral = json.getBoolean("is_general");
		this.members = json.getJSONArray("members").<String>stream().map(this.conn::getUserById).collect(Collectors.toList());
		this.topic = new UserCreatedString(this.conn, json.getJSONObject("topic"));
		this.purpose = new UserCreatedString(this.conn, json.getJSONObject("purpose"));
		this.isMember = json.getBoolean("is_member");
	}

	public static TautChannel getById(TautConnection conn, String id) {
		return new TautChannel(conn, id);
	}

	public static TautChannel getByName(TautConnection conn, String name) throws TautException {
		if(name.startsWith("#")) {
			name = name.substring(1);
		}
		for(TautChannel channel : getAll(conn)) {
			if(channel.getName().equalsIgnoreCase(name)) {
				return channel;
			}
		}
		throw new TautException(String.format("Channel `%s' not found", name));
	}

	public static List<TautChannel> getAll(TautConnection conn) throws TautException {
		return conn.post("channels.list").getJSONArray("channels").<JSONObject>stream().map(json -> new TautChannel(conn, json)).collect(Collectors.toList());
	}
}
