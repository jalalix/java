package com.jalalix.java;

/** Options for {@link JalaliX#create} / {@link JalaliX#parseJalali}. */
public final class JalaliOptions {
	/** IANA timezone id, or aliases like {@code utc}, {@code local}. */
	public String timezone;

	/** Creates options with no timezone override. */
	public JalaliOptions() {}

	/** @param timezone IANA timezone id or alias */
	public JalaliOptions(String timezone) {
		this.timezone = timezone;
	}

	/** Creates options with the given timezone. */
	public static JalaliOptions ofTimezone(String timezone) {
		return new JalaliOptions(timezone);
	}
}
