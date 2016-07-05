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
	private final TautChannel channel;
	private final String text;
	private final WhoWhen current;
	private final Optional<WhoWhen> edited;
	private final TautAttachment[] attachments;

	public TautReceivedMessage(TautChannel channel, String text, WhoWhen current, Optional<WhoWhen> edited, TautAttachment[] attachments) {
		this.conn = channel.conn;
		this.channel = channel;
		this.text = text;
		this.current = current;
		this.edited = edited;
		this.attachments = attachments;
	}

	public TautReceivedMessage(TautChannel channel, JSONObject json) {
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
		this.attachments = json.streamObjectArray("attachments").map(attachment -> new TautAttachment(this.conn, attachment)).toArray(TautAttachment[]::new);
	}

	public TautChannel getChannel() { return this.channel; }
	public String getText() { return this.text; }
	public WhoWhen getCurrent() { return this.current; }
	public Optional<WhoWhen> getEdited() { return this.edited; }
	public TautAttachment[] getAttachments() { return this.attachments; }
}
