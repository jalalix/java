# JalaliX - Java

Jalali (Persian / Shamsi) calendar for Java 17+. Pure `java.time` (`ZoneId`), no runtime dependencies beyond the JDK.

[![license](https://img.shields.io/badge/license-MIT-blue.svg)](./LICENSE)

```java
import com.jalalix.java.JalaliX;
import com.jalalix.java.JalaliFormatOptions;

JalaliX date = JalaliX.create("2025-02-25");

date.format("YYYY/MM/DD"); // 1403/12/07
date.format("DD MMM YYYY", JalaliFormatOptions.persianDigits()); // ۰۷ اسفند ۱۴۰۳

JalaliX.parseJalali("1403/12/07").toISODate(); // 2025-02-25
```

`toISODate()` uses wall-clock getters in the active timezone.

## Install

Maven / Gradle coordinates: `com.jalalix.java:jalalix:0.1.0`

```xml
<dependency>
  <groupId>com.jalalix.java</groupId>
  <artifactId>jalalix</artifactId>
  <version>0.1.0</version>
</dependency>
```

```kotlin
implementation("com.jalalix.java:jalalix:0.1.0")
```

```bash
./gradlew test
./gradlew publishToMavenLocal
./gradlew publishToMavenCentral
```

## Usage

### create / format

```java
JalaliX now = JalaliX.create(System.currentTimeMillis(), JalaliOptions.ofTimezone("Asia/Tehran"));

now.format("YYYY/MM/DD");
now.format("WN، DD MMM YYYY");
now.format("YYYY/MM/DD", JalaliFormatOptions.persianDigits());
now.format("DD MMM YYYY", JalaliFormatOptions.gregorian());
```

`format` also supports `format(str, persianDigits, gregorian)` and `format(str, JalaliFormatOptions)`.

### parseJalali

```java
JalaliX.parseJalali("1403/12/07");
JalaliX.parseJalali("۱۴۰۳-۱۲-۰۷ 14:30");
JalaliX.parseJalali("1403.12.07");
```

Named `parseJalali` because `java.time` already provides parsers. Accepts `/`, `-`, or `.` as the date separator.

### normalizeQueryDate

Soft-normalize loose API/query date values to a Gregorian `YYYY-MM-DD HH:mm:ss` string (or `null`). Does not throw on junk input.

```java
JalaliX.normalizeQueryDate("1403/12/07", JalaliQueryOptions.ofTimezone("Asia/Tehran"));
// "2025-02-25 00:00:00"

JalaliX.normalizeQueryDate("undefined"); // null
JalaliX.normalizeQueryDate(1740430800000L, JalaliQueryOptions.ofTimezone("Asia/Tehran"));
// "2025-02-25 00:30:00"
```

Handles sentinels, unix timestamps (seconds or milliseconds; non-positive → `null`), Persian/Arabic-Indic digits, `/` `-` `.` separators, fractional seconds, optional `Z` / offset suffixes.

Detects Jalali when the year is in `jalaliYearMin`…`jalaliYearMax` (default `1`…`1899`) **and** the civil date is valid; otherwise Gregorian when valid, or returns the digit-cleaned string. Non-string/non-number values pass through unchanged.

### toISODate

```java
JalaliX d = JalaliX.create("2025-02-25T00:30:00+03:30");

d.toISODate(); // 2025-02-25
d.toISODate("jalali"); // 1403-12-07
```

### add / start / end

```java
JalaliX.create("2025-02-25").addMonths(1).getStartOfMonth().format("YYYY/MM/DD");
// e.g. 1404/01/01

JalaliX d = JalaliX.create("2025-02-25", JalaliOptions.ofTimezone("Asia/Tehran"));
d.addDays(7).format("YYYY/MM/DD");
d.getEndOfWeek().format("YYYY/MM/DD");
```

### Persian / Arabic digits

```java
JalaliX.toPersian("1403/12/07"); // ۱۴۰۳/۱۲/۰۷
JalaliX.fromPersian("۱۴۰۳/۱۲/۰۷"); // 1403/12/07
JalaliX.fromPersian("١٤٠٣/١٢/٠٧"); // 1403/12/07 (Arabic-Indic)
```

## Timezone

Internal storage is always a **UTC Unix millisecond instant** plus an IANA timezone id (`ZoneId`). `setTimezone` is **instant-preserving**: `getTime()` stays the same; wall clock / Jalali day may change.

```java
JalaliX d = JalaliX.create(1740430800000L);

JalaliX tehran = d.setTimezone("Asia/Tehran");
tehran.format("YYYY/MM/DD"); // 1403/12/07
tehran.format("HH:mm"); // 00:30

JalaliX utc = d.setTimezone("UTC");
utc.format("YYYY/MM/DD"); // 1403/12/06
utc.format("HH:mm"); // 21:00

tehran.getTime() == utc.getTime(); // true
```

- **Default zone:** `UTC`
- **Aliases:** `utc` / `gmt` → `UTC`; `local` / `default` / `system` → `ZoneId.systemDefault()`
- **Invalid zone** → `IllegalArgumentException`

```java
JalaliX.create(System.currentTimeMillis(), JalaliOptions.ofTimezone("Asia/Tehran"));
```

## Months

Easy to get wrong:

- `getMonth()` → **1…12** (Farvardin = 1)
- `setMonth(m)` / `setFullYear(y, m)` → **0…11**, same as JavaScript `Date` (Farvardin = 0)

```java
JalaliX d = JalaliX.create(System.currentTimeMillis());
d.setFullYear(1404, 0, 1); // 1 Farvardin 1404
d.getMonth(); // 1
```

## Format tokens

| Token               | Meaning                         |
| ------------------- | ------------------------------- |
| `YYYY` `YY`         | year                            |
| `MMM` `MM` `M`      | month name / number             |
| `DD` `D`            | day                             |
| `HH` `hh` `mm` `ss` | time                            |
| `A` `a`             | meridiem (ق.ظ. / ب.ظ. or AM/PM) |
| `WN` `WL`           | weekday name / letter           |
| `w` `DDD`           | week of year / day of year      |
| `X` `x`             | unix seconds / ms               |

## API

Option types: `JalaliOptions`, `JalaliQueryOptions`, `JalaliFormatOptions`.

### Constructors

| API                                  | Description                                            |
| ------------------------------------ | ------------------------------------------------------ |
| `JalaliX()`                          | Current instant in UTC.                                |
| `JalaliX(long epochMs)`              | From Unix epoch milliseconds.                          |
| `JalaliX(String isoOrDate)`          | From ISO-8601 / `YYYY-MM-DD` string.                   |
| `JalaliX(Instant instant)`           | From `java.time.Instant`.                              |
| `JalaliX(TemporalAccessor temporal)` | From temporal accessor convertible via `Instant.from`. |

### Static fields

| API              | Description                                     |
| ---------------- | ----------------------------------------------- |
| `persianNumbers` | Array of Persian digit characters `۰`…`۹`.      |
| `arabicNumbers`  | Array of Arabic-Indic digit characters `٠`…`٩`. |

### Static methods

| API                                                            | Description                                                               |
| -------------------------------------------------------------- | ------------------------------------------------------------------------- |
| `create(Object date)`                                          | Build from unix ms, ISO string, `JalaliX`, or temporal (default options). |
| `create(Object date, JalaliOptions options)`                   | Same with timezone options.                                               |
| `parseJalali(String value)`                                    | Parse Jalali date string (default options).                               |
| `parseJalali(String value, JalaliOptions options)`             | Parse with optional timezone.                                             |
| `normalizeQueryDate(Object value)`                             | Soft-normalize query values (default query options).                      |
| `normalizeQueryDate(Object value, JalaliQueryOptions options)` | Soft-normalize with timezone and year band.                               |
| `isValidDate(Object date)`                                     | `true` if the value is a supported date type.                             |
| `toPersian(String str)`                                        | Replace Western digits with Persian digits.                               |
| `fromPersian(String str)`                                      | Replace Persian and Arabic-Indic digits with Western digits.              |
| `compare(JalaliX date, JalaliX comparing)`                     | `true` if the first is strictly before the second.                        |

### Instance - getters

| API                 | Description                                                           |
| ------------------- | --------------------------------------------------------------------- |
| `getFullYear()`     | Jalali year in the active timezone.                                   |
| `getMonth()`        | Jalali month **1…12** (Farvardin = 1).                                |
| `getDate()`         | Jalali day of month **1…31**.                                         |
| `getDay()`          | Weekday **1…7** (Saturday = 1 … Friday = 7).                          |
| `getHours()`        | Wall-clock hours in the active timezone.                              |
| `getMinutes()`      | Wall-clock minutes.                                                   |
| `getSeconds()`      | Wall-clock seconds.                                                   |
| `getMilliseconds()` | Wall-clock milliseconds.                                              |
| `getTime()`         | Unix timestamp in milliseconds (UTC instant).                         |
| `getTimezone()`     | Active IANA timezone id.                                              |
| `getWeekOfYear()`   | Jalali week-of-year (1-based).                                        |
| `getDayOfYear()`    | Day of Jalali year (1…365/366).                                       |
| `getDayISO()`       | ISO-style weekday index used internally for week math.                |
| `getHoursISO()`     | Hours in 0…23 for formatting helpers.                                 |
| `getMeridiem()`     | Localized meridiem string (ق.ظ./ب.ظ. or AM/PM depending on calendar). |

### Instance - setters

| API                                                  | Description                                             |
| ---------------------------------------------------- | ------------------------------------------------------- |
| `setTime(long t)`                                    | Set the UTC instant from unix milliseconds.             |
| `setMilliseconds(int ms)`                            | Set milliseconds (wall clock).                          |
| `setSeconds(int sec)`                                | Set seconds; keep current milliseconds.                 |
| `setSeconds(int sec, int ms)`                        | Set seconds and milliseconds.                           |
| `setMinutes(int min)`                                | Set minutes; keep current lower units.                  |
| `setMinutes(int min, int sec)`                       | Set minutes and seconds.                                |
| `setMinutes(int min, int sec, int ms)`               | Set minutes, seconds, and milliseconds.                 |
| `setHours(int hours)`                                | Set hours; keep current lower units.                    |
| `setHours(int hours, int min)`                       | Set hours and minutes.                                  |
| `setHours(int hours, int min, int sec)`              | Set hours, minutes, and seconds.                        |
| `setHours(int hours, int min, int sec, int ms)`      | Set full wall-clock time.                               |
| `setDate(int date)`                                  | Set Jalali day of month.                                |
| `setMonth(int month)`                                | Set Jalali month **0…11** (Farvardin = 0).              |
| `setMonth(int month, Integer date)`                  | Set Jalali month **0…11** with optional day.            |
| `setFullYear(int year)`                              | Set Jalali year only.                                   |
| `setFullYear(int year, Integer month)`               | Set Jalali year and optional month **0…11**.            |
| `setFullYear(int year, Integer month, Integer date)` | Set Jalali year, optional month **0…11**, and day.      |
| `setTimezone(String timezone)`                       | Instant-preserving zone change; returns a new instance. |

### Instance - math, range, format

| API                                                      | Description                                           |
| -------------------------------------------------------- | ----------------------------------------------------- |
| `addYears(int year)`                                     | Add calendar years (mutates / returns `this`).        |
| `addMonths(int month)`                                   | Add calendar months.                                  |
| `addWeeks(int weeks)`                                    | Add weeks.                                            |
| `addDays(int days)`                                      | Add days.                                             |
| `getStartOfYear()`                                       | Start of Jalali year (mutates / returns `this`).      |
| `getStartOfMonth()`                                      | Start of Jalali month.                                |
| `getStartOfWeek()`                                       | Start of week.                                        |
| `getStartOfDay()`                                        | Start of day.                                         |
| `getEndOfYear()`                                         | End of Jalali year.                                   |
| `getEndOfMonth()`                                        | End of Jalali month.                                  |
| `getEndOfWeek()`                                         | End of week.                                          |
| `getEndOfDay()`                                          | End of day.                                           |
| `format(String str)`                                     | Format with default Jalali calendar and Latin digits. |
| `format(String str, boolean persianDigits)`              | Format with optional Persian digits.                  |
| `format(String str, JalaliFormatOptions options)`        | Format with calendar and digit options.               |
| `format(String str, boolean persian, boolean gregorian)` | Positional format flags.                              |
| `toISODate()`                                            | Gregorian `YYYY-MM-DD` (default).                     |
| `toISODate(String calendar)`                             | `YYYY-MM-DD` in `"gregorian"` or `"jalali"`.          |
| `clone()`                                                | Copy with the same instant and timezone.              |
| `toString()`                                             | Human-readable Jalali date/time string.               |
| `toDateString()`                                         | Jalali weekday + date string.                         |
| `toLocaleDateString()`                                   | `YYYY/MM/DD` in Jalali.                               |
| `toLocaleTimeString()`                                   | 12-hour time with meridiem.                           |

### Option helpers

| API                                               | Description                             |
| ------------------------------------------------- | --------------------------------------- |
| `JalaliOptions.ofTimezone(String timezone)`       | Options with IANA timezone.             |
| `JalaliQueryOptions.ofTimezone(String timezone)`  | Query options with timezone.            |
| `JalaliQueryOptions.ofYearBand(int min, int max)` | Query options with Jalali year band.    |
| `JalaliFormatOptions.persianDigits()`             | Format with Persian digits.             |
| `JalaliFormatOptions.gregorian()`                 | Format using Gregorian calendar tokens. |

## Links

- https://jalalix.com
- https://github.com/jalalix/java
- [CHANGELOG.md](./CHANGELOG.md)

## License

MIT
