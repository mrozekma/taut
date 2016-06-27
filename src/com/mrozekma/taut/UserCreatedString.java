package com.mrozekma.taut;

import java.util.Date;
import java.util.Optional;

public class UserCreatedString {
	private final TautConnection conn;
	private final String value;
	private final Optional<TautUser> creator;
	private final Date lastSet;

	UserCreatedString(TautConnection conn, String value, Optional<TautUser> creator, Date lastSet) {
		this.conn = conn;
		this.value = value;
		this.creator = creator;
		this.lastSet = lastSet;
	}

	UserCreatedString(TautConnection conn, JSONObject json) {
		this.conn = conn;
		this.value = json.getString("value");
		this.creator = json.getString("creator").isEmpty() ? Optional.empty() : Optional.of(this.conn.getUserById(json.getString("creator")));
		this.lastSet = this.conn.resolveDate(json.getLong("last_set"));
	}

	public String getValue() {
		return this.value;
	}

	public Optional<TautUser> getCreator() {
		return this.creator;
	}

	public Date getLastSet() {
		return this.lastSet;
	}

	@Override public String toString() {
		return this.getValue();
	}
}
