package com.mrozekma.taut;

import java.util.Date;
import java.util.Optional;

//TODO Message subtypes: https://api.slack.com/events/message
public class TautReceivedMessage {
	public static class WhoWhen {
		private final Optional<TautUser> user;
		private final String ts;

		WhoWhen(Optional<TautUser> user, String ts) {
			this.user = user;
			this.ts = ts;
		}

		public Optional<TautUser> getUser() { return this.user; }
		public String getTs() { return this.ts; }

		public Date getTsDate() {
			return TautConnection.tsApiToHost(this.ts);
		}
	}

	private final TautConnection conn;
	private final TautAbstractChannel channel;
	private final String text;
	private final WhoWhen current;
	private final Optional<WhoWhen> edited;
	private final TautAttachment[] attachments;

	private final boolean starred;
	private final TautChannel[] pins;
	private final TautReaction[] reactions;

	public TautReceivedMessage(TautAbstractChannel channel, String text, WhoWhen current, Optional<WhoWhen> edited, TautAttachment[] attachments, boolean starred, TautChannel[] pins, TautReaction[] reactions) {
		this.conn = channel.conn;
		this.channel = channel;
		this.text = text;
		this.current = current;
		this.edited = edited;
		this.attachments = attachments;

		this.starred = starred;
		this.pins = pins;
		this.reactions = reactions;
	}

	public TautReceivedMessage(TautAbstractChannel channel, JSONObject json) {
		this.conn = channel.conn;
		this.channel = channel;
		this.text = json.getString("text");
		this.current = new WhoWhen(json.getOpt("user", (String user) -> new TautUser(this.conn, user)), json.getString("ts"));
		if(json.has("edited")) {
			final JSONObject edited = json.getJSONObject("edited");
			this.edited = Optional.of(new WhoWhen(edited.getOpt("user", (String user) -> new TautUser(this.conn, user)), edited.getString("ts")));
		} else {
			this.edited = Optional.empty();
		}
		this.attachments = json.<JSONObject>streamArray("attachments").map(attachment -> new TautAttachment(this.conn, attachment)).toArray(TautAttachment[]::new);

		this.starred = json.optBoolean("is_starred", false);
		this.pins = json.has("pinned_to") ? json.getJSONArray("pinned_to").<String>stream().map(id -> new TautChannel(this.conn, id)).toArray(TautChannel[]::new) : new TautChannel[0];
		this.reactions = json.has("reactions") ? json.getJSONArray("reactions").<JSONObject>stream().map(js -> new TautReaction(this.conn, js)).toArray(TautReaction[]::new) : new TautReaction[0];
	}

	public TautAbstractChannel getChannel() { return this.channel; }
	public String getText() { return this.text; }
	public WhoWhen getCurrent() { return this.current; }
	public Optional<WhoWhen> getEdited() { return this.edited; }
	public TautAttachment[] getAttachments() { return this.attachments; }
	public boolean getStarred() { return this.starred; }
	public TautChannel[] getPins() { return this.pins; }
	public TautReaction[] getReactions() { return this.reactions; }
}
