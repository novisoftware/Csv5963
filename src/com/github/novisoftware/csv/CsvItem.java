package com.github.novisoftware.csv;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;

/**
 * CSV の 1 個のフィールド。内容はバイト列のまま保持する。
 * 配列の内容は変更しないこと。
 */
public final class CsvItem {
	/** ファイル上の元のバイト列 (引用符・エスケープを含み、区切り文字は含まない)。 */
	public final byte[] raw;

	/** 引用符を外し、引用符のエスケープ("" → ")を戻した内容。irregular のときは raw と同じ。 */
	public final byte[] body;

	/** 引用符で囲まれた、書式どおりの項目。 */
	public final boolean quoted;

	/** 標準的な書式に当てはまらない項目 (raw をそのまま保持している)。 */
	public final boolean irregular;

	private final CsvEncoding encoding;

	CsvItem(CsvEncoding encoding, byte[] raw, byte[] body, boolean quoted, boolean irregular) {
		this.encoding = encoding;
		this.raw = raw;
		this.body = body;
		this.quoted = quoted;
		this.irregular = irregular;
	}

	/**
	 * body を文字コードに従ってデコードした文字列を返す。
	 * デコードできない場合は null。結果は保持しない。
	 */
	public String getText() {
		CharsetDecoder decoder = encoding.charset.newDecoder()
				.onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT);
		try {
			return decoder.decode(ByteBuffer.wrap(body)).toString();
		} catch (CharacterCodingException e) {
			return null;
		}
	}

	/**
	 * body と quoted から raw を再生成する。irregular の場合は raw をそのまま返す。
	 */
	public byte[] regenerateRaw() {
		if (irregular) {
			return raw;
		}
		if (!quoted) {
			return body;
		}
		return quote(encoding, body);
	}

	/**
	 * body を引用符で囲む (本文中の引用符は "" にする)。
	 */
	static byte[] quote(CsvEncoding encoding, byte[] body) {
		int unit = encoding.unitSize;
		byte[] quote = encoding.encodeAscii('"');
		ByteArrayOutputStream out = new ByteArrayOutputStream(body.length + 2 * unit);
		out.write(quote, 0, quote.length);
		int p = 0;
		while (p < body.length) {
			int step = Math.min(unit, body.length - p);
			out.write(body, p, step);
			if (encoding.unitAt(body, p) == '"') {
				out.write(quote, 0, quote.length);
			}
			p += step;
		}
		out.write(quote, 0, quote.length);
		return out.toByteArray();
	}
}
