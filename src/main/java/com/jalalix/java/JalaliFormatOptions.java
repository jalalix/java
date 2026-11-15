package com.jalalix.java;

/** Options for {@link JalaliX#format(String, JalaliFormatOptions)}. */
public final class JalaliFormatOptions {
	// {@code "jalali"} (default) or {@code "gregorian"}.
	public String calendar;
	// When true, digits are rendered with Persian numerals.
	public boolean persianDigits;

	// Creates options with default calendar and Latin digits.
	public JalaliFormatOptions() {}

	/**
	 * @param calendar {@code "jalali"}, {@code "gregorian"}, or {@code null} for default
	 * @param persianDigits whether to use Persian digits
	 */
	public JalaliFormatOptions(String calendar, boolean persianDigits) {
		this.calendar = calendar;
		this.persianDigits = persianDigits;
	}

	// Options that format with Persian digits.
	public static JalaliFormatOptions persianDigits() {
		return new JalaliFormatOptions(null, true);
	}

	// Options that format in the Gregorian calendar.
	public static JalaliFormatOptions gregorian() {
		return new JalaliFormatOptions("gregorian", false);
	}
}
