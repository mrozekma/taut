package com.mrozekma.taut;

import java.awt.*;
import java.util.Optional;

public class TautAttachment {
	static class Field {
		private final String title, value;
		private final boolean isShort;

		private Field(String title, String value, boolean isShort) {
			this.title = title;
			this.value = value;
			this.isShort = isShort;
		}
	}

	private final TautConnection conn;
	private Optional<String> fallback, pretext, authorName, authorLink, authorIcon, title, titleLink, text, imageUrl, thumbUrl, footer, footerIcon;
	private Optional<Color> color;
	private Field[] fields;
	private Optional<Integer> ts;

	public TautAttachment(TautConnection conn) {
		this(conn, "", Color.BLACK, "", "", "", "", "", "", "", new Field[0], "", "", "", "", 0);
	}

	public TautAttachment(TautConnection conn, String fallback, Color color, String pretext, String authorName, String authorLink, String authorIcon, String title, String titleLink, String text, Field[] fields, String imageUrl, String thumbUrl, String footer, String footerIcon, int ts) {
		this.conn = conn;
		this.fallback = Optional.of(fallback);
		this.color = Optional.of(color);
		this.pretext = Optional.of(pretext);
		this.authorName = Optional.of(authorName);
		this.authorLink = Optional.of(authorLink);
		this.authorIcon = Optional.of(authorIcon);
		this.title = Optional.of(title);
		this.titleLink = Optional.of(titleLink);
		this.text = Optional.of(text);
		this.fields = fields;
		this.imageUrl = Optional.of(imageUrl);
		this.thumbUrl = Optional.of(thumbUrl);
		this.footer = Optional.of(footer);
		this.footerIcon = Optional.of(footerIcon);
		this.ts = Optional.of(ts);
	}

	public TautAttachment(TautConnection conn, JSONObject json) {
		this.conn = conn;
		this.fallback = json.getOpt("fallback");
		this.color = json.getOpt("color", (String clr) -> TautConnection.colorApiToHost(clr));
		this.pretext = json.getOpt("pretext");
		this.authorName = json.getOpt("author_name");
		this.authorLink = json.getOpt("author_link");
		this.authorIcon = json.getOpt("author_icon");
		this.title = json.getOpt("title");
		this.titleLink = json.getOpt("title_link");
		this.text = json.getOpt("text");
		this.fields = json.streamObjectArray("fields").map(field -> new Field(field.getString("title"), field.getString("value"), field.getBoolean("short"))).toArray(Field[]::new);
		this.imageUrl = json.getOpt("image_url");
		this.thumbUrl = json.getOpt("thumb_url");
		this.footer = json.getOpt("footer");
		this.footerIcon = json.getOpt("footer_icon");
		this.ts = json.has("ts") ? Optional.of(json.getInt("ts")) : Optional.empty();
	}

	public Optional<String> getFallback() { return this.fallback; }
	public Optional<String> getPretext() { return this.pretext; }
	public Optional<String> getAuthorName() { return this.authorName; }
	public Optional<String> getAuthorLink() { return this.authorLink; }
	public Optional<String> getAuthorIcon() { return this.authorIcon; }
	public Optional<String> getTitle() { return this.title; }
	public Optional<String> getTitleLink() { return this.titleLink; }
	public Optional<String> getText() { return this.text; }
	public Optional<String> getImageUrl() { return this.imageUrl; }
	public Optional<String> getThumbUrl() { return this.thumbUrl; }
	public Optional<String> getFooter() { return this.footer; }
	public Optional<String> getFooterIcon() { return this.footerIcon; }
	public Optional<Color> getColor() { return this.color; }
	public Field[] getFields() { return this.fields; }
	public Optional<Integer> getTs() { return this.ts; }

	public TautAttachment setFallback(String fallback) {
		this.fallback = Optional.of(fallback);
		return this;
	}

	public TautAttachment setPretext(String pretext) {
		this.pretext = Optional.of(pretext);
		return this;
	}

	public TautAttachment setAuthorName(String authorName) {
		this.authorName = Optional.of(authorName);
		return this;
	}

	public TautAttachment setAuthorLink(String authorLink) {
		this.authorLink = Optional.of(authorLink);
		return this;
	}

	public TautAttachment setAuthorIcon(String authorIcon) {
		this.authorIcon = Optional.of(authorIcon);
		return this;
	}

	public TautAttachment setTitle(String title) {
		this.title = Optional.of(title);
		return this;
	}

	public TautAttachment setTitleLink(String titleLink) {
		this.titleLink = Optional.of(titleLink);
		return this;
	}

	public TautAttachment setText(String text) {
		this.text = Optional.of(text);
		return this;
	}

	public TautAttachment setImageUrl(String imageUrl) {
		this.imageUrl = Optional.of(imageUrl);
		return this;
	}

	public TautAttachment setThumbUrl(String thumbUrl) {
		this.thumbUrl = Optional.of(thumbUrl);
		return this;
	}

	public TautAttachment setFooter(String footer) {
		this.footer = Optional.of(footer);
		return this;
	}

	public TautAttachment setFooterIcon(String footerIcon) {
		this.footerIcon = Optional.of(footerIcon);
		return this;
	}

	public TautAttachment setColor(Color color) {
		this.color = Optional.of(color);
		return this;
	}

	public TautAttachment setFields(Field[] fields) {
		this.fields = fields;
		return this;
	}

	public TautAttachment setTs(int ts) {
		this.ts = Optional.of(ts);
		return this;
	}
}
