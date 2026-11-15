package com.jalalix.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class NormalizeQueryDateTest {

	@Test
	void fromPersianConvertsPersianAndArabicIndicDigits() {
		assertEquals("۱۴۰۳/۱۲/۰۷", JalaliX.toPersian("1403/12/07"));
		assertEquals("1403/12/07", JalaliX.fromPersian("۱۴۰۳/۱۲/۰۷"));
		assertEquals("1403/12/07", JalaliX.fromPersian("١٤٠٣/١٢/٠٧"));
	}

	@Test
	void parseJalaliAcceptsDotSeparator() {
		assertEquals("2025-02-25", JalaliX.parseJalali("1403.12.07").toISODate());
	}

	@Test
	void normalizeQueryDateSentinelsAndTimestamps() {
		assertNull(JalaliX.normalizeQueryDate(null));
		assertNull(JalaliX.normalizeQueryDate("null"));
		assertNull(JalaliX.normalizeQueryDate(""));
		assertNull(JalaliX.normalizeQueryDate("undefined"));
		assertNull(JalaliX.normalizeQueryDate("Invalid Date"));
		assertNull(JalaliX.normalizeQueryDate("NaN"));
		assertNull(JalaliX.normalizeQueryDate(0));
		assertNull(JalaliX.normalizeQueryDate(-1));

		long seconds = 1740430800L; // 2025-02-24 21:00:00 UTC / 2025-02-25 00:30:00 Tehran

		assertEquals(
			"2025-02-24 21:00:00",
			JalaliX.normalizeQueryDate(seconds, JalaliQueryOptions.ofTimezone("UTC")));
		assertEquals(
			"2025-02-25 00:30:00",
			JalaliX.normalizeQueryDate(seconds * 1000, JalaliQueryOptions.ofTimezone("Asia/Tehran")));
	}

	@Test
	void normalizeQueryDateJalaliAndGregorian() {
		assertEquals(
			"2025-02-25 00:00:00",
			JalaliX.normalizeQueryDate("1403/12/07", JalaliQueryOptions.ofTimezone("UTC")));
		assertEquals(
			"2025-02-25 14:30:00",
			JalaliX.normalizeQueryDate("۱۴۰۳-۱۲-۰۷ 14:30:00", JalaliQueryOptions.ofTimezone("UTC")));
		assertEquals("2025-02-25 00:00:00", JalaliX.normalizeQueryDate("2025.02.25"));
		assertEquals("2025-02-25 12:30:45", JalaliX.normalizeQueryDate("2025-02-25 12:30:45.123Z"));
		assertEquals("not-a-date", JalaliX.normalizeQueryDate("not-a-date"));

		// Wider default band than the old 1278-1600 heuristic.
		assertNotNull(JalaliX.normalizeQueryDate("1200/01/01", JalaliQueryOptions.ofTimezone("UTC")));
		assertNotNull(JalaliX.normalizeQueryDate("1800/01/01", JalaliQueryOptions.ofTimezone("UTC")));

		// Gregorian years stay Gregorian; override bounds when needed.
		assertEquals("2025-01-01 00:00:00", JalaliX.normalizeQueryDate("2025-01-01"));

		JalaliQueryOptions widened = JalaliQueryOptions.ofYearBand(1, 3000);
		widened.timezone = "UTC";
		assertNotEquals("2025-01-01 00:00:00", JalaliX.normalizeQueryDate("2025-01-01", widened));
	}
}
