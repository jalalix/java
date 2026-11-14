package dev.jalalix;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Jalali (Persian / Shamsi) calendar on a UTC millisecond instant + IANA {@link ZoneId}.
 * Pure {@code java.time} port of {@code @jalalix/typescript} — same algorithm and camelCase API.
 */
public final class JalaliX implements Cloneable {

	public static final String[] persianNumbers = {"۰", "۱", "۲", "۳", "۴", "۵", "۶", "۷", "۸", "۹"};

	private static final double LEAP_YEAR_DIVISION = 0.24219858156;
	private static final int FIRST_DAY_OF_WEEK = 6;
	private static final long DATE_LENGTH = 86_400_000L;
	private static final Pattern JALALI_PARSE = Pattern.compile(
		"^(\\d{4})[/-](\\d{1,2})[/-](\\d{1,2})(?:[ T](\\d{1,2}):(\\d{1,2})(?::(\\d{1,2}))?)?$");

	private static final Pattern DATE_ONLY = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

	private static final String[] WEEK_DAYS_NAMES = {
		"شنبه", "یکشنبه", "دوشنبه", "سه شنبه", "چهار شنبه", "پنج شنبه", "جمعه"
	};

	private static final String[] WEEK_DAYS_LETTERS = {"ش", "ی", "د", "س", "چ", "پ", "ج"};

	private static final String[] WEEK_DAYS_GREGORIAN_NAMES = {
		"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
	};

	private static final String[] WEEK_DAYS_GREGORIAN_LETTERS = {"S", "M", "T", "W", "T", "F", "S"};

	private static final String[] MONTHS_NAMES = {
		"فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
		"مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
	};

	private static final String[] MONTHS_GREGORIAN_NAMES = {
		"January", "February", "March", "April", "May", "June",
		"July", "August", "September", "October", "November", "December"
	};

	private long time;
	private String timezone = "UTC";
	private final JalaliDate jalali = new JalaliDate();
	private boolean isConverted = false;
	private final String am = "ق.ظ.";
	private final String pm = "ب.ظ.";
	private final String amGregorian = "AM";
	private final String pmGregorian = "PM";

	private static final class WallParts {
		int year, month, day, hour, minute, second, ms;
	}

	private static final class JalaliDate {
		int year = 1348;
		int month = 10;
		int day = 11;
		int hours = 0;
		int min = 0;
		int sec = 0;
		int ms = 0;
		boolean isLeapYear = false;
	}

	public JalaliX() {
		this.time = System.currentTimeMillis();
	}

	public JalaliX(long epochMs) {
		this.time = epochMs;
	}

	public JalaliX(String isoOrDate) {
		this.time = parseInput(isoOrDate);
	}

	public JalaliX(Instant instant) {
		this.time = instant.toEpochMilli();
	}

	public JalaliX(TemporalAccessor temporal) {
		this.time = Instant.from(temporal).toEpochMilli();
	}

	private ZoneId zoneId() {
		return ZoneId.of(timezone);
	}

	private static String hostTimezone() {
		return ZoneId.systemDefault().getId();
	}

	private WallParts getWallParts() {
		ZonedDateTime z = Instant.ofEpochMilli(time).atZone(zoneId());
		WallParts w = new WallParts();
		w.year = z.getYear();
		w.month = z.getMonthValue();
		w.day = z.getDayOfMonth();
		w.hour = z.getHour();
		w.minute = z.getMinute();
		w.second = z.getSecond();
		w.ms = Math.floorMod(time, 1000);
		return w;

	}

	// Date.UTC(y, m-1, d, h, mi, s) equivalent — civil components as UTC instant (ms truncated to seconds). 
	private static long dateUtc(int y, int m, int d, int h, int mi, int s) {
		return LocalDateTime.of(y, m, d, h, mi, s).toInstant(ZoneOffset.UTC).toEpochMilli();
	}

