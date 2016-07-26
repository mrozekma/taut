package com.mrozekma.taut;

import java.util.Date;
import java.util.Optional;

public abstract class TautAbstractChannel extends LazyLoadedObject {
	protected TautAbstractChannel(TautConnection conn, String id) {
		super(conn, id);
	}

	protected TautAbstractChannel(TautConnection conn, JSONObject json) {
		super(conn, json);
	}

	protected abstract String getRoutePrefix();

	public Iterable<TautReceivedMessage> history() throws TautException {
		return this.history(100);
	}

	public Iterable<TautReceivedMessage> history(int count) throws TautException {
		return this.history(Optional.empty(), Optional.empty(), true, count, true);
	}

	public Iterable<TautReceivedMessage> history(int count, Date latest, Date oldest, boolean inclusive) throws TautException {
		return this.history(Optional.of(latest), Optional.of(oldest), inclusive, count, true);
	}

	public Iterable<TautReceivedMessage> history(Optional<Date> latest, Optional<Date> oldest, boolean inclusive, int count, boolean unreads) throws TautException {
		return new HistoryIterable(this, latest, oldest, inclusive, count, unreads);
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

	public TautFile uploadFile(TautFileUpload file) throws TautException {
		file.setChannels(this);
		return this.conn.uploadFile(file);
	}
}
