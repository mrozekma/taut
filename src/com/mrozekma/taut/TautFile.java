package com.mrozekma.taut;

import org.json.JSONException;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

// https://api.slack.com/types/file
public class TautFile extends LazyLoadedObject {
	private Subtype subtype;

	private String title;
	private String mimetype;
	private String filetype;
	private String prettyType;
	private String permalink;

	private Optional<String> name; // this can be included in the reply, but be null
	private Optional<String> username; // not sure if this is optional
	private Optional<String> urlPrivate;
	private Optional<String> urlPrivateDownload;
	private Optional<String> permalinkPublic;

	private int created;
	private int size;
	private int numStars;
	private int commentsCount;

	private TautUser user;
	private TautChannel[] channels;
	private TautChannel[] groups;
//	private TautDirectMessage[] ims;
	private TautChannel[] pinnedTo;
	private Optional<TautFileComment> initialComment;
	private TautReaction[] reactions;

	private Optional<TautFileComment[]> comments = Optional.empty();

	private boolean editable;
	// private boolean isExternal; // Seems superfluous given 'mode'
	private boolean isPublic;
	private boolean publicUrlShared; // opt
	private boolean displayAsBot;
	private boolean isStarred; // opt, set if calling user starred the file; terrible name

	private Map<Integer, String> thumbs;

	TautFile(TautConnection conn, String id) {
		super(conn, id);
		// Fc is a TautFileComment
		if(!id.startsWith("F") || id.startsWith("Fc")) {
			throw new IllegalArgumentException("Invalid file ID: " + id);
		}
	}

	TautFile(TautConnection conn, JSONObject json) {
		super(conn, json);
	}

	public Subtype getSubtype() throws TautException { this.checkLoad(); return this.subtype; }

	public String getTitle() throws TautException { this.checkLoad(); return this.title; }
	public String getMimetype() throws TautException { this.checkLoad(); return this.mimetype; }
	public String getFiletype() throws TautException { this.checkLoad(); return this.filetype; }
	public String getPrettyType() throws TautException { this.checkLoad(); return this.prettyType; }
	public String getPermalink() throws TautException { this.checkLoad(); return this.permalink; }

	public Optional<String> getName() throws TautException { this.checkLoad(); return this.name; }
	public Optional<String> getUsername() throws TautException { this.checkLoad(); return this.username; }
	public Optional<String> getUrlPrivate() throws TautException { this.checkLoad(); return this.urlPrivate; }
	public Optional<String> getUrlPrivateDownload() throws TautException { this.checkLoad(); return this.urlPrivateDownload; }
	public Optional<String> getPermalinkPublic() throws TautException { this.checkLoad(); return this.permalinkPublic; }

	public int getCreated() throws TautException { this.checkLoad(); return this.created; }
	public int getSize() throws TautException { this.checkLoad(); return this.size; }
	public int getNumStars() throws TautException { this.checkLoad(); return this.numStars; }
	public int getCommentsCount() throws TautException { this.checkLoad(); return this.commentsCount; }

	public TautUser getUser() throws TautException { this.checkLoad(); return this.user; }
	public TautChannel[] getChannels() throws TautException { this.checkLoad(); return this.channels; }
	public TautChannel[] getGroups() throws TautException { this.checkLoad(); return this.groups; }
//	public TautDirectMessage[] getIms() throws TautException { this.checkLoad(); return this.ims; }
	public TautChannel[] getPinnedTo() throws TautException { this.checkLoad(); return this.pinnedTo; }
	public Optional<TautFileComment> getInitialComment() throws TautException { this.checkLoad(); return this.initialComment; }

	public boolean getEditable() throws TautException { this.checkLoad(); return this.editable; }
//	public boolean getIsExternal() throws TautException { this.checkLoad(); return this.isExternal; }
	public boolean getIsPublic() throws TautException { this.checkLoad(); return this.isPublic; }
	public boolean getPublicUrlShared() throws TautException { this.checkLoad(); return this.publicUrlShared; }
	public boolean getDisplayAsBot() throws TautException { this.checkLoad(); return this.displayAsBot; }
	public boolean getIsStarred() throws TautException { this.checkLoad(); return this.isStarred; }

	public Map<Integer, String> getThumbs() throws TautException { this.checkLoad(); return this.thumbs; }

	public TautFileComment[] getComments() throws TautException {
		this.checkLoad();
		// Comments are loaded separately because they're not included in a files.list request, but they are in a files.info request
		if(!this.comments.isPresent()) {
			final List<TautFileComment> comments = new LinkedList<>();
			final JSONObject req = new JSONObject().put("count", 1000);
			int pages = 1;
			for(int page = 1; page <= pages; page++) {
				req.put("page", page);
				final JSONObject res = this.post("files.info", req);
				res.getJSONArray("comments").<JSONObject>stream().forEach(o -> comments.add(new TautFileComment(this, o)));
				pages = res.getJSONObject("paging").getInt("pages");
			}
			this.comments = Optional.of(comments.toArray(new TautFileComment[0]));
		}
		return this.comments.get();
	}

