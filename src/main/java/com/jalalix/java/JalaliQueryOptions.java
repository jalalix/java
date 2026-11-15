package com.jalalix.java;

// Options for {@link JalaliX#normalizeQueryDate}.
public final class JalaliQueryOptions {
	// IANA timezone id, or aliases like {@code utc}, {@code local}.
	public String timezone;

	// Lower bound (inclusive) of the Jalali year band; defaults to {@code 1} when {@code null}.
	public Integer jalaliYearMin;

	// Upper bound (inclusive) of the Jalali year band; defaults to {@code 1899} when {@code null}.
	public Integer jalaliYearMax;

	// Creates options with all defaults.
	public JalaliQueryOptions() {}

	/** @param timezone IANA timezone id or alias */
	public JalaliQueryOptions(String timezone) {
		this.timezone = timezone;
	}

	// Creates options with the given timezone.
	public static JalaliQueryOptions ofTimezone(String timezone) {
		return new JalaliQueryOptions(timezone);
	}

	// Creates options with the given Jalali year band.
	public static JalaliQueryOptions ofYearBand(int jalaliYearMin, int jalaliYearMax) {
		JalaliQueryOptions options = new JalaliQueryOptions();
		options.jalaliYearMin = jalaliYearMin;
		options.jalaliYearMax = jalaliYearMax;

		return options;
	}
}
