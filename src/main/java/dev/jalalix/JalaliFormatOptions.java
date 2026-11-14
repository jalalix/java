package dev.jalalix;

/** Options for {@link JalaliX#format(String, JalaliFormatOptions)}. */
public final class JalaliFormatOptions {
	/** {@code "jalali"} (default) or {@code "gregorian"}. */
	public String calendar;
	public boolean persianDigits;

	public JalaliFormatOptions() {}

	public JalaliFormatOptions(String calendar, boolean persianDigits) {
		this.calendar = calendar;
		this.persianDigits = persianDigits;
	}

	public static JalaliFormatOptions persianDigits() {
		return new JalaliFormatOptions(null, true);
	}

	public static JalaliFormatOptions gregorian() {
		return new JalaliFormatOptions("gregorian", false);
	}
}
