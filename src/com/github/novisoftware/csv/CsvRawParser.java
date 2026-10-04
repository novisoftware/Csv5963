package com.github.novisoftware.csv;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * バイト列のまま CSV を分解するパーサー。
 *
 * 文字列には変換せず、区切り文字・引用符・CR・LF だけをコードユニット単位で判定する。
 * 例外は投げず、書式の乱れた項目は irregular として元のバイト列のまま保持する。
 */
public final class CsvRawParser {
	private static final int QUOTE = '"';

	private final byte[] data;
	private final CsvEncoding enc;
	private final int unit;
	private final int sep;

	private CsvRawParser(byte[] data, CsvEncoding enc, char separator) {
		this.data = data;
		this.enc = enc;
		this.unit = enc.unitSize;
		this.sep = separator;
	}

	public static CsvDocument parse(byte[] data, String encode, char separator) {
		return parse(data, CsvEncoding.of(encode, data), separator);
	}

	public static CsvDocument parse(byte[] data, CsvEncoding encoding, char separator) {
		return new CsvRawParser(data, encoding, separator).parseAll();
	}

	private int unitAt(int pos) {
		return enc.unitAt(data, pos);
	}

	private int step(int pos) {
		return Math.min(unit, data.length - pos);
	}

	/** pos が行末なら、そのバイト数。行末でなければ 0。 */
	private int terminatorLength(int pos) {
		int v = unitAt(pos);
		if (v == '\n') {
			return unit;
		}
		if (v == '\r') {
			return unitAt(pos + unit) == '\n' ? unit * 2 : unit;
		}
		return 0;
	}

	private CsvItem item(byte[] raw, byte[] body, boolean quoted, boolean irregular) {
		return new CsvItem(enc, raw, body, quoted, irregular);
	}

	/** pos から、区切り文字・行末・ファイル終端の手前までを 1 個の不正な項目にする。 */
	private int scanToFieldEnd(int pos) {
		int q = pos;
		while (q < data.length && unitAt(q) != sep && terminatorLength(q) == 0) {
			q += step(q);
		}
		return q;
	}

	private CsvDocument parseAll() {
		List<CsvRecord> records = new ArrayList<CsvRecord>();
		int n = data.length;
		int p = enc.bom.length;
		byte[] none = new byte[0];

		while (p < n) {
			List<CsvItem> items = new ArrayList<CsvItem>();

			int tl = terminatorLength(p);
			if (tl > 0) {
				records.add(new CsvRecord(items, Arrays.copyOfRange(data, p, p + tl)));
				p += tl;
				continue;
			}

			byte[] terminator = none;
			while (true) {
				int fieldStart = p;
				int q;
				CsvItem item;

				if (unitAt(p) == QUOTE) {
					ByteArrayOutputStream body = new ByteArrayOutputStream();
					q = p + step(p);
					boolean closed = false;
					while (q < n) {
						if (unitAt(q) == QUOTE) {
							int next = q + step(q);
							if (next < n && unitAt(next) == QUOTE) {
								body.write(data, q, step(q));
								q = next + step(next);
							}
							else {
								q = next;
								closed = true;
								break;
							}
						}
						else {
							body.write(data, q, step(q));
							q += step(q);
						}
					}

					if (closed && (q >= n || unitAt(q) == sep || terminatorLength(q) > 0)) {
						item = item(Arrays.copyOfRange(data, fieldStart, q), body.toByteArray(), true, false);
					}
					else {
						// 引用符の後に文字が続く、または引用符が閉じられていない。
						q = closed ? scanToFieldEnd(q) : n;
						byte[] raw = Arrays.copyOfRange(data, fieldStart, q);
						item = item(raw, raw, false, true);
					}
				}
				else {
					q = p;
					boolean hasQuote = false;
					while (q < n && unitAt(q) != sep && terminatorLength(q) == 0) {
						if (unitAt(q) == QUOTE) {
							hasQuote = true;
						}
						q += step(q);
					}
					byte[] raw = Arrays.copyOfRange(data, fieldStart, q);
					item = item(raw, raw, false, hasQuote);
				}

				items.add(item);

				if (q >= n) {
					p = n;
					break;
				}
				if (unitAt(q) == sep) {
					p = q + step(q);
					continue;	// 区切り文字がファイル末尾なら、次の項目は空になる。
				}
				int len = terminatorLength(q);
				terminator = Arrays.copyOfRange(data, q, q + len);
				p = q + len;
				break;
			}

			records.add(new CsvRecord(items, terminator));
		}

		return new CsvDocument(enc, (char) sep, records);
	}
}