	private long wallToUtc(int y, int m, int d, int h, int mi, int s, int ms) {
		long want = dateUtc(y, m, d, h, mi, s);
		long utc = want;
		ZoneId zone = zoneId();
		for (int i = 0; i < 14; i++) {
			ZonedDateTime parts = Instant.ofEpochMilli(utc).atZone(zone);
			int hour = parts.getHour();
			long got = dateUtc(
				parts.getYear(),
				parts.getMonthValue(),
				parts.getDayOfMonth(),
				hour,
				parts.getMinute(),
				parts.getSecond());
			long diff = want - got;
			if (diff == 0) break;
			utc += diff;
		}
		return (utc / 1000) * 1000 + Math.floorMod(ms, 1000);
	}

	private JalaliDate toJalali() {
		if (!isConverted) {
			WallParts wall = getWallParts();
			long civilDay = dateUtc(wall.year, wall.month, wall.day, 0, 0, 0) / DATE_LENGTH;
			long baseDay = dateUtc(1970, 1, 1, 0, 0, 0) / DATE_LENGTH;
			long diffDays = civilDay - baseDay;

			jalali.year = 1348;
			jalali.month = 10;
			jalali.day = 11;

			if (diffDays > 0) {
				for (long i = 0; i < diffDays; i++) {
					jalali.day++;

					if (jalali.month < 7 && jalali.day > 31) {
						jalali.month++;
						jalali.day = 1;
					} else if (jalali.month > 6 && jalali.month < 12 && jalali.day > 30) {
						jalali.month++;
						jalali.day = 1;
					} else if (jalali.month == 12) {
						boolean leap = checkLeapYear(jalali.year);

						if (leap && jalali.day > 30) {
							jalali.year++;
							jalali.month = 1;
							jalali.day = 1;
						} else if (!leap && jalali.day > 29) {
							jalali.year++;
							jalali.month = 1;
							jalali.day = 1;
						}
					}
				}
			} else if (diffDays < 0) {
				for (long i = 0; i < -diffDays; i++) {
					jalali.day--;

					if (jalali.day < 1) {
						if (jalali.month == 1) {
							jalali.year--;
							jalali.month = 12;
							jalali.day = checkLeapYear(jalali.year) ? 30 : 29;
						} else if (jalali.month < 8) {
							jalali.month--;
							jalali.day = 31;
						} else if (jalali.month < 13) {
							jalali.month--;
							jalali.day = 30;
						}
					}
				}
			}

			jalali.hours = wall.hour;
			jalali.min = wall.minute;
			jalali.sec = wall.second;
			jalali.ms = wall.ms;
			jalali.isLeapYear = checkLeapYear(jalali.year);

			isConverted = true;
		}

		return jalali;
	}

	private long toGregorian(JalaliDate jDate) {
		WallParts wall = getWallParts();
		JalaliDate cur = new JalaliDate();
		cur.year = 1348;
		cur.month = 10;
		cur.day = 11;

		long diffDays = 0;

		boolean after =
			jDate.year > 1348
				|| (jDate.year == 1348 && jDate.month >= 10)
				|| (jDate.year == 1348 && jDate.month == 10 && jDate.day >= 11);

		boolean before =
			jDate.year < 1348
				|| (jDate.year == 1348 && jDate.month < 10)
				|| (jDate.year == 1348 && jDate.month == 10 && jDate.day < 11);

		if (after) {
			while (!(cur.year == jDate.year && cur.month == jDate.month && cur.day == jDate.day)) {
				diffDays++;
				cur.day++;

				if (cur.month < 7 && cur.day > 31) {
					cur.month++;
					cur.day = 1;
				} else if (cur.month > 6 && cur.month < 12 && cur.day > 30) {
					cur.month++;
					cur.day = 1;
				} else if (cur.month == 12) {
					boolean leap = checkLeapYear(cur.year);

					if (leap && cur.day > 30) {
						cur.year++;
						cur.month = 1;
						cur.day = 1;
					} else if (!leap && cur.day > 29) {
						cur.year++;
						cur.month = 1;
						cur.day = 1;
					}
				}
			}
		} else if (before) {
			while (!(cur.year == jDate.year && cur.month == jDate.month && cur.day == jDate.day)) {
				diffDays--;
				cur.day--;

				if (cur.day < 1) {
					if (cur.month == 1) {
						cur.year--;
						cur.month = 12;
						cur.day = checkLeapYear(cur.year) ? 30 : 29;
					} else if (cur.month < 8) {
						cur.month--;
						cur.day = 31;
					} else if (cur.month < 13) {
						cur.month--;
						cur.day = 30;
					}
				}
			}
		}

		int hours = jDate.hours;
		int min = jDate.min;
		int sec = jDate.sec;
		int ms = jDate.ms;

		// When called from normalizeJalali, hours come from current jalali cache / wall
		long wallAsUtc = diffDays * DATE_LENGTH + (hours * 3600L + min * 60L + sec) * 1000L + ms;
		Instant civil = Instant.ofEpochMilli(wallAsUtc);
		ZonedDateTime asUtc = civil.atZone(ZoneOffset.UTC);

		return wallToUtc(
			asUtc.getYear(),
			asUtc.getMonthValue(),
			asUtc.getDayOfMonth(),
			asUtc.getHour(),
			asUtc.getMinute(),
			asUtc.getSecond(),
			asUtc.getNano() / 1_000_000);
	}

