package com.github.novisoftware.csv;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * CsvRawParser の自己検証テスト。 main で実行し、失敗があれば終了コード 1 で終わる。
 */
public class CsvRawParserTest {
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

	private static byte[] bytes(String s, String charset) {
		return s.getBytes(Charset.forName(charset));
	}

	private static byte[] cat(byte[]... parts) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		for (byte[] part : parts) {
			out.write(part, 0, part.length);
		}
		return out.toByteArray();
	}

	private static byte[] hex(int... values) {
		byte[] b = new byte[values.length];
		for (int i = 0; i < values.length; i++) {
			b[i] = (byte) values[i];
		}
		return b;
	}

	/** パースして、元のバイト列に戻ること、通常の項目の再生成が一致することを確認する。 */
	private static CsvDocument roundTrip(String name, byte[] data, String encode, char sep) {
		CsvDocument doc;
		try {
			doc = CsvRawParser.parse(data, encode, sep);
		} catch (RuntimeException e) {
			check(name + " (exception: " + e + ")", false);
			return null;
		}

		check(name + " (round trip)", Arrays.equals(doc.toBytes(), data));
		boolean itemsOk = true;
		for (CsvRecord record : doc.records) {
			for (CsvItem item : record.items) {
				if (item.irregular) {
					itemsOk &= item.body == item.raw && !item.quoted;
				}
				else {
					itemsOk &= Arrays.equals(item.regenerateRaw(), item.raw);
				}
			}
		}
		check(name + " (items)", itemsOk);
		return doc;
	}

	private static CsvDocument utf8(String name, String text) {
		return roundTrip(name, bytes(text, "UTF-8"), "UTF-8", ',');
	}

	private static String texts(CsvDocument doc) {
		StringBuilder sb = new StringBuilder();
		for (CsvRecord record : doc.records) {
			sb.append('[');
			boolean first = true;
			for (CsvItem item : record.items) {
				if (!first) {
					sb.append('|');
				}
				first = false;
				String t = item.getText();
				sb.append(t == null ? "<null>" : t);
			}
			sb.append(']');
		}
		return sb.toString();
	}

	private static String terminators(CsvDocument doc) {
		StringBuilder sb = new StringBuilder();
		for (CsvRecord record : doc.records) {
			if (record.terminator.length == 0) {
				sb.append("E");
			}
			else if (record.terminator.length == 2) {
				sb.append("W");
			}
			else {
				sb.append(record.terminator[0] == '\n' ? "N" : "R");
			}
		}
		return sb.toString();
	}

	private static void testStructure() {
		CsvDocument d;

		d = utf8("empty", "");
		check("empty: no records", d.records.size() == 0);

		d = utf8("no terminator", "a,b");
		check("no terminator: 1 record", d.records.size() == 1 && terminators(d).equals("E"));
		check("no terminator: items", texts(d).equals("[a|b]"));

		d = utf8("LF", "a,b\n");
		check("LF: 1 record, no extra record", d.records.size() == 1 && terminators(d).equals("N"));

		d = utf8("CRLF", "a,b\r\nc,d\r\n");
		check("CRLF", terminators(d).equals("WW") && texts(d).equals("[a|b][c|d]"));

		d = utf8("CR", "a\rb\r");
		check("CR", terminators(d).equals("RR") && texts(d).equals("[a][b]"));

		d = utf8("mixed terminators", "a\r\nb\nc\rd");
		check("mixed terminators", terminators(d).equals("WNRE") && texts(d).equals("[a][b][c][d]"));

		d = utf8("blank lines", "a\n\nb\n");
		check("blank lines", d.records.size() == 3 && d.records.get(1).items.size() == 0 && terminators(d).equals("NNN"));

		d = utf8("only LF", "\n");
		check("only LF", d.records.size() == 1 && d.records.get(0).items.size() == 0);

		d = utf8("quotes", "\"a\",b,\"c,d\",\"e\"\"f\"\n");
		check("quotes: texts", texts(d).equals("[a|b|c,d|e\"f]"));
		CsvRecord r = d.records.get(0);
		check("quotes: flags", r.items.get(0).quoted && !r.items.get(1).quoted && r.items.get(2).quoted && r.items.get(3).quoted);
		check("quotes: raw", new String(r.items.get(3).raw, Charset.forName("UTF-8")).equals("\"e\"\"f\""));

		d = utf8("empty items", ",,\n\"\",\"\"\n");
		check("empty items", texts(d).equals("[||][|]") && d.records.get(1).items.get(0).quoted);

		d = utf8("newline in quote", "\"a\r\nb\",c\n");
		check("newline in quote", d.records.size() == 1 && texts(d).equals("[a\r\nb|c]"));

		d = utf8("trailing separator LF", "a,\n");
		check("trailing separator LF", texts(d).equals("[a|]"));
		d = utf8("trailing separator EOF", "a,");
		check("trailing separator EOF", texts(d).equals("[a|]"));

		d = utf8("quote in unquoted", "a\"b,c\n");
		check("quote in unquoted", d.records.get(0).items.get(0).irregular && !d.records.get(0).items.get(1).irregular);

		d = utf8("char after quote", "\"abc\"x,d\n");
		check("char after quote", d.records.get(0).items.get(0).irregular
				&& new String(d.records.get(0).items.get(0).raw, Charset.forName("UTF-8")).equals("\"abc\"x")
				&& !d.records.get(0).items.get(1).irregular);

		d = utf8("unclosed quote", "a,\"abc\ndef");
		check("unclosed quote", d.records.size() == 1 && d.records.get(0).items.size() == 2
				&& d.records.get(0).items.get(1).irregular && terminators(d).equals("E"));

		d = utf8("spaces", " a , \"b\" ,c\n");
		check("spaces", d.records.get(0).items.get(0).getText().equals(" a ")
				&& d.records.get(0).items.get(1).irregular
				&& d.records.get(0).items.get(2).getText().equals("c"));

		d = roundTrip("tab", bytes("a\t\"b\tc\"\td\n", "UTF-8"), "UTF-8", '\t');
		check("tab", texts(d).equals("[a|b\tc|d]"));
		d = roundTrip("semicolon", bytes("a;\"b;c\";d\n", "UTF-8"), "UTF-8", ';');
		check("semicolon", texts(d).equals("[a|b;c|d]"));
	}

	private static void testEncodings() {
		CsvDocument d;

		d = roundTrip("utf-8 bom", cat(hex(0xEF, 0xBB, 0xBF), bytes("\"東京都\",男\n", "UTF-8")), "UTF-8", ',');
		check("utf-8 bom", d.encoding.bom.length == 3 && texts(d).equals("[東京都|男]"));
		d = roundTrip("utf-8 no bom", bytes("東京都,女\n", "UTF-8"), "UTF-8", ',');
		check("utf-8 no bom", d.encoding.bom.length == 0 && texts(d).equals("[東京都|女]"));

		// 2 バイト目が 0x5C の文字を含む。
		byte[] sjis = cat(bytes("ソ", "MS932"), bytes(",", "MS932"), bytes("\"表\"", "MS932"), bytes(",\"a\"\"b\"\r\n", "MS932"));
		d = roundTrip("ms932", sjis, "MS932", ',');
		check("ms932", texts(d).equals("[ソ|表|a\"b]"));

		String utf16Text = "\"東京都\",男\r\n\"a\"\"b\",c\n";
		d = roundTrip("utf-16 bom BE", cat(hex(0xFE, 0xFF), bytes(utf16Text, "UTF-16BE")), "UTF-16", ',');
		check("utf-16 bom BE", d.encoding.bom.length == 2 && d.encoding.bigEndian && texts(d).equals("[東京都|男][a\"b|c]"));
		d = roundTrip("utf-16 bom LE", cat(hex(0xFF, 0xFE), bytes(utf16Text, "UTF-16LE")), "UTF-16", ',');
		check("utf-16 bom LE", d.encoding.bom.length == 2 && !d.encoding.bigEndian && texts(d).equals("[東京都|男][a\"b|c]"));
		d = roundTrip("utf-16 no bom", bytes(utf16Text, "UTF-16BE"), "UTF-16", ',');
		check("utf-16 no bom", d.encoding.bom.length == 0 && texts(d).equals("[東京都|男][a\"b|c]"));
		d = roundTrip("utf-16LE no bom", bytes(utf16Text, "UTF-16LE"), "UTF-16LE", ',');
		check("utf-16LE no bom", texts(d).equals("[東京都|男][a\"b|c]"));
		d = roundTrip("utf-16BE bom", cat(hex(0xFE, 0xFF), bytes(utf16Text, "UTF-16BE")), "UTF-16BE", ',');
		check("utf-16BE bom", d.encoding.bom.length == 2 && texts(d).equals("[東京都|男][a\"b|c]"));

		// 2 バイト単位で判定するため、バイトの並びが偶然 ',' などに見えても区切らない。
		byte[] tricky = cat(bytes("a", "UTF-16LE"), hex(0x2C, 0x30), bytes(",b", "UTF-16LE"));
		d = roundTrip("utf-16 tricky", tricky, "UTF-16LE", ',');
		check("utf-16 tricky", d.records.get(0).items.size() == 2);

		d = roundTrip("utf-16 odd byte", cat(bytes("a,b", "UTF-16LE"), hex(0x41)), "UTF-16LE", ',');
		check("utf-16 odd byte", d.records.get(0).items.size() == 2 && d.records.get(0).items.get(1).getText() == null);
	}

	private static void testGetText() {
		CsvDocument d = roundTrip("getText utf-8", cat(bytes("東京都,", "UTF-8"), hex(0xC3), bytes(",男\n", "UTF-8")), "UTF-8", ',');
		check("getText utf-8", d.records.get(0).items.get(0).getText().equals("東京都")
				&& d.records.get(0).items.get(1).getText() == null
				&& d.records.get(0).items.get(2).getText().equals("男"));

		d = roundTrip("getText ms932", cat(bytes("東京都,女,", "MS932"), hex(0x83)), "MS932", ',');
		check("getText ms932", d.records.get(0).items.get(0).getText().equals("東京都")
				&& d.records.get(0).items.get(1).getText().equals("女")
				&& d.records.get(0).items.get(2).getText() == null);
	}

	private static byte[] unit(int v, boolean big) {
		return big ? new byte[] { (byte) (v >> 8), (byte) v } : new byte[] { (byte) v, (byte) (v >> 8) };
	}

	private static void testRandom() {
		Random random = new Random(20260404L);
		int[] alphabet = { ',', '"', '\r', '\n', 'a', 'b', ' ', '\t', 0x3042, 0x00, 0xFF, 0x83, 0x5C, 0xE3 };

		String[] encodes = { "UTF-8", "MS932", "UTF-16", "UTF-16LE", "UTF-16BE" };
		for (String encode : encodes) {
			boolean utf16 = encode.startsWith("UTF-16");
			boolean big = !encode.equals("UTF-16LE");
			for (int iteration = 0; iteration < 3000; iteration++) {
				int length = random.nextInt(40);
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
				roundTrip("random " + encode + " #" + iteration, out.toByteArray(), encode, ',');
			}
		}
	}

	/** 既存パーサー (Csv) の結果と、正常な書式のサンプルで一致することを確認する。 */
	private static void testAgainstExistingParser() throws Exception {
		File file = new File("src/com/github/novisoftware/csvSampleData/SAMPLE1.csv");
		if (!file.exists()) {
			System.out.println("SKIP: " + file + " not found");
			return;
		}

		byte[] data = Files.readAllBytes(file.toPath());
		CsvDocument doc = roundTrip("sample1", data, "UTF-8", ',');

		CsvToModel old = new CsvToModel("UTF-8", Csv.AnalyzePreference.STRICT_ANALYZE);
		old.read(file.getAbsolutePath());
		ArrayList<ArrayList<String>> oldData = old.getData();

		boolean same = oldData.size() == doc.records.size();
		for (int r = 0; same && r < oldData.size(); r++) {
			List<CsvItem> items = doc.records.get(r).items;
			same = oldData.get(r).size() == items.size();
			for (int c = 0; same && c < items.size(); c++) {
				// 既存のパーサーは、引用符内の改行を LF に置き換える。
				String text = items.get(c).getText().replace("\r\n", "\n").replace("\r", "\n");
				same = text.equals(oldData.get(r).get(c));
			}
		}
		check("sample1: same as existing parser", same);
	}

	public static void main(String[] args) throws Exception {
		testStructure();
		testEncodings();
		testGetText();
		testRandom();
		testAgainstExistingParser();

		System.out.println("passed: " + passed + ", failed: " + failed);
		if (failed > 0) {
			System.exit(1);
		}
	}
}
