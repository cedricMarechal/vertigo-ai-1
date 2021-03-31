package io.vertigo.ai.impl.bb;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import io.vertigo.ai.bb.BlackBoard;
import io.vertigo.core.lang.Assertion;

public final class BlackBoardImpl implements BlackBoard {
	private final BlackBoardStorePlugin blackBoardStorePlugin;

	BlackBoardImpl(final BlackBoardStorePlugin blackBoardStorePlugin) {
		Assertion.check()
				.isNotNull(blackBoardStorePlugin);
		//---
		this.blackBoardStorePlugin = blackBoardStorePlugin;
	}

	//------------------------------------
	//--- Keys
	//------------------------------------
	@Override
	public boolean exists(final String key) {
		checkKey(key);
		//---
		return blackBoardStorePlugin
				.exists(key);
	}

	@Override
	public Set<String> keys(final String keyPattern) {
		checkKeyPattern(keyPattern);
		//---
		return blackBoardStorePlugin
				.keys(keyPattern);
	}

	@Override
	public void delete(final String keyPattern) {
		checkKeyPattern(keyPattern);
		//---
		blackBoardStorePlugin
				.remove(keyPattern);
	}

	@Override
	public Type getType(final String key) {
		checkKey(key);
		//---
		return blackBoardStorePlugin.getType(key);
	}

	//------------------------------------
	//--- KV
	//------------------------------------
	@Override
	public String format(final String msg) {
		return format(msg, this.blackBoardStorePlugin::get);
	}

	//--- KV String 
	@Override
	public String getString(final String key) {
		checkKey(key, Type.String);
		//---
		return blackBoardStorePlugin
				.getString(key);
	}

	@Override
	public void putString(final String key, final String value) {
		checkKey(key, Type.String);
		//---
		blackBoardStorePlugin
				.putString(key, value);
	}

	@Override
	public void append(final String key, final String something) {
		String value = getString(key); // getString includes type checking 
		if (value == null) {
			value = "";
		}
		putString(key, value + something);
	}

	@Override
	public boolean eq(final String key, final String compare) {
		final String value = getString(key); // getString includes type checking
		return value == null ? compare == null : value.equals(compare);
	}

	@Override
	public boolean eqCaseInsensitive(final String key, final String compare) {
		final String value = getString(key); // getString includes type checking
		return value == null ? compare == null : value.equalsIgnoreCase(compare);
	}

	@Override
	public boolean startsWith(final String key, final String compare) {
		final String value = getString(key); // getString includes type checking
		return value == null ? compare == null : value.startsWith(compare);
	}

	//--- KV Integer

	@Override
	public Integer getInteger(final String key) {
		checkKey(key, Type.Integer);
		//---
		return blackBoardStorePlugin
				.getInteger(key);
	}

	@Override
	public void putInteger(final String key, final Integer value) {
		checkKey(key, Type.Integer);
		//---
		blackBoardStorePlugin
				.putInteger(key, value);
	}

	@Override
	public void incrBy(final String key, final int value) {
		checkKey(key, Type.Integer);
		//---
		blackBoardStorePlugin.incrBy(key, value);
	}

	@Override
	public void incr(final String key) {
		incrBy(key, 1);
	}

	@Override
	public void decr(final String key) {
		incrBy(key, -1);
	}

	@Override
	public boolean lt(final String key, final Integer compare) {
		return compareInteger(key, compare) < 0;
	}

	@Override
	public boolean eq(final String key, final Integer compare) {
		return compareInteger(key, compare) == 0;
	}

	@Override
	public boolean gt(final String key, final Integer compare) {
		return compareInteger(key, compare) > 0;
	}

	private int compareInteger(final String key, final Integer compare) {
		checkKey(key, Type.Integer);
		//---
		final Integer value = getInteger(key);
		return compareInteger(value, compare);
	}

	//------------------------------------
	//- List                             
	//- All methods are prefixed with list  
	//------------------------------------
	@Override
	public int listSize(final String key) {
		checkKey(key, Type.List);
		//---
		return blackBoardStorePlugin
				.listSize(key);
	}

	@Override
	public void listPush(final String key, final String value) {
		checkKey(key, Type.List);
		//---
		blackBoardStorePlugin
				.listPush(key, value);
	}

	@Override
	public String listPop(final String key) {
		checkKey(key, Type.List);
		//---
		return blackBoardStorePlugin
				.listPop(key);
	}

	@Override
	public String listPeek(final String key) {
		checkKey(key, Type.List);
		//---
		return blackBoardStorePlugin
				.listPeek(key);
	}

	@Override
	public String listGet(final String key, final int idx) {
		checkKey(key, Type.List);
		//---
		return blackBoardStorePlugin
				.listGet(key, idx);
	}

	//------------------------------------
	//- Utils                             -
	//------------------------------------
	/**
	 * Checks the key is following the regex	 
	 * 
	 * @param key the key
	 */
	private static void checkKey(final String key) {
		Assertion.check()
				.isNotBlank(key)
				.isTrue(key.matches(KEY_REGEX), "the key '{0}' must contain only a-z 1-9 words separated with /", key);
	}

	/**
	 * Checks  
	 * - the key is following the regex
	 * - the type is ok
	 * 
	 * @param key
	 * @param type
	 */
	private void checkKey(final String key, final Type type) {
		Assertion.check()
				.isNotBlank(key)
				.isNotNull(type);
		//---
		final Type t = getType(key);
		if (t != null && !type.equals(t)) {
			throw new IllegalStateException("the type of the key " + t + " is not the one expected " + type);
		}
	}

	private static void checkKeyPattern(final String keyPattern) {
		Assertion.check()
				.isNotBlank(keyPattern)
				.isTrue(keyPattern.matches(KEY_PATTERN_REGEX), "the key pattern '{0}' must contain only a-z 1-9 words separated with / and is finished by a * or nothing", keyPattern);
	}

	private static int compareInteger(final Integer value, final Integer compare) {
		if (value == null) {
			return compare == null
					? 0
					: -1;
		}
		if (compare == null) {
			return value == null
					? 0
					: -1;
		}
		return value.compareTo(compare);
	}

	public static String format(final String msg, final Function<String, String> kv) {
		Assertion.check()
				.isNotNull(msg);
		//---
		final String START_TOKEN = "{{";
		final String END_TOKEN = "}}";

		final StringBuilder builder = new StringBuilder(msg);
		int start = 0;
		int end;
		while ((end = builder.indexOf(END_TOKEN, start)) >= 0) {
			start = builder.lastIndexOf(START_TOKEN, end);
			if (start < 0) {
				throw new IllegalStateException("An end token '" + END_TOKEN + "+'has been found without a start token " + START_TOKEN);
			}
			final var paramName = builder.substring(start + START_TOKEN.length(), end);
			final var paramVal = Optional.ofNullable(kv.apply(paramName))
					.orElse("not found:" + paramName);
			builder.replace(start, end + END_TOKEN.length(), paramVal);
		}
		if (builder.indexOf(START_TOKEN) > 0) {
			throw new IllegalStateException("A start token '" + START_TOKEN + "+'has been found without an end token " + END_TOKEN);
		}
		return builder.toString();
	}

}
