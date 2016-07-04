package com.mrozekma.taut;

import java.util.Date;
import java.util.Optional;

public class TautMessage {
	private String text;
	private boolean parse;
	private boolean linkNames;
	private boolean unfurlLinks;
	private boolean unfurlMedia;
	private boolean asUser;
	private Optional<String> username = Optional.empty();
	private Optional<String> iconUrl = Optional.empty();
	private Optional<String> iconEmoji = Optional.empty();

	private Optional<Date> sentTs = Optional.empty();

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

	Optional<Date> getSentTs() { return this.sentTs; }

	TautMessage setSentTs(Date ts) {
		this.sentTs = Optional.of(ts);
		return this;
	}
}
