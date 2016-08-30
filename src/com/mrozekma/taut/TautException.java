package com.mrozekma.taut;

public class TautException extends Exception {
	public TautException(String msg) {
		super(msg);
	}

	public TautException(Throwable parent) {
		super(parent);
	}

	public TautException(String msg, Throwable parent) {
		super(msg, parent);
	}
}

class APIError extends TautException {
	final String route;
	final JSONObject args, resp;

	APIError(String route, JSONObject args, JSONObject resp) {
		super("Slack API error: " + resp.getString("error"));
		this.route = route;
		this.args = args;
		this.resp = resp;
	}
}
