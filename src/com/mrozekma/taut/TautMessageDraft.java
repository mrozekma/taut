package com.mrozekma.taut;

import java.util.Optional;

public class TautMessageDraft {
	private String text;
	private boolean parse;
	private boolean linkNames;
	private boolean unfurlLinks;
	private boolean unfurlMedia;
	private boolean asUser;
	private Optional<String> username;
	private Optional<String> iconUrl;
	private Optional<String> iconEmoji;
	private TautAttachment[] attachments;

	private static Optional<String> defaultUsername = Optional.empty();
	private static Optional<String> defaultIconUrl = Optional.empty();
	private static Optional<String> defaultIconEmoji = Optional.empty();

	public TautMessageDraft(String text) {
		this(text, false, true, false, true, false);
	}

	public TautMessageDraft(String text, boolean parse, boolean linkNames, boolean unfurlLinks, boolean unfurlMedia, boolean asUser) {
		this.text = text;
		this.parse = parse;
		this.linkNames = linkNames;
		this.unfurlLinks = unfurlLinks;
		this.unfurlMedia = unfurlMedia;
		this.asUser = asUser;

		this.username = defaultUsername;
		this.iconUrl = defaultIconUrl;
		this.iconEmoji = defaultIconEmoji;
		this.attachments = new TautAttachment[0];
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
	public TautAttachment[] getAttachments() { return this.attachments; }

	public TautMessageDraft setText(String text) {
		this.text = text;
		return this;
	}

	public TautMessageDraft setParse(boolean parse) {
		this.parse = parse;
		return this;
	}

	public TautMessageDraft setLinkNames(boolean linkNames) {
		this.linkNames = linkNames;
		return this;
	}

	public TautMessageDraft setUnfurlLinks(boolean unfurlLinks) {
		this.unfurlLinks = unfurlLinks;
		return this;
	}

	public TautMessageDraft setUnfurlMedia(boolean unfurlMedia) {
		this.unfurlMedia = unfurlMedia;
		return this;
	}

	public TautMessageDraft setAsUser(boolean asUser) {
		this.asUser = asUser;
		if(asUser) {
			this.iconUrl = Optional.empty();
			this.iconEmoji = Optional.empty();
		}
		return this;
	}

	public TautMessageDraft setSender(String username, String icon) {
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

	public TautMessageDraft setAttachments(TautAttachment... attachments) {
		this.attachments = attachments;
		return this;
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