	private boolean checkLeapYear(int year) {
		if (year > 0) {
			double checkYear = (year + 2346) * LEAP_YEAR_DIVISION;
			return checkYear - (long) checkYear < LEAP_YEAR_DIVISION;
		}

		return false;
	}

	private JalaliDate reset() {
		isConverted = false;
		jalali.year = 1348;
		jalali.month = 10;
		jalali.day = 11;

		return toJalali();
	}

	private String addZero(int number) {
		return (number > -1 && number < 10 ? "0" : "") + number;
	}

	private String getJalaliMonthName(int number) {
		return MONTHS_NAMES[number - 1];
	}

	private String getGregorianMonthName(int number) {
		return MONTHS_GREGORIAN_NAMES[number - 1];
	}

	private int getWallWeekday() {
		WallParts wall = getWallParts();

		return Instant.ofEpochMilli(dateUtc(wall.year, wall.month, wall.day, 0, 0, 0))
			.atZone(ZoneOffset.UTC)
			.getDayOfWeek()
			.getValue()
			% 7;
		// Java Mon=1..Sun=7 -> convert: Sun=0 like JS getUTCDay
		// Wait: getValue()%7 gives Mon=1..Sat=6,Sun=0. JS: Sun=0 Mon=1 ... Sat=6.
		// Java DayOfWeek: MONDAY=1 ... SUNDAY=7. So Sunday->0 via %7, Monday->1, ... Saturday->6. Perfect.
	}

	private int getGregorianDayOfYear() {
		WallParts wall = getWallParts();
		long start = dateUtc(wall.year, 1, 1, 0, 0, 0) - DATE_LENGTH; // JS Date.UTC(year,0,0)
		long civil = dateUtc(wall.year, wall.month, wall.day, 0, 0, 0);

		return (int) Math.round((civil - start) / (double) DATE_LENGTH);
	}

	private int getGregorianWeekOfYear() {
		WallParts wall = getWallParts();
		ZonedDateTime d = Instant.ofEpochMilli(dateUtc(wall.year, wall.month, wall.day, 0, 0, 0)).atZone(ZoneOffset.UTC);
		int utcDay = d.getDayOfWeek().getValue() % 7; // JS getUTCDay
		int adjust = 4 - (utcDay == 0 ? 7 : utcDay);
		d = d.plusDays(adjust);
		ZonedDateTime yearStart = LocalDate.of(d.getYear(), 1, 1).atStartOfDay(ZoneOffset.UTC);

		return (int) Math.ceil(((d.toInstant().toEpochMilli() - yearStart.toInstant().toEpochMilli()) / (double) DATE_LENGTH + 1) / 7.0);
	}

