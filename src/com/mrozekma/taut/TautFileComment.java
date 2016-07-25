package com.mrozekma.taut;

import java.util.Date;

public class TautFileComment extends LazyLoadedObject {
	private final TautFile file;
	private long created;
	private TautUser user;
	private String comment;

	// I made this a LazyLoadedObject, but I don't actually think it's possible to lookup comments by ID
	/*
	TautFileComment(TautFile file, String id) {
		super(file.conn, id);
		this.file = file;
		if(!id.startsWith("Fc")) {
			throw new IllegalArgumentException("Invalid file comment ID: " + id);
		}
	}
	*/

	TautFileComment(TautFile file, JSONObject json) {
		super(file.conn, json);
		this.file = file;
	}

	public TautFile getFile() throws TautException { return this.file; }
	public long getCreated() throws TautException { this.checkLoad(); return this.created; }
	public TautUser getUser() throws TautException { this.checkLoad(); return this.user; }
	public String getComment() throws TautException { this.checkLoad(); return this.comment; }

	public Date getCreatedDate() throws TautException {
		return TautConnection.tsApiToHost(this.getCreated());
	}

	JSONObject post(String route) throws TautException {
		return this.post(route, new JSONObject());
	}

	JSONObject post(String route, JSONObject args) throws TautException {
		args.put("id", this.getId());
		return this.file.post(route, args);
	}

	@Override protected JSONObject load() throws TautException {
		throw new UnsupportedOperationException();
	}

	@Override protected void populate(JSONObject json) {
		this.created = json.getLong("created");
		this.user = new TautUser(this.conn, json.getString("user"));
		this.comment = json.getString("comment");
	}

	public TautFileComment edit(String comment) throws TautException {
		this.populate(this.post("files.comments.edit", new JSONObject().put("comment", comment)));
		return this;
	}

	public void delete() throws TautException {
		this.post("files.comments.delete");
		this.unload();
	}
}
