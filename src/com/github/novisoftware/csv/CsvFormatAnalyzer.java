package com.github.novisoftware.csv;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import com.github.novisoftware.csv.CsvFormat.QuoteRule;

/**
 * CsvDocument から書式情報 (CsvFormat) を抽出する。
 */
public final class CsvFormatAnalyzer {
	private static final char[] SEPARATOR_CANDIDATES = { ',', '\t', ';', '|' };

	private CsvFormatAnalyzer() {
	}

	/**
	 * 区切り文字を判定する。列数がそろっているレコードが最も多くなる候補を選ぶ。
	 * 判定できない場合は ',' を返す。
	 */
	public static char detectSeparator(byte[] data, String encode) {
		CsvEncoding encoding = CsvEncoding.of(encode, data);
		char best = ',';
		int bestScore = 0;
		for (char candidate : SEPARATOR_CANDIDATES) {
			CsvDocument doc = CsvRawParser.parse(data, encoding, candidate);
			Map<Integer, Integer> counts = new TreeMap<Integer, Integer>();
			for (CsvRecord record : doc.records) {
				if (record.items.size() > 0) {
					Integer n = counts.get(record.items.size());
					counts.put(record.items.size(), n == null ? 1 : n + 1);
				}
			}
			int modeColumns = mode(counts);
			int score = modeColumns >= 2 ? counts.get(modeColumns) : 0;
			if (score > bestScore) {
				best = candidate;
				bestScore = score;
			}
		}
		return best;
	}

	/** 最も多い値を返す。同数なら大きいほう。空なら 0。 */
	private static int mode(Map<Integer, Integer> counts) {
		int mode = 0;
		int modeCount = 0;
		for (Map.Entry<Integer, Integer> e : counts.entrySet()) {
			if (e.getValue() >= modeCount) {
				mode = e.getKey();
				modeCount = e.getValue();
			}
		}
		return mode;
	}

	private static String hex(byte[] b) {
		return Arrays.toString(b);
	}

	public static CsvFormat analyze(CsvDocument doc) {
		CsvEncoding encoding = doc.encoding;
		char separator = doc.separator;
		List<CsvRecord> records = doc.records;
		int n = records.size();

		// 行末
		Map<String, Integer> terminatorCounts = new LinkedHashMap<String, Integer>();
		Map<String, byte[]> terminatorSamples = new HashMap<String, byte[]>();
		for (CsvRecord record : records) {
			if (record.terminator.length > 0) {
				String k = hex(record.terminator);
				Integer c = terminatorCounts.get(k);
				terminatorCounts.put(k, c == null ? 1 : c + 1);
				terminatorSamples.put(k, record.terminator);
			}
		}
		byte[] mainTerminator = new byte[0];
		int best = 0;
		for (Map.Entry<String, Integer> e : terminatorCounts.entrySet()) {
			if (e.getValue() > best) {
				best = e.getValue();
				mainTerminator = terminatorSamples.get(e.getKey());
			}
		}
		boolean lastTerminated = n == 0 || records.get(n - 1).terminator.length > 0;
		Map<Integer, byte[]> terminatorExceptions = new TreeMap<Integer, byte[]>();
		for (int r = 0; r < n; r++) {
			byte[] t = records.get(r).terminator;
			if (t.length > 0 && !Arrays.equals(t, mainTerminator)) {
				terminatorExceptions.put(r, t);
			}
		}

		// 列数と空行。1 行目はヘッダーのことが多く、列数や書式が 2 行目以降と異なるので、主な列数は 2 行目以降から決める。
		Map<Integer, Integer> columnCounts = new TreeMap<Integer, Integer>();
		List<Integer> blankRecords = new ArrayList<Integer>();
		int dataMaxColumns = 0;
		for (int r = 0; r < n; r++) {
			int size = records.get(r).items.size();
			if (size == 0) {
				blankRecords.add(r);
			}
			else if (r >= 1) {
				Integer c = columnCounts.get(size);
				columnCounts.put(size, c == null ? 1 : c + 1);
			}
			if (r >= 1) {
				dataMaxColumns = Math.max(dataMaxColumns, size);
			}
		}
		int mainColumns = columnCounts.isEmpty() ? (n > 0 ? records.get(0).items.size() : 0) : mode(columnCounts);
		Map<Integer, Integer> columnCountExceptions = new TreeMap<Integer, Integer>();
		for (int r = 0; r < n; r++) {
			int size = records.get(r).items.size();
			if (size > 0 && size != mainColumns) {
				columnCountExceptions.put(r, size);
			}
		}

		// 引用符のルール。書式の乱れた項目は、ルールの選択に使わない。1 行目は、列に関わらず 1 つのルールを、2 行目以降は列ごとのルールを選ぶ。
		QuoteRule[] rules = QuoteRule.values();
		int[] firstMismatches = new int[rules.length];
		int[][] mismatches = new int[dataMaxColumns][rules.length];
		for (int r = 0; r < n; r++) {
			List<CsvItem> items = records.get(r).items;
			for (int c = 0; c < items.size(); c++) {
				CsvItem item = items.get(c);
				if (item.irregular) {
					continue;
				}
				for (int k = 0; k < rules.length; k++) {
					if (CsvFormat.predictQuoted(rules[k], encoding, separator, item.body) != item.quoted) {
						if (r == 0) {
							firstMismatches[k]++;
						}
						else {
							mismatches[c][k]++;
						}
					}
				}
			}
		}
		QuoteRule firstRecordRule = rules[bestRule(firstMismatches)];
		QuoteRule[] columnRules = new QuoteRule[dataMaxColumns];
		for (int c = 0; c < dataMaxColumns; c++) {
			columnRules[c] = rules[bestRule(mismatches[c])];
		}

		TreeSet<Long> quoteExceptions = new TreeSet<Long>();
		Map<Long, byte[]> rawExceptions = new TreeMap<Long, byte[]>();
		for (int r = 0; r < n; r++) {
			List<CsvItem> items = records.get(r).items;
			for (int c = 0; c < items.size(); c++) {
				CsvItem item = items.get(c);
				QuoteRule rule = r == 0 ? firstRecordRule : columnRules[c];
				if (item.irregular) {
					rawExceptions.put(CsvFormat.key(r, c), item.raw);
				}
				else if (CsvFormat.predictQuoted(rule, encoding, separator, item.body) != item.quoted) {
					quoteExceptions.add(CsvFormat.key(r, c));
				}
			}
		}

		return new CsvFormat(encoding, separator, n, mainTerminator, lastTerminated, terminatorExceptions,
				mainColumns, blankRecords, columnCountExceptions, firstRecordRule, columnRules, quoteExceptions,
				rawExceptions);
	}

	/** 予測が外れる項目が最も少ないルールの番号。同数なら、先に定義されたルール。 */
	private static int bestRule(int[] mismatches) {
		int best = 0;
		for (int k = 1; k < mismatches.length; k++) {
			if (mismatches[k] < mismatches[best]) {
				best = k;
			}
		}
		return best;
	}

	/** 再構築に渡す、項目の本文の一覧を作る (書式の乱れた項目は raw)。 */
	public static List<List<byte[]>> bodiesOf(CsvDocument doc) {
		List<List<byte[]>> bodies = new ArrayList<List<byte[]>>();
		for (CsvRecord record : doc.records) {
			List<byte[]> row = new ArrayList<byte[]>();
			for (CsvItem item : record.items) {
				row.add(item.body);
			}
			bodies.add(row);
		}
		return bodies;
	}
}
