package com.mrozekma.taut;

import java.util.function.Function;

public interface TautEventListener {
	enum EventType {
		accounts_changed, bot_added, bot_changed, channel_archive, channel_created, channel_deleted,
		channel_history_changed, channel_joined, channel_left, channel_marked, channel_rename, channel_unarchive,
		commands_changed, dnd_updated, dnd_updated_user, email_domain_changed, emoji_changed, file_change,
		file_comment_added, file_comment_deleted, file_comment_edited, file_created, file_deleted, file_public,
		file_shared, file_unshared, group_archive, group_close, group_history_changed, group_joined, group_left,
		group_marked, group_open, group_rename, group_unarchive, hello, im_close, im_created, im_history_changed,
		im_marked, im_open, manual_presence_change, message, pin_added, pin_removed, pref_change, presence_change,
		reaction_added, reaction_removed, reconnect_url, star_added, star_removed, subteam_created, subteam_self_added,
		subteam_self_removed, subteam_updated, team_domain_change, team_join, team_migration_started, team_plan_change,
		team_pref_change, team_profile_change, team_profile_delete, team_profile_reorder, team_rename, url_verification,
		user_change, user_typing
	}

	@FunctionalInterface
	interface ChannelCreator {
		TautAbstractChannel makeChannel(JSONObject json) throws TautException;
	}

	default void fire(TautConnection conn, JSONObject json) throws TautException {
		final EventType type;
		try {
			type = EventType.valueOf(json.getString("type"));
		} catch(IllegalArgumentException e) {
			this.onUnknownEvent(json);
			return;
		}

		ChannelCreator makeChannel = data -> {
			if(data.has("channel")) {
				final String id = data.getString("channel");
				switch(id.isEmpty() ? '\0' : id.charAt(0)) {
				case 'C':
				case 'G':
					return new TautChannel(conn, id);
				case 'D':
					if(data.has("user")) {
						final TautUser user = new TautUser(conn, data.getString("user"));
						return new TautDirectChannel(user);
					}
					break;
				}
			}
			throw new TautException("Unable to construct channel");
		};

		//TODO Most if not all of the rest of these
		switch(type) {
		case message: {
			final TautAbstractChannel channel = makeChannel.makeChannel(json);
			final TautMessage message = new TautMessage(channel, json);
			this.onMessage(message);
			break; }
		case reaction_added: {
			final TautReaction reaction = new TautReaction(conn, json.getString("reaction"), 1, new TautUser(conn, json.getString("user")));

			final JSONObject item = json.getJSONObject("item");
			final String itemType = item.getString("type");
			if(itemType.equals("message")) {
				final TautAbstractChannel channel = makeChannel.makeChannel(item);
				this.onMessageReactionAdded(channel.messageByTs(item.getString("ts")), reaction);
			} else if(itemType.equals("file")) {
				this.onFileReactionAdded(new TautFile(conn, item.getString("file")), reaction);
			} else if(itemType.equals("file_comment")) {
				//TODO Not sure how to do this
//				this.onFileCommentReactionAdded(new TautFileComment(new TautFile(conn, item.getString("file")), item.getString("file_comment")), reaction);
				throw new TautException("Unimplemented");
			} else {
				throw new TautException("Unexpected reaction item type: " + itemType);
			}
			break; }
		default:
			this.onUnknownEvent(json);
		}
	}

	default void onUnknownEvent(JSONObject json) {}

	default void onMessage(TautMessage message) {}

	default void onMessageReactionAdded(TautMessage message, TautReaction reaction) {}

	default void onFileReactionAdded(TautFile file, TautReaction reaction) {}

	default void onFileCommentReactionAdded(TautFileComment comment, TautReaction reaction) {}
}
