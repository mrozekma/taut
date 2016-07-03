package com.mrozekma.taut;

abstract class LazyLoadedObject {
	private boolean isLoaded;
	final TautConnection conn;
	private final String id;

	protected LazyLoadedObject(TautConnection conn, String id) {
		this.conn = conn;
		this.id = id;
		this.isLoaded = false;
	}

	protected LazyLoadedObject(TautConnection conn, JSONObject json) {
		this.conn = conn;
		this.id = json.getString("id");
		this.isLoaded = true;
		this.populate(json);
	}

	protected final void checkLoad() throws TautException {
		if(!this.isLoaded) {
			this.populate(this.load());
			this.isLoaded = true;
		}
	}

	public String getId() { return this.id; }

	protected abstract JSONObject load() throws TautException;
	protected abstract void populate(JSONObject json);

	@Override public boolean equals(Object o) {
		if(!this.getClass().equals(o.getClass())) {
			return false;
		}
		final LazyLoadedObject other = (LazyLoadedObject)o;
		if(!this.isLoaded || !other.isLoaded) {
			return false;
		}
		return this.id.equals(other.id);
	}

	@Override public String toString() {
		// Slack has a unique ID character prefix for each type, so this should be unique even between different concrete classes
		return this.id;
	}
}
