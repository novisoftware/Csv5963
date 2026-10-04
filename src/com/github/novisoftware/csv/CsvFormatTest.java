package com.github.novisoftware.csv;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Random;

import com.github.novisoftware.csv.CsvFormat.QuoteRule;

/**
 * CsvFormatAnalyzer / CsvFormat の自己検証テスト。 main で実行し、失敗があれば終了コード 1 で終わる。
 */
public class CsvFormatTest {
	private static int passed = 0;
	private static int failed = 0;

	private static void check(String name, boolean ok) {
		if (ok) {
			passed++;
		}
		else {
			failed++;
			System.out.println("FAIL: " + name);
		}
	}

	private static byte[] utf8(String s) {
		return s.getBytes(Charset.forName("UTF-8"));
	}

	private static byte[] cat(byte[]... parts) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		for (byte[] part : parts) {
			out.write(part, 0, part.length);
		}
		return out.toByteArray();
	}

	/** 解析した書式情報と本文から、元のバイト列に戻ることを確認する。 */
	private static CsvFormat analyze(String name, byte[] data, String encode, char sep) {
		CsvDocument doc = CsvRawParser.parse(data, encode, sep);
		CsvFormat format = CsvFormatAnalyzer.analyze(doc);
		byte[] rebuilt = format.rebuild(CsvFormatAnalyzer.bodiesOf(doc));
		check(name + " (rebuild)", Arrays.equals(rebuilt, data));
		return format;
	}

	private static CsvFormat analyze(String name, String text) {
		return analyze(name, utf8(text), "UTF-8", ',');
	}

	private static boolean sameRules(CsvFormat f, QuoteRule... rules) {
		return Arrays.equals(f.columnRules, rules);
	}

	private static void testRules() {
		CsvFormat f;

		f = analyze("all quoted", "\"a\",\"b\"\r\n\"c\",\"d\"\r\n");
		check("all quoted: rules", sameRules(f, QuoteRule.ALWAYS, QuoteRule.ALWAYS));
		check("all quoted: no exceptions", f.exceptionCount() == 0);
		check("all quoted: CRLF", f.mainTerminator.length == 2 && f.lastRecordTerminated);

		f = analyze("minimal quoting", "a,\"b,c\"\nd,e\n");
		check("minimal quoting: rules", sameRules(f, QuoteRule.WHEN_NEEDED, QuoteRule.WHEN_NEEDED));
		check("minimal quoting: no exceptions", f.exceptionCount() == 0);

		f = analyze("all unquoted", "a,b\nc,d\n");
		check("all unquoted: no exceptions", f.exceptionCount() == 0);

		f = analyze("always except empty", "\"a\",,\"c\"\n\"d\",,\"f\"\n\"g\",\"h\",\n");
		check("always except empty: rules", sameRules(f, QuoteRule.ALWAYS, QuoteRule.ALWAYS_EXCEPT_EMPTY, QuoteRule.ALWAYS_EXCEPT_EMPTY));
		check("always except empty: no exceptions", f.exceptionCount() == 0);

		f = analyze("unneeded quote", "a\nb\n\"c\"\nd\n");
		check("unneeded quote: 1 exception", f.quoteExceptions.size() == 1
				&& f.quoteExceptions.contains(CsvFormat.key(2, 0)) && f.columnRules[0] == QuoteRule.WHEN_NEEDED);

		f = analyze("irregular", "a\"b,c\n");
		check("irregular: raw exception", f.rawExceptions.size() == 1 && f.quoteExceptions.size() == 0);
	}

	/** 1 行目(ヘッダー)と 2 行目以降で、書式が異なるファイル。 */
	private static void testFirstRecord() {
		CsvFormat f;

		f = analyze("quoted header", "\"name\",\"age\"\r\nAlice,30\r\nBob,25\r\n");
		check("quoted header", f.firstRecordRule == QuoteRule.ALWAYS
				&& sameRules(f, QuoteRule.WHEN_NEEDED, QuoteRule.WHEN_NEEDED) && f.exceptionCount() == 0);

		f = analyze("unquoted header", "name,age\n\"Alice\",\"30\"\n\"Bob\",\"25\"\n");
		check("unquoted header", f.firstRecordRule == QuoteRule.WHEN_NEEDED
				&& sameRules(f, QuoteRule.ALWAYS, QuoteRule.ALWAYS) && f.exceptionCount() == 0);

		f = analyze("header with other column count", "id,name,extra\n1,A\n2,B\n");
		check("header with other column count", f.mainColumnCount == 2
				&& f.columnCountExceptions.size() == 1 && f.columnCountExceptions.get(0) == 3
				&& f.columnRules.length == 2);

		f = analyze("header only", "\"a\",\"b\"\n");
		check("header only", f.firstRecordRule == QuoteRule.ALWAYS && f.columnRules.length == 0
				&& f.mainColumnCount == 2 && f.exceptionCount() == 0);

		f = analyze("header with an unneeded quote", "a,\"b\"\nc,d\n");
		check("header with an unneeded quote", f.exceptionCount() == 1 && f.quoteExceptions.contains(CsvFormat.key(0, 1)));
	}

	private static void testLayout() {
		CsvFormat f;

		f = analyze("mixed terminators", "a\r\nb\r\nc\nd");
		check("mixed terminators", f.mainTerminator.length == 2 && f.terminatorExceptions.size() == 1
				&& f.terminatorExceptions.containsKey(2) && !f.lastRecordTerminated);

		f = analyze("no final newline", "a,b\nc,d");
		check("no final newline", !f.lastRecordTerminated && f.terminatorExceptions.size() == 0);

		f = analyze("blank lines", "a,b\n\nc,d\n");
		check("blank lines", f.blankRecords.size() == 1 && f.blankRecords.get(0) == 1 && f.mainColumnCount == 2
				&& f.columnCountExceptions.size() == 0);

		f = analyze("column counts", "a,b\nc\nd,e\n");
		check("column counts", f.mainColumnCount == 2 && f.columnCountExceptions.size() == 1
				&& f.columnCountExceptions.get(1) == 1);

		f = analyze("empty", "");
		check("empty", f.recordCount == 0 && f.lastRecordTerminated && f.columnRules.length == 0);

		f = analyze("only CR", "a\rb\r");
		check("only CR", f.mainTerminator.length == 1 && f.mainTerminator[0] == '\r');
	}

	private static void testSeparator() {
		check("separator comma", CsvFormatAnalyzer.detectSeparator(utf8("a,b,c\n1,2,3\n"), "UTF-8") == ',');
		check("separator tab", CsvFormatAnalyzer.detectSeparator(utf8("a\tb\tc\n1\t2\t3\n"), "UTF-8") == '\t');
		check("separator semicolon", CsvFormatAnalyzer.detectSeparator(utf8("a;b;c\n1;2;3\n"), "UTF-8") == ';');
		check("separator pipe", CsvFormatAnalyzer.detectSeparator(utf8("a|b\n1|2\n"), "UTF-8") == '|');
		check("separator single column", CsvFormatAnalyzer.detectSeparator(utf8("abc\ndef\n"), "UTF-8") == ',');
		check("separator comma in quotes", CsvFormatAnalyzer.detectSeparator(utf8("\"a,b\";c\n\"d,e\";f\n"), "UTF-8") == ';');
		check("separator utf-16", CsvFormatAnalyzer.detectSeparator(
				cat(new byte[] { (byte) 0xFF, (byte) 0xFE }, "a\tb\n1\t2\n".getBytes(Charset.forName("UTF-16LE"))), "UTF-16") == '\t');
	}

	private static void testEncodings() {
		CsvFormat f;

		f = analyze("utf-16 LE bom", cat(new byte[] { (byte) 0xFF, (byte) 0xFE },
				"\"東京都\",\"男\"\r\n\"大阪府\",\"女\"\r\n".getBytes(Charset.forName("UTF-16LE"))), "UTF-16", ',');
		check("utf-16 LE bom", f.encoding.bom.length == 2 && f.mainTerminator.length == 4
				&& f.columnRules[0] == QuoteRule.ALWAYS && f.exceptionCount() == 0);

		f = analyze("ms932", "\"表\",\"ソ\"\r\n\"能\",\"ゾ\"\r\n".getBytes(Charset.forName("MS932")), "MS932", ',');
		check("ms932", f.columnRules[0] == QuoteRule.ALWAYS && f.exceptionCount() == 0);

		f = analyze("utf-8 bom", cat(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF }, utf8("a;b\n")), "UTF-8", ';');
		check("utf-8 bom", f.encoding.bom.length == 3 && f.separator == ';');
	}

	private static byte[] unit(int v, boolean big) {
		return big ? new byte[] { (byte) (v >> 8), (byte) v } : new byte[] { (byte) v, (byte) (v >> 8) };
	}

	private static void testRandom() {
		Random random = new Random(20260405L);
		int[] alphabet = { ',', ',', '"', '"', '\r', '\n', '\n', 'a', 'b', ' ', '\t', ';', 0x3042, 0x00, 0xFF, 0x83, 0x5C };

		String[] encodes = { "UTF-8", "MS932", "UTF-16", "UTF-16LE", "UTF-16BE" };
		for (String encode : encodes) {
			boolean utf16 = encode.startsWith("UTF-16");
			boolean big = !encode.equals("UTF-16LE");
			for (int iteration = 0; iteration < 3000; iteration++) {
				int length = random.nextInt(60);
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				if (utf16 && random.nextBoolean()) {
					byte[] bom = unit(0xFEFF, big);
					out.write(bom, 0, 2);
				}
				for (int i = 0; i < length; i++) {
					int v = alphabet[random.nextInt(alphabet.length)];
					if (utf16) {
						byte[] u = unit(v, big);
						out.write(u, 0, 2);
					}
					else {
						out.write(v);
					}
				}
				if (utf16 && random.nextInt(8) == 0) {
					out.write(0x41);
				}
				byte[] data = out.toByteArray();
				char sep = CsvFormatAnalyzer.detectSeparator(data, encode);
				analyze("random " + encode + " #" + iteration, data, encode, sep);
			}
		}
	}

	private static void testSample() throws Exception {
		File file = new File("src/com/github/novisoftware/csvSampleData/SAMPLE1.csv");
		if (!file.exists()) {
			System.out.println("SKIP: " + file + " not found");
			return;
		}
		byte[] data = Files.readAllBytes(file.toPath());
		char sep = CsvFormatAnalyzer.detectSeparator(data, "UTF-8");
		CsvFormat f = analyze("sample1", data, "UTF-8", sep);
		check("sample1", sep == ',' && f.exceptionCount() == 0
				&& f.columnRules.length == 3 && f.firstRecordRule == QuoteRule.ALWAYS && f.columnRules[0] == QuoteRule.ALWAYS
				&& f.columnCountExceptions.size() == 2);
	}

	public static void main(String[] args) throws Exception {
		testRules();
		testFirstRecord();
		testLayout();
		testSeparator();
		testEncodings();
		testRandom();
		testSample();

		System.out.println("passed: " + passed + ", failed: " + failed);
		if (failed > 0) {
			System.exit(1);
		}
	}
}