	private String getGregorianWeekDayName() {
		return WEEK_DAYS_GREGORIAN_NAMES[getWallWeekday()];
	}

	private String getGregorianWeekDayLetter() {
		return WEEK_DAYS_GREGORIAN_LETTERS[getWallWeekday()];
	}

	private String getWeekDayName() {
		return WEEK_DAYS_NAMES[(getDay() + FIRST_DAY_OF_WEEK) % 7];
	}

	private String getWeekDayLetter() {
		return WEEK_DAYS_LETTERS[(getDay() + FIRST_DAY_OF_WEEK) % 7];
	}

	private String getUTCTimezone() {
		WallParts wall = getWallParts();
		long asUtc = dateUtc(wall.year, wall.month, wall.day, wall.hour, wall.minute, wall.second) + wall.ms;
		int offsetMin = (int) Math.round((asUtc - time) / 60000.0);
		String sign = offsetMin >= 0 ? "+" : "-";
		int abs = Math.abs(offsetMin);
		int hh = abs / 60;
		int mm = abs % 60;

		return " GMT" + sign + addZero(hh) + addZero(mm);
	}

	private long normalizeDate(int days) {
		time = time + days * DATE_LENGTH;

		return time;
	}

	private long normalizeJalali(JalaliDate jDate, int year, int month, int day) {
		if (month < 1) {
			int yearDiff = (int) Math.ceil((1 - month) / 12.0);
			year -= yearDiff;
			month = 12 * yearDiff + month;
		} else if (month > 12) {
			int yearDiff = (int) Math.floor((month - 1) / 12.0);
			year += yearDiff;
			month = month - 12 * yearDiff;
		}

		if (month > 6 && month < 12 && day > 30) day = 30;
		else if (month == 12) {
			boolean leap = checkLeapYear(year);
			if (leap && day > 30) day = 30;
			else if (!leap && day > 29) day = 29;
		}

		JalaliDate next = new JalaliDate();
		next.year = year;
		next.month = month;
		next.day = day;
		next.hours = jDate.hours;
		next.min = jDate.min;
		next.sec = jDate.sec;
		next.ms = jDate.ms;
		time = toGregorian(next);

		return time;
	}

	private String normalizeTimezone(String tz) {
		if (tz == null) return timezone;

		String lowered = tz.toLowerCase(Locale.ROOT);

		if (lowered.equals("default") || lowered.equals("local") || lowered.equals("system")) {
			return hostTimezone();
		}

		if (lowered.equals("utc") || lowered.equals("gmt")) return "UTC";

		try {
			ZoneId z = ZoneId.of(tz);
			return z.getId();
		} catch (DateTimeException e) {
			throw new IllegalArgumentException("Invalid IANA timezone: " + tz, e);
		}
	}

	@Override
	public String toString() {
		JalaliDate j = toJalali();

		return getWeekDayName() + " " + addZero(j.day) + " " + getJalaliMonthName(j.month) + " " + addZero(j.year)
			+ " " + j.hours + ":" + addZero(j.min) + ":" + addZero(j.sec) + getUTCTimezone();
	}

	public String toDateString() {
		JalaliDate j = toJalali();

		return getWeekDayName() + " " + addZero(j.day) + " " + getJalaliMonthName(j.month) + " " + addZero(j.year);
	}

	public String toLocaleDateString() {
		JalaliDate j = toJalali();

		return addZero(j.year) + "/" + addZero(j.month) + "/" + addZero(j.day);
	}

	public String toLocaleTimeString() {
		JalaliDate j = toJalali();

		return (j.hours % 12) + ":" + addZero(j.min) + ":" + addZero(j.sec) + " " + getMeridiem();
	}

	public int getFullYear() {
		return toJalali().year;
	}

	public int getMonth() {
		return toJalali().month;
	}

	public int getDate() {
		return toJalali().day;
	}

	public int getDay() {
		return (getWallWeekday() - FIRST_DAY_OF_WEEK + 7 + 1) % 7;
	}

	public int getHours() {
		return getWallParts().hour;
	}

