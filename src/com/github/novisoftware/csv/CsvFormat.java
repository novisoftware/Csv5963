package com.github.novisoftware.csv;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CSV ファイルの書式情報 (ルール + 例外)。
 *
 * 各項目の本文 (body) と、この書式情報から、元のファイルのバイト列を再構築できる。
 * ルールで説明できない部分は、例外として保持する。
 */
public final class CsvFormat {
	/** 列ごとの引用符のルール。 */
	public enum QuoteRule {
		/** 区切り文字・引用符・改行を含む項目だけ引用符で囲む。 */
		WHEN_NEEDED,
		/** すべて引用符で囲む。 */
		ALWAYS,
		/** 空の項目以外を、引用符で囲む。 */
		ALWAYS_EXCEPT_EMPTY
	}

	public final CsvEncoding encoding;
	public final char separator;
	public final int recordCount;

	/** 最も多い行末のバイト列。行末が 1 つもなければ空。 */
	public final byte[] mainTerminator;

	/** 最終行に行末があるか。 */
	public final boolean lastRecordTerminated;

	/** 主な行末と異なる行末を持つレコード (レコードの番号 → 行末のバイト列)。 */
	public final Map<Integer, byte[]> terminatorExceptions;

	/** 最も多い列数 (空行を除く)。2 行目以降から求める。2 行目以降がなければ 1 行目。 */
	public final int mainColumnCount;

	/** 空行のレコードの番号。 */
	public final List<Integer> blankRecords;

	/** 主な列数と異なる列数のレコード (レコードの番号 → 列数)。空行は含まない。 */
	public final Map<Integer, Integer> columnCountExceptions;

	/**
	 * 1 行目の引用符のルール。1 行目はヘッダーであることが多く、2 行目以降と書式が異なるので、別に持つ。
	 * 1 行目のすべての列に、同じルールを使う。
	 */
	public final QuoteRule firstRecordRule;

	/** 2 行目以降の、列ごとの引用符のルール。長さは、2 行目以降の最大の列数。 */
	public final QuoteRule[] columnRules;

	/** ルールの予測と、引用符の有無が異なる項目 (key(レコード, 列))。 */
	public final Set<Long> quoteExceptions;

	/** 書式が乱れた項目の、元のバイト列 (key(レコード, 列) → raw)。 */
	public final Map<Long, byte[]> rawExceptions;

	CsvFormat(CsvEncoding encoding, char separator, int recordCount, byte[] mainTerminator,
			boolean lastRecordTerminated, Map<Integer, byte[]> terminatorExceptions, int mainColumnCount,
			List<Integer> blankRecords, Map<Integer, Integer> columnCountExceptions, QuoteRule firstRecordRule,
			QuoteRule[] columnRules, Set<Long> quoteExceptions, Map<Long, byte[]> rawExceptions) {
		this.encoding = encoding;
		this.separator = separator;
		this.recordCount = recordCount;
		this.mainTerminator = mainTerminator;
		this.lastRecordTerminated = lastRecordTerminated;
		this.terminatorExceptions = terminatorExceptions;
		this.mainColumnCount = mainColumnCount;
		this.blankRecords = blankRecords;
		this.columnCountExceptions = columnCountExceptions;
		this.firstRecordRule = firstRecordRule;
		this.columnRules = columnRules;
		this.quoteExceptions = quoteExceptions;
		this.rawExceptions = rawExceptions;
	}

	public static long key(int record, int column) {
		return ((long) record << 32) | column;
	}

	/** 本文に、引用符が必要な文字 (区切り文字・引用符・CR・LF) が含まれるか。 */
	static boolean needsQuote(CsvEncoding encoding, char separator, byte[] body) {
		int p = 0;
		while (p < body.length) {
			int v = encoding.unitAt(body, p);
			if (v == separator || v == '"' || v == '\r' || v == '\n') {
				return true;
			}
			p += Math.min(encoding.unitSize, body.length - p);
		}
		return false;
	}

	/** ルールから予測される、引用符の有無。 */
	static boolean predictQuoted(QuoteRule rule, CsvEncoding encoding, char separator, byte[] body) {
		switch (rule) {
		case ALWAYS:
			return true;
		case ALWAYS_EXCEPT_EMPTY:
			return body.length > 0;
		default:
			return needsQuote(encoding, separator, body);
		}
	}

	/** 例外の総数 (行末、引用符、書式の乱れ、空行を除く列数)。 */
	public int exceptionCount() {
		return terminatorExceptions.size() + quoteExceptions.size() + rawExceptions.size();
	}

	/**
	 * 各項目の本文から、ファイルのバイト列を再構築する。
	 *
	 * @param bodies レコードごとの、項目の本文 (書式の乱れた項目は raw)。空行は空のリスト。
	 */
	public byte[] rebuild(List<List<byte[]>> bodies) {
		byte[] sep = encoding.encodeAscii(separator);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(encoding.bom, 0, encoding.bom.length);

		int last = bodies.size() - 1;
		for (int r = 0; r <= last; r++) {
			List<byte[]> items = bodies.get(r);
			for (int c = 0; c < items.size(); c++) {
				if (c > 0) {
					out.write(sep, 0, sep.length);
				}

				byte[] raw = rawExceptions.get(key(r, c));
				if (raw == null) {
					byte[] body = items.get(c);
					QuoteRule rule = r == 0 ? firstRecordRule : columnRules[c];
					boolean quoted = predictQuoted(rule, encoding, separator, body);
					if (quoteExceptions.contains(key(r, c))) {
						quoted = !quoted;
					}
					raw = quoted ? CsvItem.quote(encoding, body) : body;
				}
				out.write(raw, 0, raw.length);
			}

			if (r == last && !lastRecordTerminated) {
				continue;
			}
			byte[] terminator = terminatorExceptions.get(r);
			if (terminator == null) {
				terminator = mainTerminator;
			}
			out.write(terminator, 0, terminator.length);
		}

		return out.toByteArray();
	}

	private static String terminatorName(byte[] terminator, CsvEncoding encoding) {
		if (terminator.length == 0) {
			return "none";
		}
		boolean crlf = terminator.length == 2 * encoding.unitSize;
		if (crlf) {
			return "CRLF";
		}
		return encoding.unitAt(terminator, 0) == '\r' ? "CR" : "LF";
	}

	/** 確認用の文字列。 */
	public String describe() {
		StringBuilder sb = new StringBuilder();
		sb.append("encoding: ").append(encoding.name).append(" (BOM ").append(encoding.bom.length).append(" bytes)\n");
		sb.append("separator: ").append(separator == '\t' ? "TAB" : String.valueOf(separator)).append('\n');
		sb.append("records: ").append(recordCount).append("  blank: ").append(blankRecords.size()).append('\n');
		sb.append("terminator: ").append(terminatorName(mainTerminator, encoding))
			.append("  last record terminated: ").append(lastRecordTerminated)
			.append("  exceptions: ").append(terminatorExceptions.size()).append('\n');
		sb.append("columns: ").append(mainColumnCount)
			.append("  records with other column count: ").append(columnCountExceptions.size()).append('\n');
		sb.append("first record quote rule: ").append(firstRecordRule).append('\n');
		sb.append("quote rule per column (records after the first):\n");
		for (int c = 0; c < columnRules.length; c++) {
			sb.append("  [").append(c + 1).append("] ").append(columnRules[c]).append('\n');
		}
		sb.append("quote exceptions: ").append(quoteExceptions.size())
			.append("  irregular items: ").append(rawExceptions.size()).append('\n');
		return sb.toString();
	}
}
