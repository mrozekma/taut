package com.mrozekma.taut;

import java.awt.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class TautAttachment {
	static class Field {
		private final String title, value;
		private final boolean isShort;

		private Field(JSONObject json) {
			this(json.getString("title"), json.getString("value"), json.getBoolean("short"));
		}

		private Field(String title, String value, boolean isShort) {
			this.title = title;
			this.value = value;
			this.isShort = isShort;
		}

		JSONObject toJSON() {
			return new JSONObject()
					.put("title", this.title)
					.put("value", this.value)
					.put("short", this.isShort);
		}
	}

	enum MarkdownIn {
		pretext, text, fields
	}

	private final TautConnection conn;
	private Optional<String> fallback, pretext, authorName, authorLink, authorIcon, title, titleLink, text, imageUrl, thumbUrl, footer, footerIcon;
	private Optional<Color> color;
	private Field[] fields;
	private Optional<Long> ts;
	private final Set<MarkdownIn> markdownIn = new HashSet<>();

	public TautAttachment(TautConnection conn) {
		this.conn = conn;
		this.fallback = Optional.empty();
		this.color = Optional.empty();
		this.pretext = Optional.empty();
		this.authorName = Optional.empty();
		this.authorLink = Optional.empty();
		this.authorIcon = Optional.empty();
		this.title = Optional.empty();
		this.titleLink = Optional.empty();
		this.text = Optional.empty();
		this.fields = new Field[0];
		this.imageUrl = Optional.empty();
		this.thumbUrl = Optional.empty();
		this.footer = Optional.empty();
		this.footerIcon = Optional.empty();
		this.ts = Optional.empty();
	}

	public TautAttachment(TautConnection conn, String fallback, Color color, String pretext, String authorName, String authorLink, String authorIcon, String title, String titleLink, String text, Field[] fields, String imageUrl, String thumbUrl, String footer, String footerIcon, long ts) {
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
		this.fields = json.<JSONObject>streamArray("fields").map(Field::new).toArray(Field[]::new);
		this.imageUrl = json.getOpt("image_url");
		this.thumbUrl = json.getOpt("thumb_url");
		this.footer = json.getOpt("footer");
		this.footerIcon = json.getOpt("footer_icon");
		this.ts = json.has("ts") ? Optional.of(json.getLong("ts")) : Optional.empty();
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
	public Optional<Long> getTs() { return this.ts; }

	public TautAttachment setFallback(String fallback) {
		this.fallback = Optional.of(fallback);
		return this;
	}

	public TautAttachment setPretext(String pretext) {
		return this.setPretext(pretext, false);
	}

	public TautAttachment setPretext(String pretext, boolean markdown) {
		this.pretext = Optional.of(pretext);
		if(markdown) {
			this.markdownIn.add(MarkdownIn.pretext);
		}
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
		return this.setText(text, false);
	}

	public TautAttachment setText(String text, boolean markdown) {
		this.text = Optional.of(text);
		if(markdown) {
			this.markdownIn.add(MarkdownIn.text);
		}
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

	public TautAttachment setColor(String color) {
		if(color.startsWith("#")) {
			color = color.substring(1);
		}
		if(color.length() != 6) {
			throw new IllegalArgumentException("Bad color: " + color);
		}
		final int r = Integer.parseInt(color.substring(0, 2), 16),
		          g = Integer.parseInt(color.substring(2, 4), 16),
		          b = Integer.parseInt(color.substring(4, 6), 16);
		return this.setColor(new Color(r, g, b));
	}

	public TautAttachment setFields(Field... fields) {
		return this.setFields(fields, false);
	}

	public TautAttachment setFields(Field[] fields, boolean markdown) {
		this.fields = fields;
		if(markdown) {
			this.markdownIn.add(MarkdownIn.fields);
		}
		return this;
	}

	public TautAttachment setTs(long ts) {
		this.ts = Optional.of(ts);
		return this;
	}

	JSONObject toJSON() {
		return new JSONObject()
				.putOpt("fallback", this.fallback)
				.putOpt("color", this.color.map(color -> String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue())))
				.putOpt("pretext", this.pretext)
				.putOpt("author_name", this.authorName)
				.putOpt("author_link", this.authorLink)
				.putOpt("author_icon", this.authorIcon)
				.putOpt("title", this.title)
				.putOpt("title_link", this.titleLink)
				.putOpt("text", this.text)
				.put("fields", Arrays.stream(this.fields).map(Field::toJSON).toArray(JSONObject[]::new))
				.putOpt("image_url", this.imageUrl)
				.putOpt("thumb_url", this.thumbUrl)
				.putOpt("footer", this.footer)
				.putOpt("footer_icon", this.footerIcon)
				.putOpt("ts", this.ts)
				.put("mrkdwn_in", this.markdownIn.stream().map(MarkdownIn::toString).toArray(String[]::new))
				;
	}
}
