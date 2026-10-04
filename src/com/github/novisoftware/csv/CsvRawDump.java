package com.github.novisoftware.csv;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * CSV ファイルの構造 (行末、列数、引用符の割合など) を表示する確認用コマンド。
 *
 * 使い方: CsvRawDump &lt;ファイル&gt; [文字コード(既定 MS932)] [区切り文字(省略すると自動判定。タブは \t)]
 */
public class CsvRawDump {
	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			System.err.println("usage: CsvRawDump <file> [encode] [separator]");
			System.exit(1);
		}

		String encode = args.length >= 2 ? args[1] : Csv.encodeList[0];
		byte[] data = Files.readAllBytes(new File(args[0]).toPath());
		char separator;
		if (args.length >= 3 && args[2].length() > 0) {
			separator = args[2].equals("\\t") ? '\t' : args[2].charAt(0);
		}
		else {
			separator = CsvFormatAnalyzer.detectSeparator(data, encode);
		}
		CsvDocument doc = CsvRawParser.parse(data, encode, separator);

		int crlf = 0;
		int lf = 0;
		int cr = 0;
		int none = 0;
		int blank = 0;
		int irregular = 0;
		int maxColumns = 0;
		TreeMap<Integer, Integer> columnCounts = new TreeMap<Integer, Integer>();
		for (CsvRecord record : doc.records) {
			int tl = record.terminator.length;
			if (tl == 0) {
				none++;
			}
			else if (tl == 2 * doc.encoding.unitSize) {
				crlf++;
			}
			else if (doc.encoding.unitAt(record.terminator, 0) == '\n') {
				lf++;
			}
			else {
				cr++;
			}

			if (record.items.size() == 0) {
				blank++;
			}
			else {
				Integer count = columnCounts.get(record.items.size());
				columnCounts.put(record.items.size(), count == null ? 1 : count + 1);
			}
			maxColumns = Math.max(maxColumns, record.items.size());
			for (CsvItem item : record.items) {
				if (item.irregular) {
					irregular++;
				}
			}
		}

		int[] quoted = new int[maxColumns];
		int[] total = new int[maxColumns];
		for (CsvRecord record : doc.records) {
			for (int c = 0; c < record.items.size(); c++) {
				total[c]++;
				if (record.items.get(c).quoted) {
					quoted[c]++;
				}
			}
		}

		System.out.println("file: " + args[0]);
		System.out.println("encoding: " + doc.encoding.name + " (charset " + doc.encoding.charset.name() + ", BOM " + doc.encoding.bom.length + " bytes)");
		System.out.println("bytes: " + data.length + "  round trip: " + (java.util.Arrays.equals(doc.toBytes(), data) ? "OK" : "NG"));
		System.out.println("records: " + doc.records.size() + "  (blank: " + blank + ")");
		System.out.println("terminators: CRLF=" + crlf + " LF=" + lf + " CR=" + cr + " none=" + none);
		System.out.println("irregular items: " + irregular);

		List<String> counts = new ArrayList<String>();
		for (java.util.Map.Entry<Integer, Integer> e : columnCounts.entrySet()) {
			counts.add(e.getKey() + " columns: " + e.getValue() + " records");
		}
		System.out.println("column counts: " + counts);

		System.out.println("quoted ratio per column:");
		for (int c = 0; c < maxColumns; c++) {
			System.out.println(String.format("  [%d] %d / %d (%.1f%%)", c + 1, quoted[c], total[c], 100.0 * quoted[c] / total[c]));
		}

		System.out.println();
		System.out.println("format:");
		System.out.print(CsvFormatAnalyzer.analyze(doc).describe());
	}
}
