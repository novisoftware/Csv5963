package com.github.novisoftware.csv;

import java.util.List;

/**
 * CSV の 1 行 (レコード)。
 */
public final class CsvRecord {
	/** 項目。空行は 0 個。 */
	public final List<CsvItem> items;

	/** 行末のバイト列 (CRLF / LF / CR)。ファイルの最終行で改行がなければ空。 */
	public final byte[] terminator;

	CsvRecord(List<CsvItem> items, byte[] terminator) {
		this.items = items;
		this.terminator = terminator;
	}
}