	public int getMinutes() {
		return getWallParts().minute;
	}

	public int getSeconds() {
		return getWallParts().second;
	}

	public int getMilliseconds() {
		return Math.floorMod(time, 1000);
	}

	public long getTime() {
		return time;
	}

	public long setTime(long t) {
		this.time = t;
		reset();

		return getTime();
	}

	public long setMilliseconds(int ms) {
		WallParts wall = getWallParts();
		time = wallToUtc(wall.year, wall.month, wall.day, wall.hour, wall.minute, wall.second, ms);
		reset();

		return getTime();
	}

	public long setSeconds(int sec) {
		return setSeconds(sec, getWallParts().ms);
	}

	public long setSeconds(int sec, int ms) {
		WallParts wall = getWallParts();
		time = wallToUtc(wall.year, wall.month, wall.day, wall.hour, wall.minute, sec, ms);
		reset();

		return getTime();
	}

	public long setMinutes(int min) {
		WallParts w = getWallParts();

		return setMinutes(min, w.second, w.ms);
	}

	public long setMinutes(int min, int sec) {
		return setMinutes(min, sec, getWallParts().ms);
	}

	public long setMinutes(int min, int sec, int ms) {
		WallParts wall = getWallParts();
		time = wallToUtc(wall.year, wall.month, wall.day, wall.hour, min, sec, ms);
		reset();

		return getTime();
	}

	public long setHours(int hours) {
		WallParts w = getWallParts();

		return setHours(hours, w.minute, w.second, w.ms);
	}

	public long setHours(int hours, int min) {
		WallParts w = getWallParts();

		return setHours(hours, min, w.second, w.ms);
	}

	public long setHours(int hours, int min, int sec) {
		return setHours(hours, min, sec, getWallParts().ms);
	}

	public long setHours(int hours, int min, int sec, int ms) {
		WallParts wall = getWallParts();
		time = wallToUtc(wall.year, wall.month, wall.day, hours, min, sec, ms);
		reset();

		return getTime();
	}

	public long setDate(int date) {
		JalaliDate j = toJalali();
		normalizeDate(date - j.day);
		reset();

		return getTime();
	}

	public long setMonth(int month) {
		return setMonth(month, null);
	}

	public long setMonth(int month, Integer date) {
		JalaliDate j = toJalali();
		normalizeJalali(j, j.year, month + 1, j.day);
		reset();

		if (date != null) {
			j = toJalali();
			normalizeDate(date - j.day);
			reset();
		}

		return getTime();
	}

	public long setFullYear(int year) {
		return setFullYear(year, null, null);
	}

	public long setFullYear(int year, Integer month) {
		return setFullYear(year, month, null);
	}

	public long setFullYear(int year, Integer month, Integer date) {
		JalaliDate j = toJalali();
		normalizeJalali(j, year, month != null ? month + 1 : j.month, j.day);
		reset();

		if (date != null) {
			j = toJalali();
			normalizeDate(date - j.day);
			reset();
		}

		return getTime();
	}

	private static long parseInput(Object date) {
		if (date instanceof Number) return ((Number) date).longValue();
		if (date instanceof JalaliX) return ((JalaliX) date).getTime();
		if (date instanceof Instant) return ((Instant) date).toEpochMilli();
		if (date instanceof TemporalAccessor) return Instant.from((TemporalAccessor) date).toEpochMilli();
		if (date instanceof String) return parseIsoString((String) date);

		throw new IllegalArgumentException("JalaliX.create: unsupported date type");
	}

