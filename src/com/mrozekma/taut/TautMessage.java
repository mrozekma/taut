package com.mrozekma.taut;

import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

//TODO Message subtypes: https://api.slack.com/events/message
public class TautMessage {
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

	final TautConnection conn;
	private final TautAbstractChannel channel;
	private final String text;
	private final WhoWhen current;
	private final Optional<WhoWhen> edited;
	private final TautAttachment[] attachments;

	private final boolean starred;
	private final TautChannel[] pins;
	private final TautReactionList reactions;

	public TautMessage(TautAbstractChannel channel, String text, WhoWhen current, Optional<WhoWhen> edited, TautAttachment[] attachments, boolean starred, TautChannel[] pins) {
		this.conn = channel.conn;
		this.channel = channel;
		this.text = text;
		this.current = current;
		this.edited = edited;
		this.attachments = attachments;

		this.starred = starred;
		this.pins = pins;

		this.reactions = new TautMessageReactionList(this);
	}

	public TautMessage(TautAbstractChannel channel, JSONObject json) {
		this.conn = channel.conn;
		this.channel = channel;
		this.text = json.getString("text");
		{
			final Optional<TautUser> user;
			if(json.has("user")) {
				user = Optional.of(new TautUser(this.conn, json.getString("user")));
			} else if(json.has("bot_id")) {
				user = Optional.of(new TautUser(this.conn, json.getString("bot_id")));
			} else {
				user = Optional.empty();
			}
			this.current = new WhoWhen(user, json.getString("ts"));
		}
		if(json.has("edited")) {
			final JSONObject edited = json.getJSONObject("edited");
			this.edited = Optional.of(new WhoWhen(edited.getOpt("user", (String user) -> new TautUser(this.conn, user)), edited.getString("ts")));
		} else {
			this.edited = Optional.empty();
		}
		this.attachments = json.<JSONObject>streamArray("attachments").map(attachment -> new TautAttachment(this.conn, attachment)).toArray(TautAttachment[]::new);

		this.starred = json.optBoolean("is_starred", false);
		this.pins = json.has("pinned_to") ? json.getJSONArray("pinned_to").<String>stream().map(id -> new TautChannel(this.conn, id)).toArray(TautChannel[]::new) : new TautChannel[0];

		this.reactions = new TautMessageReactionList(this);
	}

	public TautAbstractChannel getChannel() { return this.channel; }
	public String getText() { return this.text; }
	public WhoWhen getCurrent() { return this.current; }
	public Optional<WhoWhen> getEdited() { return this.edited; }
	public TautAttachment[] getAttachments() { return this.attachments; }
	public boolean getStarred() { return this.starred; }
	public TautChannel[] getPins() { return this.pins; }
	public TautReactionList getReactions() { return this.reactions; }

	public void delete() throws TautException {
		this.delete(true);
	}

	public void delete(boolean asUser) throws TautException {
		this.getChannel().post("chat.delete", new JSONObject().put("ts", this.getCurrent().getTs()).put("as_user", asUser));
	}

	public TautMessage update(TautMessageDraft newMessage) throws TautException {
		//TODO Commented out the arguments that aren't in the chat.update docs, but I suspect they do exist
		final JSONObject args = new JSONObject()
				.put("ts", this.getCurrent().getTs())
				.put("text", newMessage.getText())
				.put("parse", newMessage.getParse() ? "full" : "none")
				.put("link_names", newMessage.getLinkNames() ? 1 : 0)
//				.put("unfurl_links", newMessage.getUnfurlLinks())
//				.put("unfurl_media", newMessage.getUnfurlMedia())
				.putOpt("username", newMessage.getUsername())
				.put("as_user", newMessage.getAsUser())
//				.putOpt("icon_url", newMessage.getIconUrl())
//				.putOpt("icon_emoji", newMessage.getIconEmoji())
				.put("attachments", Arrays.stream(newMessage.getAttachments()).map(TautAttachment::toJSON).toArray(JSONObject[]::new))
				;
		final JSONObject res = this.getChannel().post("chat.update", args);
		return this.channel.messageByTs(res.getString("ts"));
	}

	public TautMessage update(String newText) throws TautException {
		return this.update(new TautMessageDraft(newText));
	}

	public void addReaction(String name) throws TautException {
		this.getReactions().add(name);
	}

	public void removeReaction(String name) throws TautException {
		this.getReactions().remove(name);
	}
}
