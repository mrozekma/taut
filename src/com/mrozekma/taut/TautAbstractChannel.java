package com.mrozekma.taut;

import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

public abstract class TautAbstractChannel extends LazyLoadedObject {
	protected TautAbstractChannel(TautConnection conn, String id) {
		super(conn, id);
	}

	protected TautAbstractChannel(TautConnection conn, JSONObject json) {
		super(conn, json);
	}

	public abstract boolean isDirect();

	protected abstract String getRoutePrefix();

	public Iterable<TautMessage> history() throws TautException {
		return this.history(100);
	}

	public Iterable<TautMessage> history(int count) throws TautException {
		return this.history(Optional.empty(), Optional.empty(), true, count, true);
	}

	public Iterable<TautMessage> history(int count, Date latest, Date oldest, boolean inclusive) throws TautException {
		return this.history(Optional.of(latest), Optional.of(oldest), inclusive, count, true);
	}

	public Iterable<TautMessage> history(Optional<Date> latest, Optional<Date> oldest, boolean inclusive, int count, boolean unreads) throws TautException {
		return new HistoryIterable(this, latest, oldest, inclusive, count, unreads);
	}

	public TautMessage messageByTs(String ts) throws TautException {
		final JSONObject req = new JSONObject()
			.put("channel", this.getId())
			.put("latest", ts)
			.put("oldest", ts)
			.put("inclusive", 1)
			.put("count", 1);
		final JSONObject res = this.conn.historyConnection.post(this.getRoutePrefix() + ".history", req);
		final Optional<JSONObject> message = res.<JSONObject>streamArray("messages").findAny();
		return new TautMessage(this, message.orElseThrow(() ->
			new TautException(String.format("No message in channel %s with timestamp %s", this.getId(), ts)))
		);
	}

	public TautMessage sendMessage(TautMessageDraft message) throws TautException {
		final JSONObject args = new JSONObject()
				.put("text", message.getText())
				.put("parse", message.getParse() ? "full" : "none")
				.put("link_names", message.getLinkNames() ? 1 : 0)
				.put("unfurl_links", message.getUnfurlLinks())
				.put("unfurl_media", message.getUnfurlMedia())
				.putOpt("username", message.getUsername())
				.put("as_user", message.getAsUser())
				.putOpt("icon_url", message.getIconUrl())
				.putOpt("icon_emoji", message.getIconEmoji())
				.put("attachments", Arrays.stream(message.getAttachments()).map(TautAttachment::toJSON).toArray(JSONObject[]::new))
				;
		final JSONObject res = this.post("chat.postMessage", args);
		return this.messageByTs(res.getString("ts"));
	}

	public TautMessage sendMessage(String text) throws TautException {
		return this.sendMessage(new TautMessageDraft(text));
	}

	public TautMessage sendMeMessage(String text) throws TautException {
		final JSONObject res = this.post("chat.meMessage", new JSONObject().put("text", text));
		return this.messageByTs(res.getString("ts"));
	}

	public TautMessage sendAttachment(TautAttachment attachment) throws TautException {
		return this.sendMessage(new TautMessageDraft("\n").setAttachments(attachment));
	}

	public TautFile uploadFile(TautFileUpload file) throws TautException {
		file.setChannels(this);
		return this.conn.uploadFile(file);
	}
}
