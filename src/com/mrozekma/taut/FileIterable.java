package com.mrozekma.taut;

import java.util.*;
import java.util.stream.Collectors;

public class FileIterable implements Iterable<TautFile> {
	public enum Type {spaces, snippets, images, gdocs, zips, pdfs}

	private final TautConnection conn;
	private Optional<TautUser> user = Optional.empty();
	private Optional<TautChannel> channel = Optional.empty();
	private Optional<Date> tsFrom = Optional.empty(), tsTo = Optional.empty();
	private Type[] types = new Type[0];

	FileIterable(TautConnection conn) throws TautException {
		this.conn = conn;
	}

	public FileIterable setUser(TautUser user) {
		if(user.conn != conn) {
			throw new IllegalArgumentException("User from different connection");
		}
		this.user = Optional.of(user);
		return this;
	}

	public FileIterable setChannel(TautChannel channel) {
		if(channel.conn != conn) {
			throw new IllegalArgumentException("Channel from different connection");
		}
		this.channel = Optional.of(channel);
		return this;
	}

	public FileIterable setTsFrom(Date date) {
		this.tsFrom = Optional.of(date);
		return this;
	}

	public FileIterable setTsTo(Date date) {
		this.tsTo = Optional.of(date);
		return this;
	}

	public FileIterable setTypes(Type... types) {
		this.types = types;
		return this;
	}

	@Override public Iterator<TautFile> iterator() {
		return new Iterator<TautFile>() {
			private final JSONObject request = new JSONObject() {{
				final FileIterable f = FileIterable.this;
				putOpt("user", f.user);
				putOpt("channel", f.channel);
				f.tsFrom.ifPresent(ts -> put("ts_from", ts));
				f.tsTo.ifPresent(ts -> put("ts_to", ts));
				if(f.types.length > 0) {
					put("types", Arrays.stream(f.types).map(Type::name).collect(Collectors.joining(",")));
				}
				put("count", 100);
			}};
			private int nextPage = 1;
			private int pages;
			private LinkedList<TautFile> files;

			{
				this.doRequest();
			}

			private void doRequest() throws RuntimeException {
				this.request.put("page", this.nextPage);
				final JSONObject res;
				try {
					res = conn.post("files.list", request);
				} catch(TautException e) {
					// Can't throw TautException because we need to conform to the Iterable interface
					throw new RuntimeException(e);
				}
				this.files = new LinkedList<>(res.<JSONObject>streamArray("files").map(file -> new TautFile(conn, file)).collect(Collectors.toList()));
				this.pages = res.getJSONObject("paging").getInt("pages");
				this.nextPage = res.getJSONObject("paging").getInt("page") + 1;
			}

			@Override public boolean hasNext() {
				return !this.files.isEmpty() || this.nextPage <= this.pages;
			}

			@Override public TautFile next() {
				if(this.files.isEmpty()) {
					if(this.nextPage <= this.pages) {
						// Next request
						this.doRequest();
					} else {
						throw new NoSuchElementException();
					}
				}
				return this.files.remove();
			}
		};
	}
}
