package com.mrozekma.taut;

class Map<K, V> extends java.util.HashMap<K, V> {
	public Map<K, V> set(K key, V val) {
		super.put(key, val);
		return this;
	}
}

class HTTPMap extends Map<String, String> {}
