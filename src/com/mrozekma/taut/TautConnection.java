package com.mrozekma.taut;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

public class TautConnection {
	private static final String API_URL = "https://slack.com/api/%s";

	private final String token;

	public TautConnection(String token) throws IOException {
		this.token = token;
	}

	JSONObject post(String route) throws TautException {
		return this.post(route, new JSONObject());
	}

	JSONObject post(String route, JSONObject args) throws TautException {
		final HttpClient client = HttpClients.createDefault();
		final HttpPost post = new HttpPost(String.format(API_URL, route));

		final List<NameValuePair> nvps = new LinkedList<>();
		nvps.add(new BasicNameValuePair("token", this.token));
		args.stream().forEach(k -> {
			final String key = (String)k;
			nvps.add(new BasicNameValuePair(key, args.getString(key)));
		});
		try {
			post.setEntity(new UrlEncodedFormEntity(nvps));
		} catch(UnsupportedEncodingException e) {
			throw new RuntimeException(e);
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

		if(!rtn.getBoolean("ok")) {
			throw new APIError(route, args, rtn);
		}
		return rtn;
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

	public TautUser getUserById(String id) {
		return TautUser.getById(this, id);
	}

	public TautUser getUserByName(String name) throws TautException {
		return TautUser.getByName(this, name);
	}

	public List<TautUser> getUsers() throws TautException {
		return TautUser.getAll(this);
	}

	public Date resolveDate(long unixTs) {
		return new Date(unixTs * 1000);
	}
}
