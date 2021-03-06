package com.mrozekma.taut;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import java.awt.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.List;

public class TautConnection {
	static final boolean VERBOSE = System.getenv().containsKey("TAUT_VERBOSE");
	private static final String API_URL = "https://slack.com/api/%s";

	private final String token;
	private final TautUser me;
	TautConnection historyConnection;

	public TautConnection(String token) throws TautException {
		this.token = token;

		final JSONObject res = this.post("auth.test");
		this.me = new TautUser(this, res.getString("user_id"));

		this.historyConnection = this;
	}

	public void setHistoryConnection(TautConnection historyConnection) {
		this.historyConnection = historyConnection;
	}

	JSONObject post(String route) throws TautException {
		return this.post(route, new JSONObject());
	}

	JSONObject post(String route, JSONObject args) throws TautException {
		return staticPost(route, args.put("token", this.token));
	}

	private static JSONObject staticPost(String route, JSONObject args) throws TautException {
		final HttpClient client = HttpClients.createDefault();
		final HttpPost post = new HttpPost(String.format(API_URL, route));

		// Use a MultipartEntity iff there are byte[](s) in 'args'
		// It's possible to use it in all cases, but the inefficiency bothers me
		{
			final HttpEntity entity;
			if(args.stream().map(args::get).anyMatch(e -> e instanceof byte[])) {
				MultipartEntityBuilder builder = MultipartEntityBuilder.create();
				args.stream().forEach(key -> {
					final Object val = args.get(key);
					if(val instanceof byte[]) {
						builder.addBinaryBody(key, (byte[])val, ContentType.APPLICATION_OCTET_STREAM, "file");
					} else {
						builder.addTextBody(key, val.toString());
					}
				});
				entity = builder.build();
			} else {
				final List<NameValuePair> nvps = new LinkedList<>();
				args.stream().forEach(key -> {
					final Object val = args.get(key);
					nvps.add(new BasicNameValuePair(key, val.toString()));
				});

				try {
					entity = new UrlEncodedFormEntity(nvps, "UTF-8");
				} catch(UnsupportedEncodingException e) {
					throw new TautException(e);
				}
			}

			if(VERBOSE) {
				System.out.printf("[Tx API] %s ", route);
				try {
					entity.writeTo(System.out);
					System.out.println();
				} catch(IOException e) {
					System.out.println("(error)");
				}
			}
			post.setEntity(entity);
		}

		final JSONObject rtn;
		try {
			final HttpResponse resp = client.execute(post);
			final HttpEntity entity = resp.getEntity();
			final Scanner s = new Scanner(entity.getContent());
			final StringBuffer buffer = new StringBuffer();
			while(s.hasNextLine()) {
				buffer.append(s.nextLine());
			}
			rtn = new JSONObject(buffer.toString());
		} catch(IOException e) {
			throw new TautException(e);
		}

		if(VERBOSE) {
			System.out.printf("[Rx API] %s\n", rtn);
		}
		if(!rtn.getBoolean("ok")) {
			throw new APIError(route, args, rtn);
		}
		return rtn;
	}

	public static String oauthAccess(String clientId, String clientSecret, String code) throws TautException {
		final JSONObject json = staticPost("oauth.access", new JSONObject().put("client_id", clientId).put("client_secret", clientSecret).put("code", code));
		return json.getString("access_token");
	}

	public TautChannel getChannelById(String id) {
		return TautChannel.getById(this, id);
	}

	public TautChannel getChannelByName(String name) throws TautException {
		return TautChannel.getByName(this, name);
	}

	public List<TautChannel> getChannels() throws TautException {
		return TautChannel.getAll(this);
	}

	public TautChannel createChannel(String name) throws TautException {
		return TautChannel.create(this, name);
	}

	public TautFile uploadFile(TautFileUpload file) throws TautException {
		return TautFile.upload(this, file);
	}

	public TautUser getSelf() {
		return this.me;
	}

	public TautUser getUserById(String id) {
		return TautUser.getById(this, id);
	}

	public TautUser getUserByName(String name) throws TautException {
		return TautUser.getByName(this, name);
	}

	public List<TautUser> getUsers() throws TautException {
		return TautUser.getAll(this);
	}

	public FileIterable iterFiles() throws TautException {
		return new FileIterable(this);
	}

	public FileIterable iterFiles(TautUser user) throws TautException {
		return new FileIterable(this).setUser(user);
	}

	public FileIterable iterFiles(TautChannel channel) throws TautException {
		return new FileIterable(this).setChannel(channel);
	}

	public void revokeToken() throws TautException {
		this.post("auth.revoke");
	}

	public java.util.Map<String, String> emojiList() throws TautException {
		final JSONObject res = this.post("emoji.list");
		return new Map<String, String>() {{
			res.getJSONObject("emoji").forEach((String k, String v) -> this.put(k, v));
		}};
	}

	public TautRTMConnection rtmStart() throws TautException {
		return new TautRTMConnection(this);
	}

	// ts is frequently used as an ID, so in those cases we keep them as strings to avoid precision problems when converting back.
	// This is only used if we really need the date for something
	static Date tsApiToHost(String ts) {
		return new Date((long)(Double.parseDouble(ts) * 1000));
	}

	static Date tsApiToHost(long ts) {
		return new Date(ts * 1000);
	}

	static String tsHostToApi(Date ts) {
		return "" + (ts.getTime() * 1000);
	}

	static Color colorApiToHost(String color) {
		if(color.startsWith("#")) {
			color = color.substring(1);
		}
		if(color.length() != 6) {
			throw new NumberFormatException("Bad color: " + color);
		}
		final int r = Integer.parseInt(color.substring(0, 2), 16);
		final int g = Integer.parseInt(color.substring(2, 4), 16);
		final int b = Integer.parseInt(color.substring(4, 6), 16);
		return new Color(r, g, b);
	}

	static String colorHostToApi(Color color) {
		return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
	}
}
