package com.github.novisoftware.csv;

import java.nio.charset.Charset;
import java.util.Locale;

/**
 * CSVファイルの文字コードの情報。
 *
 * バイト列のまま CSV を分解するために、コードユニットの大きさ、バイト順、BOM を保持する。
 * 対象は ASCII 互換の文字コード (UTF-8, MS932 など) と UTF-16 (BE / LE)。
 */
public final class CsvEncoding {
	private static final byte[] NO_BYTES = new byte[0];

	public final String name;

	/** BOM を除く本文のデコードに使う Charset。UTF-16 は BOM から決めたバイト順のもの。 */
	public final Charset charset;

	/** 1 コードユニットのバイト数 (1 または 2)。 */
	public final int unitSize;

	public final boolean bigEndian;

	/** ファイルの先頭にあった BOM。なければ空。 */
	public final byte[] bom;

	private CsvEncoding(String name, Charset charset, int unitSize, boolean bigEndian, byte[] bom) {
		this.name = name;
		this.charset = charset;
		this.unitSize = unitSize;
		this.bigEndian = bigEndian;
		this.bom = bom;
	}

	private static boolean startsWith(byte[] data, int... head) {
		if (data.length < head.length) {
			return false;
		}
		for (int i = 0; i < head.length; i++) {
			if ((data[i] & 0xFF) != head[i]) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 文字コード名とファイルの内容から、文字コードの情報を作る。
	 *
	 * @param name 文字コード名 (Csv.encodeList の名前)
	 * @param data ファイル全体のバイト列 (BOM の判定に使う)
	 */
	public static CsvEncoding of(String name, byte[] data) {
		String upper = name.toUpperCase(Locale.ROOT);

		if (upper.equals("UTF-8") || upper.equals("UTF8")) {
			byte[] bom = startsWith(data, 0xEF, 0xBB, 0xBF) ? new byte[] { data[0], data[1], data[2] } : NO_BYTES;
			return new CsvEncoding(name, Charset.forName("UTF-8"), 1, true, bom);
		}

		if (upper.startsWith("UTF-16") || upper.equals("UTF16")) {
			boolean beBom = startsWith(data, 0xFE, 0xFF);
			boolean leBom = startsWith(data, 0xFF, 0xFE);
			boolean big;
			byte[] bom = NO_BYTES;
			if (upper.endsWith("LE")) {
				big = false;
				if (leBom) {
					bom = new byte[] { data[0], data[1] };
				}
			}
			else if (upper.endsWith("BE")) {
				big = true;
				if (beBom) {
					bom = new byte[] { data[0], data[1] };
				}
			}
			else {
				// BOM がなければ、Java の UTF-16 と同じくビッグエンディアンとして扱う。
				big = !leBom;
				if (beBom || leBom) {
					bom = new byte[] { data[0], data[1] };
				}
			}
			return new CsvEncoding(name, Charset.forName(big ? "UTF-16BE" : "UTF-16LE"), 2, big, bom);
		}

		return new CsvEncoding(name, Charset.forName(name), 1, true, NO_BYTES);
	}

	/**
	 * ASCII の 1 文字を、この文字コードのバイト列にする。
	 */
	public byte[] encodeAscii(char c) {
		if (unitSize == 1) {
			return new byte[] { (byte) c };
		}
		return bigEndian ? new byte[] { 0, (byte) c } : new byte[] { (byte) c, 0 };
	}

	/**
	 * data の pos から始まるコードユニットの値を返す。
	 * 末尾に 1 ユニットに満たないバイトしか残っていない場合は -1 を返す。
	 */
	public int unitAt(byte[] data, int pos) {
		if (pos + unitSize > data.length) {
			return -1;
		}
		if (unitSize == 1) {
			return data[pos] & 0xFF;
		}
		int a = data[pos] & 0xFF;
		int b = data[pos + 1] & 0xFF;
		return bigEndian ? (a << 8) | b : (b << 8) | a;
	}
}
