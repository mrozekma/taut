package com.mrozekma.taut;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// https://api.slack.com/types/channel
public class TautChannel extends LazyLoadedObject {
	private String name;
	private long created;
	private TautUser creator;
	private boolean isArchived, isGeneral, isPrivate;
	private List<TautUser> members;
	private UserCreatedString topic, purpose;
	private boolean isMember;

	TautChannel(TautConnection conn, String id) {
		super(conn, id);
		if(!(id.startsWith("C") || id.startsWith("G"))) {
			throw new IllegalArgumentException("Invalid channel ID: " + id);
		}
	}

	TautChannel(TautConnection conn, JSONObject json) {
		super(conn, json);
	}

	public String getName() throws TautException { this.checkLoad(); return this.name; }
	public long getCreated() throws TautException { this.checkLoad(); return this.created; }
	public TautUser getCreator() throws TautException { this.checkLoad(); return this.creator; }
	public boolean isArchived() throws TautException { this.checkLoad(); return this.isArchived; }
	public boolean isGeneral() throws TautException { this.checkLoad(); return this.isGeneral; }
	public boolean isPrivate() throws TautException { this.checkLoad(); return this.isPrivate; }
	public List<TautUser> getMembers() throws TautException { this.checkLoad(); return this.members; }
	public UserCreatedString getTopic() throws TautException { this.checkLoad(); return this.topic; }
	public UserCreatedString getPurpose() throws TautException { this.checkLoad(); return this.purpose; }
	public boolean isMember() throws TautException { this.checkLoad(); return this.isMember; }

	public Date getCreatedDate() throws TautException {
		return TautConnection.tsApiToHost(this.getCreated());
	}

	@Override protected JSONObject load() throws TautException {
		switch(this.getId().charAt(0)) {
		case 'C':
			return this.post("channels.info").getJSONObject("channel");
		case 'G':
			return this.post("groups.info").getJSONObject("group");
		default:
			throw new IllegalStateException("Invalid channel ID: " + this.getId());
		}
	}

	@Override protected void populate(JSONObject json) {
		this.name = json.getString("name");
		this.created = json.getLong("created");
		this.creator = this.conn.getUserById(json.getString("creator"));
		this.isArchived = json.getBoolean("is_archived");
		this.isGeneral = json.optBoolean("is_general", false);
		this.isPrivate = json.optBoolean("is_group", false);
		this.members = json.getJSONArray("members").<String>stream().map(this.conn::getUserById).collect(Collectors.toList());
		this.topic = new UserCreatedString(this.conn, json.getJSONObject("topic"));
		this.purpose = new UserCreatedString(this.conn, json.getJSONObject("purpose"));
		if(json.has("is_member")) {
			this.isMember = json.getBoolean("is_member");
		} else { // Groups don't have is_member. I think being able to see the group at all means we must be a member, but I check anyway
			this.isMember = this.members.contains(this.conn.getSelf());
		}
	}

	JSONObject post(String route) throws TautException {
		return this.post(route, new JSONObject());
	}

	JSONObject post(String route, JSONObject args) throws TautException {
		if(route.startsWith(".")) {
			switch(this.getId().charAt(0)) {
			case 'C':
				route = "channels" + route;
				break;
			case 'G':
				route = "groups" + route;
				break;
			default:
				throw new IllegalStateException("Invalid channel ID: " + this.getId());
			}
		}
		args.put("channel", this.getId());
		return this.conn.post(route, args);
	}

	public void archive() throws TautException {
		this.post(".archive");
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
		this.post(".invite", new JSONObject().put("user", user.getId()));
	}

	public void join() throws TautException {
		// Why does this take a name instead of an ID
		this.post(".join", new JSONObject().put("name", this.getName()));
	}

	public void kick(TautUser user) throws TautException {
		this.post(".kick", new JSONObject().put("user", user.getId()));
	}

	public void leave() throws TautException {
		this.post(".leave");
	}

	public void markRead(Date ts) throws TautException {
		this.post(".mark", new JSONObject().put("ts", ts));
	}

	public void rename(String name) throws TautException {
		this.post(".rename", new JSONObject().put("name", name));
	}

	public void setPurpose(String purpose) throws TautException {
		this.post(".setPurpose", new JSONObject().put("purpose", purpose));
	}

	public void setTopic(String topic) throws TautException {
		this.post(".setTopic", new JSONObject().put("topic", topic));
	}

	public void unarchive() throws TautException {
		this.post(".unarchive");
	}

	public TautMessage sendMessage(TautMessage message) throws TautException {
		final JSONObject args = new JSONObject()
				.put("text", message.getText())
				.put("parse", message.getParse() ? "full" : "none")
				.put("link_names", message.getLinkNames() ? 1 : 0)
				.put("unfurl_links", message.getUnfurlLinks())
				.put("unfurl_media", message.getUnfurlMedia())
				.putOpt("username", message.getUsername())
				.put("as_user", message.getAsUser())
				.putOpt("icon_url", message.getIconUrl())
				.putOpt("icon_emoji", message.getIconEmoji());
		final JSONObject res = this.post("chat.postMessage", args);
		message.setSentTs(res.getString("ts"));
		message.setSentChannel(this);
		return message;
	}

	public TautMessage sendMessage(String text) throws TautException {
		return this.sendMessage(new TautMessage(text));
	}

	public TautMessage sendMeMessage(String text) throws TautException {
		final JSONObject res = this.post("chat.meMessage", new JSONObject().put("text", text));
		final TautMessage message = new TautMessage(text);
		message.setSentTs(res.getString("ts"));
		message.setSentChannel(this);
		return message;
	}

	public FileIterable iterFiles() throws TautException {
		return this.conn.iterFiles(this);
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
		for(TautChannel channel : getAllPrivate(conn)) {
			if(channel.getName().equalsIgnoreCase(name)) {
				return channel;
			}
		}
		throw new TautException(String.format("Channel `%s' not found", name));
	}

	public static List<TautChannel> getAll(TautConnection conn) throws TautException {
		return conn.post("channels.list").<JSONObject>streamArray("channels").map(json -> new TautChannel(conn, json)).collect(Collectors.toList());
	}

	public static List<TautChannel> getAllPrivate(TautConnection conn) throws TautException {
		return conn.post("groups.list").<JSONObject>streamArray("groups").map(json -> new TautChannel(conn, json)).collect(Collectors.toList());
	}

	public static TautChannel create(TautConnection conn, String name) throws TautException {
		if(name.startsWith("#")) {
			name = name.substring(1);
		}
		final JSONObject res = conn.post("channels.create", new JSONObject().put("name", name));
		return new TautChannel(conn, res.getJSONObject("channel"));
	}
}