	private static long parseIsoString(String value) {
		String s = value.trim();

		if (DATE_ONLY.matcher(s).matches()) {
			LocalDate d = LocalDate.parse(s);
			return d.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
		}

		if (s.endsWith("Z") || s.endsWith("z")) {
			return Instant.parse(s.toUpperCase(Locale.ROOT).replace("Z", "Z")).toEpochMilli();
		}

		try {
			if (s.contains("T")) {
				// with offset
				try {
					return Instant.parse(s).toEpochMilli();
				} catch (DateTimeParseException ignored) {
				}

				try {
					return DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(s, Instant::from).toEpochMilli();
				} catch (DateTimeParseException ignored) {
				}

				LocalDateTime ldt = LocalDateTime.parse(s);

				return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
			}

			return Instant.parse(s).toEpochMilli();
		} catch (DateTimeParseException e) {
			throw new IllegalArgumentException("JalaliX.create: invalid date string \"" + value + "\"", e);
		}
	}

	public static JalaliX create(Object date) {
		return create(date, null);
	}

	public static JalaliX create(Object date, JalaliOptions options) {
		JalaliX output = new JalaliX(parseInput(date));

		if (options != null && options.timezone != null) {
			output = output.setTimezone(options.timezone);
		}

		return output;
	}

	public static JalaliX parseJalali(String value) {
		return parseJalali(value, null);
	}

	public static JalaliX parseJalali(String value, JalaliOptions options) {
		String normalized = fromPersian(String.valueOf(value)).trim();
		Matcher match = JALALI_PARSE.matcher(normalized);

		if (!match.matches()) {
			throw new IllegalArgumentException("JalaliX.parseJalali: invalid Jalali date \"" + value + "\"");
		}

		int year = Integer.parseInt(match.group(1));
		int month = Integer.parseInt(match.group(2));
		int day = Integer.parseInt(match.group(3));
		int hours = match.group(4) != null ? Integer.parseInt(match.group(4)) : 0;
		int minutes = match.group(5) != null ? Integer.parseInt(match.group(5)) : 0;
		int seconds = match.group(6) != null ? Integer.parseInt(match.group(6)) : 0;

		if (month < 1 || month > 12 || day < 1 || day > 31) {
			throw new IllegalArgumentException("JalaliX.parseJalali: invalid Jalali date \"" + value + "\"");
		}

		JalaliX output = create(System.currentTimeMillis(), options);
		output.setFullYear(year, month - 1, day);
		output.setHours(hours, minutes, seconds, 0);

		return output;
	}

	public static boolean isValidDate(Object date) {
		if (date instanceof JalaliX) return true;
		if (date instanceof Instant) return true;

		if (date instanceof TemporalAccessor) {
			try {
				Instant.from((TemporalAccessor) date);

				return true;
			} catch (Exception e) {
				return false;
			}
		}

		if (date instanceof Number) return true;

		return false;
	}

	public static String toPersian(String str) {
		String out = str;

		for (int i = 0; i <= 9; i++) {
			out = out.replace(String.valueOf(i), persianNumbers[i]);
		}

		return out;
	}

	public static String fromPersian(String str) {
		String out = str;

		for (int i = 0; i <= 9; i++) {
			out = out.replace(persianNumbers[i], String.valueOf(i));
		}

		return out;
	}

	public static boolean compare(JalaliX date, JalaliX comparing) {
		return date.getTime() < comparing.getTime();
	}

	/** Creates a copy with the same instant and timezone. */
	@Override
	public JalaliX clone() {
		JalaliX output = new JalaliX(time);
		output.timezone = this.timezone;

		return output;
	}

	public String getTimezone() {
		return timezone;
	}

	public int getWeekOfYear() {
		int dayOfYear = getDayOfYear();
		int dayOfWeek = new JalaliX(time).setTimezone(timezone).getStartOfYear().getDayISO();

		return (int) Math.ceil((dayOfYear + dayOfWeek - 1) / 7.0);
	}

	public int getDayOfYear() {
		JalaliDate j = toJalali();
		int output = j.day;

		if (j.month < 7) output += (j.month - 1) * 31;
		else if (j.month < 12) output += 186 + (j.month - 7) * 30;
		else output += 336;
		return output;
	}

	public int getDayISO() {
		return ((getDay() + 6) % 7) + 1;
	}

	public int getHoursISO() {
		int hours = getHours();

		return hours > 12 ? hours - 12 : hours;
	}

