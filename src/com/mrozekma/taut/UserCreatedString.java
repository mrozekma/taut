package com.mrozekma.taut;

import java.util.Date;
import java.util.Optional;

public class UserCreatedString {
	private final TautConnection conn;
	private final String value;
	private final Optional<TautUser> creator;
	private final long lastSet;
	private final boolean isPresent;

	UserCreatedString(TautConnection conn, String value, Optional<TautUser> creator, long lastSet) {
		this.conn = conn;
		this.value = value;
		this.lastSet = lastSet;
		if(this.value.isEmpty() && this.lastSet == 0) {
			this.creator = Optional.empty();
			this.isPresent = false;
		} else {
			this.creator = creator;
			this.isPresent = true;
		}
	}

	UserCreatedString(TautConnection conn, JSONObject json) {
		this.conn = conn;
		if(json == null) {
			this.value = "";
			this.creator = Optional.empty();
			this.lastSet = 0;
			this.isPresent = false;
		} else {
			this.value = json.getString("value");
			this.lastSet = json.getLong("last_set");
			if(this.value.isEmpty() && this.lastSet == 0) {
				this.creator = Optional.empty();
				this.isPresent = false;
			} else {
				this.creator = json.getString("creator").isEmpty() ? Optional.empty() : Optional.of(this.conn.getUserById(json.getString("creator")));
				this.isPresent = true;
			}
		}
	}

	public boolean isPresent() {
		return this.isPresent;
	}

	public String getValue() {
		return this.value;
	}

	public Optional<TautUser> getCreator() {
		return this.creator;
	}

	public long getLastSet() {
		return this.lastSet;
	}

	public Date getLastSetDate() {
		return TautConnection.tsApiToHost(this.getLastSet());
	}

	@Override public String toString() {
		return this.getValue();
	}
}
