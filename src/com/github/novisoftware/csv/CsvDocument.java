package com.github.novisoftware.csv;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * バイト列のまま保持した CSV ファイル全体。
 */
public final class CsvDocument {
	public final CsvEncoding encoding;
	public final char separator;
	public final List<CsvRecord> records;

	CsvDocument(CsvEncoding encoding, char separator, List<CsvRecord> records) {
		this.encoding = encoding;
		this.separator = separator;
		this.records = records;
	}

	/**
	 * ファイルのバイト列を再構築する。読み取り元のバイト列と一致する。
	 */
	public byte[] toBytes() {
		byte[] sep = encoding.encodeAscii(separator);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(encoding.bom, 0, encoding.bom.length);
		for (CsvRecord record : records) {
			boolean first = true;
			for (CsvItem item : record.items) {
				if (!first) {
					out.write(sep, 0, sep.length);
				}
				first = false;
				out.write(item.raw, 0, item.raw.length);
			}
			out.write(record.terminator, 0, record.terminator.length);
		}
		return out.toByteArray();
	}
}
