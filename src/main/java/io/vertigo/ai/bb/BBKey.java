package io.vertigo.ai.bb;

import io.vertigo.core.lang.Assertion;

/**
 * The key defines the location of a value.  
 * A key can be composed of atomic keys (Ki).
 * K1+K2+...Kn => K 
 * K is a composite key (aka a path)
 * Where 
 *  - K1 is the head
 *  - Kn is the tail
 *  
 * if K is atomic then head = tail = K
 * 
 * A Key can be composite or atomic.
 * 
 * ex :
 * /u/name
 * /game/score
 * /game/players/1
 * /game/players/2
 * 
 * @author pchretien
 */
public final class BBKey {
	public static String KEY_REGEX = "(/[a-z0-9]+)+";
	private final String key;

	private BBKey(final String key) {
		Assertion.check()
				.isNotBlank(key)
				.isTrue(key.matches(KEY_REGEX), "the key '{0}' must contain only a-z 1-9 words separated with /", key);
		//---
		this.key = key;
	}

	public static BBKey of(final String key) {
		return new BBKey(key);
	}

	public static BBKey of(final BBKey rootKey, final String key) {
		Assertion.check()
				.isNotNull(rootKey)
				.isNotBlank(key);
		//---
		return BBKey.of(rootKey.key() + key);
	}

	public String key() {
		return key;
	}

	public BBKey add(final BBKey otherKey) {
		return BBKey.of(key + otherKey.key);
	}

	/**
	 * Extracts the head (atomic Key) of a key.
	 * A key can be composed of atomic keys (Ki).
	 * K1+K2+...Kn => K 
	 * K1 is the head
	 * 
	 * @return the head
	 */
	public BBKey head() {
		Assertion.check()
				.isTrue(key.charAt(0) == '/', "Key {0} doesn't start with the first char '/'", key);
		//---
		final int nextSlash = key.indexOf('/', 1);
		return nextSlash < 0
				? this
				: BBKey.of(key.substring(0, nextSlash));
	}

	/**
	 * Extracts the tail (atomic Key) of a key.
	 * A key can be composed of atomic keys (Ki).
	 * K1+K2+...Kn => K 
	 * Kn is the tail
	 * 
	 * @return the tail
	 */
	public BBKey tail() {
		final int lastSlash = key.lastIndexOf('/');
		return lastSlash == 0
				? this
				: BBKey.of(key.substring(lastSlash));
	}

	/**
	 * @return true if the key is atomic 
	 */
	public boolean isAtomic() {
		return key.lastIndexOf('/') == 0;
	}
	//	public BBKey indent(final String prefix) {
	//		Assertion.check().isNotBlank(prefix);
	//		//---
	//		return BBKey.of(prefix + key);
	//	}
	//
	//	public BBKey outdent(final String prefix) {
	//		Assertion.check()
	//				.isNotBlank(prefix)
	//				.isTrue(key.startsWith(prefix), "To outdent the key '{0}' it must starts with the provided prefix '{1}' ", key, prefix);
	//		//---
	//		return BBKey.of(key.substring(0, prefix.length() - 1));
	//	}

	@Override
	public boolean equals(final Object obj) {
		return obj instanceof BBKey && key.equals(((BBKey) obj).key);
	}

	@Override
	public int hashCode() {
		return key.hashCode();
	}
}
