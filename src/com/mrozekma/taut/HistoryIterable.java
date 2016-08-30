package com.mrozekma.taut;

import java.util.*;
import java.util.stream.Collectors;

public class HistoryIterable implements Iterable<TautMessage> {
	private final TautConnection conn;
	private final TautAbstractChannel channel;
	private final Optional<Date> latest;
	private final Optional<Date> oldest;
	private final boolean inclusive;
	private final int count;
	private final boolean unreads;

	HistoryIterable(TautAbstractChannel channel, Optional<Date> latest, Optional<Date> oldest, boolean inclusive, int count, boolean unreads) throws TautException {
		this.conn = channel.conn;
		this.channel = channel;
		this.latest = latest;
		this.oldest = oldest;
		this.inclusive = inclusive;
		this.count = count;
		this.unreads = unreads;
	}

	@Override public Iterator<TautMessage> iterator() {
		return new Iterator<TautMessage>() {
			private final JSONObject request = new JSONObject().putOpt("latest", latest).putOpt("oldest", oldest).put("inclusive", inclusive ? 1 : 0).put("count", count).put("unreads", unreads ? 1 : 0);
			private Optional<String> nextRequestLatest;
			private LinkedList<TautMessage> messages;

			{
				this.doRequest();
			}

			private void doRequest() throws RuntimeException {
				final JSONObject res;
				try {
					res = channel.post(channel.getRoutePrefix() + ".history", request);
				} catch(TautException e) {
					// Can't throw TautException because we need to conform to the Iterable interface
					throw new RuntimeException(e);
				}
				this.messages = new LinkedList<>(res.<JSONObject>streamArray("messages").map(message -> new TautMessage(channel, message)).collect(Collectors.toList()));
				this.nextRequestLatest = res.optBoolean("has_more", false) ? Optional.of(this.messages.getLast().getCurrent().getTs()) : Optional.empty();
			}

			@Override public boolean hasNext() {
				return !this.messages.isEmpty() || this.nextRequestLatest.isPresent();
			}

			@Override public TautMessage next() {
				if(this.messages.isEmpty()) {
					if(this.nextRequestLatest.isPresent()) {
						// Next request
						this.request.put("latest", this.nextRequestLatest.get());
						this.doRequest();
						this.nextRequestLatest = Optional.empty();
					} else {
						throw new NoSuchElementException();
					}
				}
				return this.messages.remove();
			}
		};
	}
}