	public String getMeridiem() {
		JalaliDate j = toJalali();

		return j.hours > 12 ? pm : am;
	}

	private String getGregorianMeridiem() {
		return getHours() >= 12 ? pmGregorian : amGregorian;
	}

	public JalaliX getStartOfYear() {
		return addDays(-1 * getDayOfYear() + 1).getStartOfDay();
	}

	public JalaliX getStartOfMonth() {
		return addDays(-1 * getDate() + 1).getStartOfDay();
	}

	public JalaliX getStartOfWeek() {
		return addDays(-1 * getDayISO() + 1).getStartOfDay();
	}

	public JalaliX getStartOfDay() {
		setHours(0, 0, 0, 0);

		return this;
	}

	public JalaliX getEndOfYear() {
		JalaliDate j = toJalali();
		int lastDay = j.isLeapYear ? 366 : 365;

		return addDays(lastDay - getDayOfYear()).getEndOfDay();
	}

	public JalaliX getEndOfMonth() {
		JalaliDate j = toJalali();
		int lastDay = 31;

		if (j.month > 6 && j.month < 12) lastDay = 30;
		else if (j.month == 12) lastDay = j.isLeapYear ? 30 : 29;
		return addDays(lastDay - getDate()).getEndOfDay();
	}

	public JalaliX getEndOfWeek() {
		return addDays(7 - getDayISO()).getEndOfDay();
	}

	public JalaliX getEndOfDay() {
		setHours(23, 59, 59, 999);
		return this;
	}

	public JalaliX setTimezone(String timezone) {
		JalaliX output = new JalaliX(getTime());
		output.timezone = normalizeTimezone(timezone);

		return output;
	}

	public JalaliX addYears(int year) {
		JalaliDate j = toJalali();
		normalizeJalali(j, j.year + year, j.month, j.day);
		reset();

		return this;
	}

	public JalaliX addMonths(int month) {
		JalaliDate j = toJalali();
		normalizeJalali(j, j.year, j.month + month, j.day);
		reset();

		return this;
	}

	public JalaliX addWeeks(int weeks) {
		normalizeDate(weeks * 7);
		reset();

		return this;
	}

	public JalaliX addDays(int days) {
		normalizeDate(days);
		reset();

		return this;
	}

	public String toISODate() {
		return toISODate("gregorian");
	}

	public String toISODate(String calendar) {
		if ("jalali".equals(calendar)) return format("YYYY-MM-DD");

		WallParts wall = getWallParts();

		return wall.year + "-" + addZero(wall.month) + "-" + addZero(wall.day);
	}

	public String format(String str) {
		return format(str, false, false);
	}

	public String format(String str, boolean persianDigits) {
		return format(str, persianDigits, false);
	}

	public String format(String str, JalaliFormatOptions options) {
		boolean persian = options != null && options.persianDigits;
		boolean gregorian = options != null && "gregorian".equals(options.calendar);

		return format(str, persian, gregorian);
	}

