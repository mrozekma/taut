package com.mrozekma.taut;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.json.JSONException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class TautHTTPSServer {
	public abstract static class RequestHandler implements HttpHandler {
		protected abstract void onSlackRequest(JSONObject request) throws TautException;

		protected final TautConnection conn;
		private String verificationToken;

		public RequestHandler(TautConnection conn) {
			this.conn = conn;
		}

		private void setVerificationToken(String verificationToken) {
			this.verificationToken = verificationToken;
		}

		@Override public final void handle(HttpExchange httpExchange) throws IOException {
			final StringBuffer requestText = new StringBuffer();
			final char[] buffer = new char[1024];
			try(final InputStreamReader isr = new InputStreamReader(httpExchange.getRequestBody(), StandardCharsets.UTF_8)) {
				while(true) {
					final int amt = isr.read(buffer, 0, buffer.length);
					if(amt < 0) {
						break;
					} else {
						requestText.append(buffer, 0, amt);
					}
				}
			}

			final List<NameValuePair> nvps = URLEncodedUtils.parse(requestText.toString(), StandardCharsets.UTF_8);
			if(nvps.size() != 1) {
				throw new IOException(String.format("Unexpected Slack payload: %d keys", nvps.size()));
			}
			if(!nvps.get(0).getName().equals("payload")) {
				throw new IOException("Unexpected Slack payload: wrong key");
			}

			final String payload = nvps.get(0).getValue();
			final JSONObject request;
			try {
				request = new JSONObject(payload);
			} catch(JSONException e) {
				throw new IOException(e);
			}

			if(!request.optString("token", "").equals(this.verificationToken)) {
				throw new IOException("Bad verification token");
			}

//			final Optional<JSONObject> response = this.onSlackRequest(request);
//			final String responseText = response.map(JSONObject::toString).orElse("");
//			httpExchange.sendResponseHeaders(200, responseText.length());
//			httpExchange.getResponseBody().write(responseText.getBytes());
			try {
				this.onSlackRequest(request);
			} catch(TautException e) {
				throw new IOException(e);
			}
			httpExchange.sendResponseHeaders(200, 0);
			httpExchange.close();
		}
	}

	public abstract static class ActionRequestHandler extends RequestHandler {
		public static class UserAction {
			// This leaves out a bunch of the data Slack sends
			private final String callbackId, responseUrl, actionName;
			private final Optional<String> value;
			private final TautChannel channel;
			private final TautUser user;
			private final TautMessage message;

			public UserAction(TautConnection conn, JSONObject json) throws TautException {
				this.callbackId = json.getString("callback_id");
				this.channel = new TautChannel(conn, json.getJSONObject("channel").getString("id"));
				this.user = new TautUser(conn, json.getJSONObject("user").getString("id"));
				this.message = this.channel.messageByTs(json.getString("message_ts"));
				this.responseUrl = json.getString("response_url");

				final JSONObject action = json.getJSONArray("actions").getJSONObject(0);
				this.actionName = action.getString("name");
				this.value = action.has("value") ? Optional.of(action.getString("value")) :
				             action.has("selected_options") ? Optional.of(action.getJSONArray("selected_options").getJSONObject(0).getString("value")) :
				             Optional.empty();
			}

			public String getCallbackId() { return this.callbackId; }
			public String getResponseUrl() { return this.responseUrl; }
			public String getActionName() { return this.actionName; }
			public Optional<String> getValue() { return this.value; }
			public TautChannel getChannel() { return this.channel; }
			public TautUser getUser() { return this.user; }
			public TautMessage getMessage() { return this.message; }
		}

		public ActionRequestHandler(TautConnection conn) {
			super(conn);
		}

		@Override protected void onSlackRequest(JSONObject request) throws TautException {
			this.onSlackRequest(new UserAction(this.conn, request));
		}

		protected abstract void onSlackRequest(UserAction action);
	}

	private final int port;
	private final String verificationToken;
	private final SSLContext ssl;
	private Optional<HttpsServer> server = Optional.empty();

	public TautHTTPSServer(File certificatePem, File privateKeyPem, int port, String verificationToken) throws TautException {
		this.port = port;
		this.verificationToken = verificationToken;

		try {
			final List<byte[]> certBytes = parsePem(certificatePem, "CERTIFICATE");
			final Certificate[] certs = new Certificate[certBytes.size()];
			for(int i = 0; i < certs.length; i++) {
				certs[i] = CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(certBytes.get(i)));
			}

			final byte[] keyBytes = parsePem(privateKeyPem, "PRIVATE KEY").get(0);
			//TODO Support other algorithms?
			final Key key = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));

			final KeyStore ks = KeyStore.getInstance("JKS");
			ks.load(null);
			ks.setCertificateEntry("cert", certs[0]);
			ks.setKeyEntry("key", key, new char[0], certs);

			final KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(ks, new char[0]);
			final TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
			tmf.init(ks);

			this.ssl = SSLContext.getInstance("TLS");
			this.ssl.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
		} catch(IOException | NoSuchAlgorithmException | CertificateException | InvalidKeySpecException | KeyStoreException | UnrecoverableKeyException | KeyManagementException e) {
			throw new TautException(e);
		}
	}

	public synchronized void start(RequestHandler requestHandler) throws IOException {
		this.server.ifPresent(server -> server.stop(0));

		requestHandler.setVerificationToken(this.verificationToken);

		final HttpsServer server = HttpsServer.create(new InetSocketAddress(this.port), 0);
		server.setHttpsConfigurator(new HttpsConfigurator(this.ssl));
		server.createContext("/slack_hook", requestHandler);
		server.setExecutor(null);
		server.start();

		this.server = Optional.of(server);
	}

	public synchronized void stop() {
		this.server.ifPresent(server -> {
			server.stop(2);
			this.server = Optional.empty();
		});
	}

	private static List<byte[]> parsePem(File pemFile, String markerType) throws IOException {
		final List<String> lines = Files.readAllLines(Paths.get(pemFile.toURI()), StandardCharsets.US_ASCII);

		final String beginMarker = String.format("-----BEGIN %s-----", markerType);
		final String endMarker = String.format("-----END %s-----", markerType);
		Optional<StringBuilder> thisBlock = Optional.empty();
		final List<byte[]> rtn = new LinkedList<>();
		for(String line : lines) {
			if(!thisBlock.isPresent() && line.equals(beginMarker)) {
				thisBlock = Optional.of(new StringBuilder());
			} else if(thisBlock.isPresent()) {
				if(line.equals(endMarker)) {
					rtn.add(Base64.getDecoder().decode(thisBlock.get().toString()));
					thisBlock = Optional.empty();
				} else {
					thisBlock.get().append(line);
				}
			} else {
				throw new IOException("Malformed PEM file: " + pemFile.getAbsolutePath());
			}
		}
		return rtn;
	}
}
