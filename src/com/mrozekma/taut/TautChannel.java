package com.mrozekma.taut;

import java.util.Date;
import java.util.List;
import java.util.Optional;
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

	@Override protected JSONObject load() throws TautException {
		return this.post("channels.info").getJSONObject("channel");
	}

	@Override protected void populate(JSONObject json) {
		this.name = json.getString("name");
		this.created = TautConnection.tsApiToHost(json.getLong("created"));
		this.creator = this.conn.getUserById(json.getString("creator"));
		this.isArchived = json.getBoolean("is_archived");
		this.isGeneral = json.getBoolean("is_general");
		this.members = json.getJSONArray("members").<String>stream().map(this.conn::getUserById).collect(Collectors.toList());
		this.topic = new UserCreatedString(this.conn, json.getJSONObject("topic"));
		this.purpose = new UserCreatedString(this.conn, json.getJSONObject("purpose"));
		this.isMember = json.getBoolean("is_member");
	}

	JSONObject post(String route) throws TautException {
		return this.post(route, new JSONObject());
	}

	JSONObject post(String route, JSONObject args) throws TautException {
		args.put("channel", this.getId());
		return this.conn.post(route, args);
	}

	public void archive() throws TautException {
		this.post("channels.archive");
	}

	public Iterable<TautReceivedMessage> history() throws TautException {
		return this.history(100);
	}

	public Iterable<TautReceivedMessage> history(int count) throws TautException {
		return this.history(Optional.empty(), Optional.empty(), true, count, true);
	}

	public Iterable<TautReceivedMessage> history(int count, Date latest, Date oldest, boolean inclusive) throws TautException {
		return this.history(Optional.of(latest), Optional.of(oldest), inclusive, count, true);
	}

	private Iterable<TautReceivedMessage> history(Optional<Date> latest, Optional<Date> oldest, boolean inclusive, int count, boolean unreads) throws TautException {
		return new HistoryIterable(this, latest, oldest, inclusive, count, unreads);
	}

	public void invite(TautUser user) throws TautException {
		this.post("channels.invite", new JSONObject().put("user", user.getId()));
	}

	public void join() throws TautException {
		// Why does this take a name instead of an ID
		this.post("channels.join", new JSONObject().put("name", this.getName()));
	}

	public void kick(TautUser user) throws TautException {
		this.post("channels.kick", new JSONObject().put("user", user.getId()));
	}

	public void leave() throws TautException {
		this.post("channels.leave");
	}

	public void rename(String name) throws TautException {
		this.post("channels.rename", new JSONObject().put("name", name));
	}

	public void unarchive() throws TautException {
		this.post("channels.unarchive");
	}

	public TautMessage sendMessage(TautMessage msg) throws TautException {
		final JSONObject args = new JSONObject()
				.put("channel", this.getId())
				.put("text", msg.getText())
				.put("parse", msg.getParse() ? "full" : "none")
				.put("link_names", msg.getLinkNames() ? 1 : 0) // Why is this randomly a number, Slack?
				.put("unfurl_links", msg.getUnfurlLinks())
				.put("unfurl_media", msg.getUnfurlMedia())
				.putOpt("username", msg.getUsername())
				.put("as_user", msg.getAsUser())
				.putOpt("icon_url", msg.getIconUrl())
				.putOpt("icon_emoji", msg.getIconEmoji());
		final JSONObject res = this.conn.post("chat.postMessage", args);
		msg.setSentTs(TautConnection.tsApiToHost(res.getDouble("ts")));
		return msg;
	}

	public TautMessage sendMessage(String message) throws TautException {
		return this.sendMessage(new TautMessage(message));
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
		return conn.post("channels.list").streamObjectArray("channels").map(json -> new TautChannel(conn, json)).collect(Collectors.toList());
	}

	public static TautChannel create(TautConnection conn, String name) throws TautException {
		if(name.startsWith("#")) {
			name = name.substring(1);
		}
		final JSONObject res = conn.post("channels.create", new JSONObject().put("name", name));
		return new TautChannel(conn, res.getJSONObject("channel"));
	}
}
