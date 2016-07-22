package com.mrozekma.taut;

import java.awt.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

// https://api.slack.com/types/user
public class TautUser extends LazyLoadedObject {
	private String name;
	private boolean deleted;
	private Optional<Color> color;
	private Optional<String> firstName, lastName, realName, email, skype, phone;
	private Map<Integer, String> image;
	private boolean isAdmin, isOwner, isPrimaryOwner, isRestricted, isUltraRestricted, has2fa;
	private Optional<String> twoFactorType;
//	private boolean hasFiles; // Appears to not exist

	TautUser(TautConnection conn, String id) {
		super(conn, id);
		if(!id.startsWith("U")) {
			throw new IllegalArgumentException("Invalid user ID: " + id);
		}
	}

	TautUser(TautConnection conn, JSONObject json) {
		super(conn, json);
	}

	public String getName() throws TautException { this.checkLoad(); return this.name; }
	public boolean isDeleted() throws TautException { this.checkLoad(); return this.deleted; }
	public Optional<Color> getColor() throws TautException { this.checkLoad(); return this.color; }
	public Optional<String> getFirstName() throws TautException { this.checkLoad(); return this.firstName; }
	public Optional<String> getLastName() throws TautException { this.checkLoad(); return this.lastName; }
	public Optional<String> getRealName() throws TautException { this.checkLoad(); return this.realName; }
	public Optional<String> getEmail() throws TautException { this.checkLoad(); return this.email; }
	public Optional<String> getSkype() throws TautException { this.checkLoad(); return this.skype; }
	public Optional<String> getPhone() throws TautException { this.checkLoad(); return this.phone; }
	public Map<Integer, String> getImage() throws TautException { this.checkLoad(); return this.image; }
	public boolean isAdmin() throws TautException { this.checkLoad(); return this.isAdmin; }
	public boolean isOwner() throws TautException { this.checkLoad(); return this.isOwner; }
	public boolean isPrimaryOwner() throws TautException { this.checkLoad(); return this.isPrimaryOwner; }
	public boolean isRestricted() throws TautException { this.checkLoad(); return this.isRestricted; }
	public boolean isUltraRestricted() throws TautException { this.checkLoad(); return this.isUltraRestricted; }
	public boolean has2fa() throws TautException { this.checkLoad(); return this.has2fa; }
	public Optional<String> getTwoFactorType() throws TautException { this.checkLoad(); return this.twoFactorType; }
//	public boolean hasFiles() throws TautException { this.checkLoad(); return this.hasFiles; }

	private JSONObject post(String route) throws TautException {
		return this.post(route, new JSONObject());
	}

	private JSONObject post(String route, JSONObject args) throws TautException {
		args.put("user", this.getId());
		return this.conn.post(route, args);
	}

	@Override protected JSONObject load() throws TautException {
		return this.post("users.info").getJSONObject("user");
	}

	@Override protected void populate(JSONObject json) {
		this.name = json.getString("name");
		this.deleted = json.getBoolean("deleted");
		if(json.has("color")) {
			final String rgb = json.getString("color");
			final int r = Integer.parseInt(rgb.substring(0, 2), 16),
			          g = Integer.parseInt(rgb.substring(2, 4), 16),
			          b = Integer.parseInt(rgb.substring(4, 6), 16);
			this.color = Optional.of(new Color(r, g, b));
		} else {
			this.color = Optional.empty();
		}
		{
			final JSONObject profile = json.getJSONObject("profile");
			final Function<String, Optional<String>> getOpt = key -> {
				if(!profile.has(key) || profile.isNull(key)) {
					return Optional.empty();
				}
				final String val = profile.getString(key);
				return val.isEmpty() ? Optional.empty() : Optional.of(val);
			};

			this.firstName = getOpt.apply("first_name");
			this.lastName = getOpt.apply("last_name");
			this.realName = getOpt.apply("real_name");
			this.email = getOpt.apply("email");
			this.skype = getOpt.apply("skype");
			this.phone = getOpt.apply("phone");
			this.image = new Map<>();
			((Set<String>)profile.keySet()).stream().filter(key -> key.startsWith("image_")).forEach(key -> {
				final int imageSize = key.equals("image_original") ? 0 : Integer.parseInt(key.substring(6));
				this.image.put(imageSize, profile.getString(key));
			});
		}
		this.isAdmin = json.optBoolean("is_admin", false);
		this.isOwner = json.optBoolean("is_owner", false);
		this.isPrimaryOwner = json.optBoolean("is_primary_owner", false);
		this.isRestricted = json.optBoolean("is_restricted", false);
		this.isUltraRestricted = json.optBoolean("is_ultra_restricted", false);
		this.has2fa = json.optBoolean("has_2fa", false);
		this.twoFactorType = this.has2fa ? Optional.of(json.getString("two_factor_type")) : Optional.empty();
//		this.hasFiles = json.getBoolean("has_files");
	}

	private JSONObject makeJSONObject() {
		return new JSONObject().put("user", this.getId());
	}

	public FileIterable iterFiles() throws TautException {
		return this.conn.iterFiles(this);
	}

	public static TautUser getById(TautConnection conn, String id) {
		return new TautUser(conn, id);
	}

	public static TautUser getByName(TautConnection conn, String name) throws TautException {
		if(name.startsWith("@")) {
			name = name.substring(1);
		}
		for(TautUser user : getAll(conn)) {
			if(user.getName().equalsIgnoreCase(name)) {
				return user;
			}
		}
		throw new TautException(String.format("User `%s' not found", name));
	}

	public static List<TautUser> getAll(TautConnection conn) throws TautException {
		return conn.post("users.list").<JSONObject>streamArray("members").map(json -> new TautUser(conn, json)).collect(Collectors.toList());
	}
}