	public String format(String str, boolean persian, boolean gregorian) {
		boolean useGregorian = gregorian;
		List<String> placeholders = new ArrayList<>();

		java.util.function.Function<String, String> ph = val -> {
			int i = placeholders.size();
			placeholders.add(val);
			return "\uE000" + i + "\uE001";
		};

		String output = str;
		WallParts wall = getWallParts();
		int year = useGregorian ? wall.year : getFullYear();
		int month = useGregorian ? wall.month : getMonth();
		int weekOfYear = useGregorian ? getGregorianWeekOfYear() : getWeekOfYear();
		int dayOfYear = useGregorian ? getGregorianDayOfYear() : getDayOfYear();
		int date = useGregorian ? wall.day : getDate();
		String monthName = useGregorian ? getGregorianMonthName(month) : getJalaliMonthName(month);
		String weekDayName = useGregorian ? getGregorianWeekDayName() : getWeekDayName();
		String weekDayLetter = useGregorian ? getGregorianWeekDayLetter() : getWeekDayLetter();
		String meridiem = useGregorian ? getGregorianMeridiem() : getMeridiem();
		String yearStr = Integer.toString(year);

		if (output.contains("YYYY")) output = output.replace("YYYY", ph.apply(yearStr));
		if (output.contains("YYY")) output = output.replace("YYY", ph.apply(yearStr.length() >= 3 ? yearStr.substring(yearStr.length() - 3) : yearStr));
		if (output.contains("YY")) output = output.replace("YY", ph.apply(yearStr.length() >= 2 ? yearStr.substring(yearStr.length() - 2) : yearStr));
		if (output.contains("Y")) output = output.replace("Y", ph.apply(yearStr.substring(yearStr.length() - 1)));

		if (output.contains("MMM")) output = output.replace("MMM", ph.apply(monthName));
		if (output.contains("MM")) output = output.replace("MM", ph.apply(addZero(month)));
		if (output.contains("M")) output = output.replace("M", ph.apply(Integer.toString(month)));

		if (output.contains("W")) output = output.replace("W", ph.apply(addZero(weekOfYear)));
		if (output.contains("w")) output = output.replace("w", ph.apply(Integer.toString(weekOfYear)));

		if (output.contains("DW")) output = output.replace("DW", ph.apply(Integer.toString(getDayISO())));

		if (output.contains("dw")) {
			int dw = useGregorian ? (getWallWeekday() == 0 ? 7 : getWallWeekday()) : getDay();
			output = output.replace("dw", ph.apply(Integer.toString(dw)));
		}

		if (output.contains("WN")) output = output.replace("WN", ph.apply(weekDayName));
		if (output.contains("wn")) output = output.replace("wn", ph.apply(weekDayName));
		if (output.contains("WL")) output = output.replace("WL", ph.apply(weekDayLetter));
		if (output.contains("wl")) output = output.replace("wl", ph.apply(weekDayLetter));

		if (output.contains("DDDD")) output = output.replace("DDDD", ph.apply(addZero(dayOfYear)));
		if (output.contains("DDD")) output = output.replace("DDD", ph.apply(Integer.toString(dayOfYear)));
		if (output.contains("DD")) output = output.replace("DD", ph.apply(addZero(date)));
		if (output.contains("D")) output = output.replace("D", ph.apply(Integer.toString(date)));

		int hours = getHours();
		if (output.contains("HH")) output = output.replace("HH", ph.apply(addZero(hours)));
		if (output.contains("H")) output = output.replace("H", ph.apply(Integer.toString(hours)));

		int hoursISO = hours > 12 ? hours - 12 : (hours == 0 ? 12 : hours);
		if (output.contains("hh")) output = output.replace("hh", ph.apply(addZero(hoursISO)));
		if (output.contains("h")) output = output.replace("h", ph.apply(Integer.toString(hoursISO)));

		if (output.contains("mm")) output = output.replace("mm", ph.apply(addZero(getMinutes())));
		if (output.contains("m")) output = output.replace("m", ph.apply(Integer.toString(getMinutes())));
		if (output.contains("ss")) output = output.replace("ss", ph.apply(addZero(getSeconds())));
		if (output.contains("s")) output = output.replace("s", ph.apply(Integer.toString(getSeconds())));
		if (output.contains("sss")) output = output.replace("sss", ph.apply(Integer.toString(getMilliseconds())));
		if (output.contains("SSS")) output = output.replace("SSS", ph.apply(Integer.toString(getMilliseconds())));

		if (output.contains("A")) output = output.replace("A", ph.apply(meridiem));
		if (output.contains("a")) output = output.replace("a", ph.apply(meridiem));

		if (output.contains("X")) output = output.replace("X", ph.apply(Long.toString(getTime() / 1000)));
		if (output.contains("x")) output = output.replace("x", ph.apply(Long.toString(getTime())));

		for (int i = 0; i < placeholders.size(); i++) {
			output = output.replace("\uE000" + i + "\uE001", placeholders.get(i));
		}

		return persian ? toPersian(output) : output;
	}
}
