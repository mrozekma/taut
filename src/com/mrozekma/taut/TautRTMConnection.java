package com.mrozekma.taut;

import org.glassfish.tyrus.client.ClientManager;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.util.*;

public class TautRTMConnection implements MessageHandler.Whole<String> {
	private class Watchdog extends TimerTask {
		static final int PING_PERIOD = 20; // seconds

		private Optional<Timer> timer = Optional.empty();
		private int lastPingId = 0, lastPongId = 0;

		public void start() {
			this.timer.ifPresent(timer -> timer.cancel());
			this.timer = Optional.of(new Timer());
			this.timer.get().scheduleAtFixedRate(this, PING_PERIOD * 1000, PING_PERIOD * 1000);
		}

		public void stop() {
			if(this.timer.isPresent()) {
				this.timer.get().cancel();
				this.timer = Optional.empty();
			}
		}

		@Override public void run() {
			try {
				if(!TautRTMConnection.this.isConnected() || this.lastPingId != this.lastPongId) {
					this.lastPingId = this.lastPongId = 0;
					TautRTMConnection.this.connect();
					return;
				}

				try {
					this.sendPing();
				} catch(TautException e) {
					TautRTMConnection.this.connect();
				}
			} catch(TautException e) {
				throw new RuntimeException(e);
			}
		}

		public void sendPing() throws TautException {
			this.lastPingId = TautRTMConnection.this.sendMessage(new JSONObject().put("type", "ping"));
		}

		public void receivePong(JSONObject message) throws TautException {
			if(!message.optString("type", "").equals("pong")) {
				throw new TautException("Invalid pong message");
			}
			this.lastPongId = message.getInt("reply_to");
		}
	}

	private final TautConnection conn;
	private final ClientManager cm = ClientManager.createClient();
	private final Watchdog watchdog = new Watchdog();
	private final List<TautEventListener> listeners = new LinkedList<>();

	private Optional<Session> session = Optional.empty();
	private int nextMessageId = 1;

	TautRTMConnection(TautConnection conn) throws TautException {
		this.conn = conn;

		this.connect();
		this.watchdog.start();
	}

	public void addListener(TautEventListener listener) {
		this.listeners.add(listener);
	}

	public void removeListener(TautEventListener listener) {
		this.listeners.remove(listener);
	}

	private String getUrl() throws TautException {
		final JSONObject res = this.conn.post("rtm.start", new JSONObject().put("simple_latest", true).put("no_unreads", true));
		return res.getString("url");
	}

	public boolean isConnected() {
		return this.session.map(Session::isOpen).orElse(false);
	}

	public void connect() throws TautException {
		if(this.session.isPresent()) {
			try {
				session.get().close();
			} catch(IOException e) {
				throw new TautException(e);
			}
		}

		try {
			this.session = Optional.of(this.cm.connectToServer(new Endpoint() {
				@Override public void onOpen(Session session, EndpointConfig endpointConfig) {
					session.addMessageHandler(TautRTMConnection.this);
				}
			}, URI.create(this.getUrl())));
		} catch(DeploymentException | IOException e) {
			throw new TautException(e);
		}
	}

	@Override public void onMessage(String s) {
		try {
			this.receiveMessage(new JSONObject(s));
		} catch(TautException e) {
			throw new RuntimeException(e);
		}
	}

	private void receiveMessage(JSONObject json) throws TautException {
		if(TautConnection.VERBOSE) {
			System.out.format("[Rx RTM] %s\n", json);
		}
		//TODO Rx types
		if(json.optString("type", "").equals("pong")) {
			this.watchdog.receivePong(json);
		} else {
			for(TautEventListener listener : this.listeners) {
				listener.fire(this.conn, json);
			}
		}
	}

	private int sendMessage(JSONObject json) throws TautException {
		if(!this.session.isPresent()) {
			this.connect();
		}

		final int id = this.nextMessageId++;
		json.put("id", id);
		try {
			if(TautConnection.VERBOSE) {
				System.out.format("[Tx RTM] %s\n", json.toString());
			}
			this.session.get().getBasicRemote().sendText(json.toString());
		} catch(IOException e) {
			throw new TautException(e);
		}
		return id;
	}
}
