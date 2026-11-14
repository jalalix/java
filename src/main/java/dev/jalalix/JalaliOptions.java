package dev.jalalix;

/** Options for {@link JalaliX#create} / {@link JalaliX#parseJalali}. */
public final class JalaliOptions {
	public String timezone;

	public JalaliOptions() {}

	public JalaliOptions(String timezone) {
		this.timezone = timezone;
	}

	public static JalaliOptions ofTimezone(String timezone) {
		return new JalaliOptions(timezone);
	}
}
