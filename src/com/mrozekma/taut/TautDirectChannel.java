package com.mrozekma.taut;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TautDirectChannel extends TautAbstractChannel {
	// This interface is a bit odd.
	// It doesn't seem to be possible to load an im object via ID; the only option is im.open, which takes the user's ID
	// Thus, we require a user to construct, not the ID
	private TautUser user;

	private long created;
	private String lastRead;
	private Optional<TautReceivedMessage> latest;
	private int unreadCount, unreadCountDisplay;
//	private boolean isIm, isOpen; // Don't see the purpose of these

	TautDirectChannel(TautConnection conn, JSONObject json) {
		super(conn, json);
	}

	TautDirectChannel(TautUser user) throws TautException {
		this(user.conn, user.conn.post("im.open", new JSONObject().put("user", user.getId()).put("return_im", true)).getJSONObject("channel"));
	}

	public TautUser getUser() throws TautException { this.checkLoad(); return this.user; }
	public long getCreated() throws TautException { this.checkLoad(); return this.created; }
	public String getLastRead() throws TautException { this.checkLoad(); return this.lastRead; }
	public Optional<TautReceivedMessage> getLatest() throws TautException { this.checkLoad(); return this.latest; }
	public int getUnreadCount() throws TautException { this.checkLoad(); return this.unreadCount; }
	public int getUnreadCountDisplay() throws TautException { this.checkLoad(); return this.unreadCountDisplay; }

	public Date getCreatedDate() throws TautException {
		return TautConnection.tsApiToHost(this.getCreated());
	}

	@Override protected JSONObject load() throws TautException {
		throw new UnsupportedOperationException();
	}

	@Override protected void populate(JSONObject json) {
		this.user = this.conn.getUserById(json.getString("user"));
		this.created = json.getLong("created");
		this.lastRead = json.getString("last_read");
		this.latest = json.isNull("latest") ? Optional.empty() : Optional.of(new TautReceivedMessage(this, json.getJSONObject("latest")));
		this.unreadCount = json.getInt("unread_count");
		this.unreadCountDisplay = json.getInt("unread_count_display");
	}

	@Override protected void prepJSONObjectForPost(JSONObject args) {
		args.put("channel", this.getId());
	}

	@Override protected String getRoutePrefix() {
		return "im";
	}

	public void close() throws TautException {
		this.post("im.close");
	}

	public static List<TautDirectChannel> getAll(TautConnection conn) throws TautException {
		// The data that comes back from im.list is missing many of the fields from im.info, so we just lazy load by ID instead of populating now
		final JSONObject res = conn.post("im.list");
		final JSONArray ims = res.getJSONArray("ims");
		final List<TautDirectChannel> rtn = new LinkedList<>();
		for(int i = 0; i < ims.length(); i++) {
			final JSONObject json = ims.getJSONObject(i);
			if(json.optBoolean("is_im", true) && !json.optBoolean("is_user_deleted", false)) {
				rtn.add(new TautDirectChannel(new TautUser(conn, json.getString("user"))));
			}
		}
		return rtn;
	}
}
