package com.mrozekma.taut;

public class TautException extends Exception {
	TautException(String msg) {
		super(msg);
	}

	TautException(Throwable parent) {
		super(parent);
	}

	TautException(String msg, Throwable parent) {
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
