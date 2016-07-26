package com.mrozekma.taut;

import java.util.Optional;

public class TautMessage {
	private String text;
	private boolean parse;
	private boolean linkNames;
	private boolean unfurlLinks;
	private boolean unfurlMedia;
	private boolean asUser;
	private Optional<String> username;
	private Optional<String> iconUrl;
	private Optional<String> iconEmoji;

	private Optional<String> sentTs = Optional.empty();
	private Optional<TautAbstractChannel> sentChannel = Optional.empty();

	private static Optional<String> defaultUsername = Optional.empty();
	private static Optional<String> defaultIconUrl = Optional.empty();
	private static Optional<String> defaultIconEmoji = Optional.empty();

	public TautMessage(String text) {
		this(text, false, true, false, true, false);
	}

	public TautMessage(String text, boolean parse, boolean linkNames, boolean unfurlLinks, boolean unfurlMedia, boolean asUser) {
		this.text = text;
		this.parse = parse;
		this.linkNames = linkNames;
		this.unfurlLinks = unfurlLinks;
		this.unfurlMedia = unfurlMedia;
		this.asUser = asUser;

		this.username = defaultUsername;
		this.iconUrl = defaultIconUrl;
		this.iconEmoji = defaultIconEmoji;
	}

	public String getText() { return this.text; }
	public boolean getParse() { return this.parse; }
	public boolean getLinkNames() { return this.linkNames; }
	public boolean getUnfurlLinks() { return this.unfurlLinks; }
	public boolean getUnfurlMedia() { return this.unfurlMedia; }
	public boolean getAsUser() { return this.asUser; }
	public Optional<String> getUsername() { return this.username; }
	public Optional<String> getIconUrl() { return this.iconUrl; }
	public Optional<String> getIconEmoji() { return this.iconEmoji; }

	public TautMessage setText(String text) {
		this.text = text;
		return this;
	}

	public TautMessage setParse(boolean parse) {
		this.parse = parse;
		return this;
	}

	public TautMessage setLinkNames(boolean linkNames) {
		this.linkNames = linkNames;
		return this;
	}

	public TautMessage setUnfurlLinks(boolean unfurlLinks) {
		this.unfurlLinks = unfurlLinks;
		return this;
	}

	public TautMessage setUnfurlMedia(boolean unfurlMedia) {
		this.unfurlMedia = unfurlMedia;
		return this;
	}

	public TautMessage setAsUser(boolean asUser) {
		this.asUser = asUser;
		if(asUser) {
			this.iconUrl = Optional.empty();
			this.iconEmoji = Optional.empty();
		}
		return this;
	}

	public TautMessage setSender(String username, String icon) {
		this.asUser = false;
		this.username = Optional.of(username);
		if(icon.startsWith(":")) {
			this.iconUrl = Optional.empty();
			this.iconEmoji = Optional.of(icon);
		} else {
			this.iconUrl = Optional.of(icon);
			this.iconEmoji = Optional.empty();
		}
		return this;
	}

	Optional<String> getSentTs() { return this.sentTs; }

	TautMessage setSentTs(String ts) {
		this.sentTs = Optional.of(ts);
		return this;
	}

	Optional<TautAbstractChannel> getSentChannel() { return this.sentChannel; }

	TautMessage setSentChannel(TautAbstractChannel sentChannel) {
		this.sentChannel = Optional.of(sentChannel);
		return this;
	}

	public void delete() throws TautException {
		this.delete(true);
	}

	public void delete(boolean asUser) throws TautException {
		if(!(this.sentTs.isPresent() && this.sentChannel.isPresent())) {
			throw new TautException("Message has not been sent");
		}
		this.sentChannel.get().post("chat.delete", new JSONObject().put("ts", this.sentTs.get()).put("as_user", asUser));
		this.sentTs = Optional.empty();
		this.sentChannel = Optional.empty();
	}

	public void update(TautMessage newMessage) throws TautException {
		if(!(this.sentTs.isPresent() && this.sentChannel.isPresent())) {
			throw new TautException("Message has not been sent");
		}
		//TODO Commented out the arguments that aren't in the chat.update docs, but I suspect they do exist
		final JSONObject args = new JSONObject()
				.put("ts", this.getSentTs().get())
				.put("text", newMessage.getText())
				.put("parse", newMessage.getParse() ? "full" : "none")
				.put("link_names", newMessage.getLinkNames() ? 1 : 0)
//				.put("unfurl_links", msg.getUnfurlLinks())
//				.put("unfurl_media", msg.getUnfurlMedia())
				.putOpt("username", newMessage.getUsername())
				.put("as_user", newMessage.getAsUser())
//				.putOpt("icon_url", msg.getIconUrl())
//				.putOpt("icon_emoji", msg.getIconEmoji())
				;
		final JSONObject res = this.getSentChannel().get().post("chat.update", args);
		this.setSentTs(res.getString("ts"));
	}

	public void update(String newText) throws TautException {
		this.update(new TautMessage(newText));
	}

	// This interface is possibly terrible. Shrug
	public static void setDefaultSender(String username, String icon) {
		defaultUsername = Optional.of(username);
		if(icon.startsWith(":")) {
			defaultIconUrl = Optional.empty();
			defaultIconEmoji = Optional.of(icon);
		} else {
			defaultIconUrl = Optional.of(icon);
			defaultIconEmoji = Optional.empty();
		}
	}
}
