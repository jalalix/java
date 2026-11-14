package dev.jalalix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

// Loads {@code testdata/golden.json} and asserts parity with the TypeScript contract.
class GoldenTest {
	private static JsonNode golden;

	@BeforeAll
	static void load() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		try (InputStream in = GoldenTest.class.getClassLoader().getResourceAsStream("testdata/golden.json")) {
			if (in == null) {
				// Fallback: file next to project root when running from IDE
				golden = mapper.readTree(new java.io.File("testdata/golden.json"));
			} else {
				golden = mapper.readTree(in);
			}
		}
	}

	private static JalaliX createFrom(JsonNode spec) {
		if (spec.has("unixMs")) {
			JalaliX d = JalaliX.create(spec.get("unixMs").asLong());

			if (spec.has("timezone")) {
				d = d.setTimezone(spec.get("timezone").asText());
			}

			return d;
		}

		return JalaliX.create(spec.get("isoDate").asText());
	}

	@Test
	void calendar() {
		for (JsonNode c : golden.get("calendar")) {
			JalaliX d = createFrom(c.get("create"));
			JsonNode expect = c.get("expect");
			assertEquals(expect.get("formatYYYYMMDD").asText(), d.format("YYYY/MM/DD"), c.get("id").asText());
			assertEquals(expect.get("toISODateGregorian").asText(), d.toISODate(), c.get("id").asText() + " iso g");
			assertEquals(expect.get("toISODateJalali").asText(), d.toISODate("jalali"), c.get("id").asText() + " iso j");
		}
	}

	@Test
	void parse() {
		for (JsonNode c : golden.get("parse")) {
			JalaliX d = JalaliX.parseJalali(c.get("input").asText());
			JsonNode expect = c.get("expect");
			assertEquals(expect.get("toISODateGregorian").asText(), d.toISODate(), c.get("id").asText());

			if (expect.has("formatYYYYMMDD")) {
				assertEquals(expect.get("formatYYYYMMDD").asText(), d.format("YYYY/MM/DD"), c.get("id").asText() + " format");
			}
		}
	}

	@Test
	void format() {
		for (JsonNode c : golden.get("format")) {
			if (c.has("toPersian")) {
				assertEquals(c.get("toPersian").get("expect").asText(), JalaliX.toPersian(c.get("toPersian").get("input").asText()));
				assertEquals(c.get("fromPersian").get("expect").asText(), JalaliX.fromPersian(c.get("fromPersian").get("input").asText()));
				continue;
			}

			JalaliX d = createFrom(c.get("create"));
			JalaliFormatOptions opts = new JalaliFormatOptions();
			JsonNode options = c.get("options");

			if (options != null) {
				if (options.has("persianDigits")) opts.persianDigits = options.get("persianDigits").asBoolean();
				if (options.has("calendar")) opts.calendar = options.get("calendar").asText();
			}

			assertEquals(c.get("expect").asText(), d.format(c.get("pattern").asText(), opts), c.get("id").asText());
		}
	}

	@Test
	void timezone() {
		for (JsonNode c : golden.get("timezone")) {
			if (c.has("setTimezoneAlias")) {
				JalaliX d = JalaliX.create(c.get("unixMs").asLong()).setTimezone(c.get("setTimezoneAlias").asText());
				assertEquals(c.get("expectTimezone").asText(), d.getTimezone(), c.get("id").asText());
				continue;
			}

			JalaliX d = JalaliX.create(c.get("unixMs").asLong()).setTimezone(c.get("withTimezone").asText());
			JsonNode expect = c.get("expect");

			assertEquals(expect.get("jalaliSlash").asText(), d.format("YYYY/MM/DD"), c.get("id").asText() + " jalali");

			if (expect.has("jalaliIso")) {
				assertEquals(expect.get("jalaliIso").asText(), d.toISODate("jalali"), c.get("id").asText() + " jalaliIso");
			}

			if (expect.has("wallGregorian")) {
				assertEquals(expect.get("wallGregorian").asText(), d.toISODate(), c.get("id").asText() + " wallG");
			}

			assertEquals(expect.get("hours").asInt(), d.getHours(), c.get("id").asText() + " h");
			assertEquals(expect.get("minutes").asInt(), d.getMinutes(), c.get("id").asText() + " m");

			if (expect.has("getTime")) {
				assertEquals(expect.get("getTime").asLong(), d.getTime(), c.get("id").asText() + " instant");
			}
		}
	}

	@Test
	void mutations() {
		for (JsonNode c : golden.get("mutations")) {
			JalaliX d = createFrom(c.get("create"));

			if (c.has("setFullYear")) {
				JsonNode sfy = c.get("setFullYear");
				d.setFullYear(sfy.get("year").asInt(), sfy.get("month0").asInt(), sfy.get("day").asInt());
				assertEquals(c.get("expect").get("getMonth").asInt(), d.getMonth(), c.get("id").asText() + " getMonth");
				assertEquals(c.get("expect").get("formatYYYYMMDD").asText(), d.format("YYYY/MM/DD"), c.get("id").asText() + " format");
				continue;
			}

			for (JsonNode opNode : c.get("ops")) {
				String op = opNode.asText();

				if (op.startsWith("addMonths:")) {
					d = d.addMonths(Integer.parseInt(op.split(":")[1]));
				} else if (op.equals("getStartOfMonth")) {
					d = d.getStartOfMonth();
				}
			}

			assertEquals(c.get("expect").get("formatYYYYMMDD").asText(), d.format("YYYY/MM/DD"), c.get("id").asText());
		}
	}

	@Test
	void smokeCompareClone() {
		JalaliX a = JalaliX.create("2025-02-25");
		JalaliX b = JalaliX.create("2025-02-26");
		assertTrue(JalaliX.compare(a, b));
		JalaliX c = a.clone();
		assertEquals(a.getTime(), c.getTime());
		assertEquals(a.getTimezone(), c.getTimezone());
	}
}
