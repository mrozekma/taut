package com.mrozekma.taut;

import java.util.AbstractSequentialList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class TautReactionList extends AbstractSequentialList<TautReaction> {
	protected final TautConnection conn;
	private Optional<List> backingList = Optional.empty();

	protected TautReactionList(TautConnection conn) {
		this.conn = conn;
	}

	private void checkLoad() {
		try {
			if(!this.backingList.isPresent()) {
				this.backingList = Optional.of(this.load());
			}
		} catch(TautException e) {
			// Need to conform to List interface
			throw new RuntimeException(e);
		}
	}

	protected abstract JSONObject buildRequest();

	protected abstract JSONObject extractResponse(JSONObject json);

	public void add(String name) throws TautException {
		final JSONObject req = this.buildRequest();
		req.put("name", name);
		this.conn.post("reactions.add", req);
	}

	public void remove(String name) throws TautException {
		final JSONObject req = this.buildRequest();
		req.put("name", name);
		this.conn.post("reactions.remove", req);
	}

	private List load() throws TautException {
		final JSONObject req = this.buildRequest();
		req.put("full", true);
		final JSONObject res = this.extractResponse(this.conn.post("reactions.get", req));
		return res.<JSONObject>streamArray("reactions").map(e -> new TautReaction(this.conn, e)).collect(Collectors.toList());
	}

	@Override public ListIterator<TautReaction> listIterator(int i) {
		this.checkLoad();
		return this.backingList.get().listIterator(i);
	}

	@Override public int size() {
		this.checkLoad();
		return this.backingList.get().size();
	}
}

class TautFileReactionList extends TautReactionList {
	private final TautFile file;

	protected TautFileReactionList(TautFile file) {
		super(file.conn);
		this.file = file;
	}

	@Override protected JSONObject buildRequest() {
		return new JSONObject().put("file", this.file.getId());
	}

	@Override protected JSONObject extractResponse(JSONObject json) {
		return json.getJSONObject("file");
	}
}

class TautFileCommentReactionList extends TautReactionList {
	private final TautFileComment comment;

	protected TautFileCommentReactionList(TautFileComment comment) {
		super(comment.conn);
		this.comment = comment;
	}

	@Override protected JSONObject buildRequest() {
		return new JSONObject().put("file_comment", this.comment.getId());
	}

	@Override protected JSONObject extractResponse(JSONObject json) {
		return json.getJSONObject("comment");
	}
}

class TautMessageReactionList extends TautReactionList {
	private final TautMessage message;

	protected TautMessageReactionList(TautMessage message) {
		super(message.conn);
		this.message = message;
	}

	@Override protected JSONObject buildRequest() {
		return new JSONObject().put("channel", this.message.getChannel().getId()).put("timestamp", this.message.getCurrent().getTs());
	}

	@Override protected JSONObject extractResponse(JSONObject json) {
		return json.getJSONObject("message");
	}
}