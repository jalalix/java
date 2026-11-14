# JalaliX

Jalali (Persian / Shamsi) calendar for Java 17+. Pure `java.time` (ZoneId), no runtime dependencies beyond the JDK.

```java
import dev.jalalix.JalaliX;
import dev.jalalix.JalaliFormatOptions;

JalaliX date = JalaliX.create("2025-02-25");

date.format("YYYY/MM/DD"); // 1403/12/07
date.format("DD MMM YYYY", JalaliFormatOptions.persianDigits()); // ۰۷ اسفند ۱۴۰۳

JalaliX.parseJalali("1403/12/07").toISODate(); // 2025-02-25
```

## Install

Maven / Gradle coordinates: `dev.jalalix:jalalix:0.1.0`

```bash
./gradlew test
./gradlew publishToMavenLocal
```

## Timezone

Internal storage is a **UTC Unix millisecond instant** plus an IANA timezone id (`ZoneId`). `setTimezone` is **instant-preserving**: `getTime()` stays the same; wall clock / Jalali day may change.

```java
JalaliX d = JalaliX.create(1740430800000L);
JalaliX tehran = d.setTimezone("Asia/Tehran");
tehran.format("YYYY/MM/DD"); // 1403/12/07
tehran.format("HH:mm:ss");   // 00:30:00

JalaliX utc = d.setTimezone("UTC");
utc.format("YYYY/MM/DD"); // 1403/12/06
```

- **Default zone:** `UTC` (portable; pass `{ timezone: "Asia/Tehran" }` or `setTimezone`)
- **Aliases:** `utc` / `gmt` -> `UTC`; `local` / `default` / `system` -> `ZoneId.systemDefault()`
- **Invalid zone** -> `IllegalArgumentException`

## Months

- `getMonth()` -> 1..12 (Farvardin = 1)
- `setMonth(m)` / `setFullYear(y, m)` -> 0..11 (Farvardin = 0), same as JS `Date`

## License

MIT