	JSONObject post(String route) throws TautException {
		return this.post(route, new JSONObject());
	}

	JSONObject post(String route, JSONObject args) throws TautException {
		args.put("file", this.getId());
		return this.conn.post(route, args);
	}

	@Override protected JSONObject load() throws TautException {
		return this.post("files.info").getJSONObject("file");
	}

	@Override protected void populate(JSONObject json) {
		final String mode = json.getString("mode");
		if(mode.equals("hosted")) {
			this.subtype = new HostedFile();
		} else if(mode.equals("external")) {
			final Optional<String> externalType = json.getOpt("external_type");
			this.subtype = new ExternalFile(externalType);
		} else if(mode.equals("snippet")) {
			final String editLink = json.getString("edit_link");
			final String preview = json.getString("preview");
			final String previewHighlight = json.getString("preview_highlight");
			final int lines = json.getInt("lines");
			final int linesMore = json.getInt("lines_more");
			this.subtype = new SnippetFile(editLink, preview, previewHighlight, lines, linesMore);
		} else if(mode.equals("post")) {
			final String editLink = json.getString("edit_link");
			this.subtype = new PostFile(editLink);
		} else if(mode.equals("space")) {
			this.subtype = new SpaceFile();
		} else {
			throw new JSONException("Invalid mode: " + mode);
		}

		this.title = json.getString("title");
		this.mimetype = json.getString("mimetype");
		this.filetype = json.getString("filetype");
		this.prettyType = json.getString("pretty_type");
		this.permalink = json.getString("permalink");

		this.name = Optional.ofNullable(json.optString("name", null));
		this.username = json.getOpt("username");
		this.urlPrivate = json.getOpt("url_private");
		this.urlPrivateDownload = json.getOpt("url_private_download");
		this.permalinkPublic = json.getOpt("permalink_public");

		this.created = json.getInt("created");
		this.size = json.getInt("size");
		this.numStars = json.optInt("num_stars", 0);
		this.commentsCount = json.getInt("comments_count");

		this.user = new TautUser(this.conn, json.getString("user"));
		this.channels = json.<String>streamArray("channels").map(id -> new TautChannel(this.conn, id)).toArray(TautChannel[]::new);
		this.groups = json.<String>streamArray("groups").map(id -> new TautChannel(this.conn, id)).toArray(TautChannel[]::new);
//		this.ims = json.<JSONObject>streamArray("ims").map(id -> new TautDirectMessage(this.conn, id)).toArray(TautDirectMessage[]::new);
		this.pinnedTo = json.<String>streamArray("pinned_to").map(id -> new TautChannel(this.conn, id)).toArray(TautChannel[]::new);
		this.initialComment = json.has("initial_comment") ? Optional.of(new TautFileComment(this, json.getJSONObject("initial_comment"))) : Optional.empty();

		this.editable = json.optBoolean("editable", false);
//		this.isExternal = json.optBoolean("is_external", false);
		this.isPublic = json.optBoolean("is_public", false);
		this.publicUrlShared = json.optBoolean("public_url_shared", false);
		this.displayAsBot = json.optBoolean("display_as_bot", false);
		this.isStarred = json.optBoolean("is_starred", false);

		this.thumbs = new Map<>();
		((Set<String>)json.keySet()).stream().filter(key -> key.startsWith("thumb_")).forEach(key -> {
			// Ignoring other keys like thumb_360_gif and thumb_360_w for now
			try {
				final int thumbSize = Integer.parseInt(key.substring(6));
				this.thumbs.put(thumbSize, json.getString(key));
			} catch(NumberFormatException e) {}
		});
	}

	public static abstract class Subtype {}

	public static class HostedFile extends Subtype {}

	public static class ExternalFile extends Subtype {
		private final Optional<String> externalType;

		ExternalFile(Optional<String> externalType) {
			this.externalType = externalType;
		}

		public Optional<String> getExternalType() { return this.externalType; }
	}

	public class SnippetFile extends Subtype {
		private String editLink;
		private String preview;
		private String previewHighlight;

		private int lines; // Total number of lines
		private int linesMore; // Lines not shown in preview

		SnippetFile(String editLink, String preview, String previewHighlight, int lines, int linesMore) {
			this.editLink = editLink;
			this.preview = preview;
			this.previewHighlight = previewHighlight;
			this.lines = lines;
			this.linesMore = linesMore;
		}

		public String getEditLink() { return this.editLink; }
		public String getPreview() { return this.preview; }
		public String getPreviewHighlight() { return this.previewHighlight; }

		public int getLines() { return this.lines; }
		public int getLinesMore() { return this.linesMore; }
	}

	public class PostFile extends Subtype {
		private final String editLink; // post, snippet

		PostFile(String editLink) {
			this.editLink = editLink;
		}

		public String getEditLink() { return this.editLink; }
	}

	// This mode is undocumented; it appears to be the intro files every team starts with
	public static class SpaceFile extends Subtype {}
}
