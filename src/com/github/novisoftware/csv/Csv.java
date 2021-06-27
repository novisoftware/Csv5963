package com.github.novisoftware.csv;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

/**
 *
 * CSVフォーマットについては RFC4180 にまとめられていて日本語訳は以下。
 * http://www.kasai.fm/wiki/rfc4180jp
 *
 * Wikipediaには以下のような記載あり(思わず「本当だろうか」と驚いてしまう)。
 * バックスラッシュを前につける書き方は、ありそうだけれど、扱わない。
 *
 * <pre>
 * レコード区切り文字列
 *     CR LF を区切り文字列として扱わない処理系がある。
 * フィールド区切り文字
 *     全角コンマ「，」を区切りとみなす処理系がある。
 * ダブルクォート文字の表現
 *     ダブルクォート文字を表現する方法として「ダブルクォートを重ねる」処理系と、「バックスラッシュを前につける」処理系が存在する。
 * ダブルクォート文字の有無
 *     多くのソフトウェアは、必要なときのみフィールドをダブルクォートで囲む。ただし、そうでないファイルも読み取れる。
 * フィールド数
 *     読み取りファイルのフィールド数が一定でない場合、ほとんどのソフトウェアは、空文字列（長さ0の文字列）からなるフィールドを適宜追加して数をそろえる。
 * 空行、フィールド数が0個のレコード
 *     空行を全てのフィールドが空文字列であるとして処理する処理系と、空行を無視する処理系がある。
 * 注釈
 *     特定の書式の文字列を注釈行として扱う処理系がある。
 * </pre>
 *
 */
public class Csv {
	/**
	 * 読み取り方法の列挙定義。
	 *
	 * ファイルが若干いい加減なフォーマットだった場合に、エラーで打ち切るか、とにかく読み取るかを決める。
	 * 読み取りのポリシーみたいなもの。
	 */
	static public enum AnalyzePreference {
		STRICT_ANALYZE,	// 厳密な
		LOOSE_ANALYZE		// ややいい加減な
	}

	/**
	 * 読み取り方法のデフォルト
	 */
	protected AnalyzePreference analyzePreference = AnalyzePreference.STRICT_ANALYZE;

	/**
	 * メニュー等で使用するための (文字列, 読み取り方法列挙定義) の対
	 */
	static public class AnalyzePreferenceElem {
		final public String name;
		final public AnalyzePreference pref;

		AnalyzePreferenceElem(String name, AnalyzePreference pref) {
			this.name = name;
			this.pref = pref;
		}
	}

	/**
	 * 読み取り方法の一覧
	 */
	static public final AnalyzePreferenceElem[] AnalyzePreferenceList = {
			new AnalyzePreferenceElem("Strict", AnalyzePreference.STRICT_ANALYZE),
			new AnalyzePreferenceElem("Loose", AnalyzePreference.LOOSE_ANALYZE)
	};

	/**
	 * 読み取り方法を変更する
	 *
	 * @param analyzePreference 指定する読み込み方法
	 */
	void setAnalyzePreference(AnalyzePreference analyzePreference) {
		this.analyzePreference = analyzePreference;
	}

	/**
	 * 現在指定されている読み込み方法を取得する。
	 *
	 * @return 現在指定されている読み込み方法
	 */
	AnalyzePreference getAnalyzePreference() {
		return analyzePreference;
	}

	/**
	 * エンコードのリスト
	 *
	 * https://docs.oracle.com/javase/jp/1.5.0/guide/intl/encoding.doc.html
	 */
	static final public String encodeList[] = {
//		"Shift_JIS",
		"MS932",
		"UTF-8",
		"UTF-16",
		"UTF-16BE",
		"UTF-16LE"
	};

	/**
	 * 読み取り中の状態の列挙定義
	 */
	private enum ReadState {
		NONE,				// 行頭、または、区切り文字の直後
		FLAT,				// クオートされていない文字列の読み取り中
		QUOTE,				// クオートされている文字列の読み取り中
		QUOTE_IN_QUOTE	// クオートされている文字列の読み取り中にクオートを読み取った
	}

	/**
	 * 読み取り中の状態
	 */
	private ReadState readState;

	/**
	 * クオート文字
	 */
	char QUOTE_CHAR = '"';

	/**
	 * 区切り文字
	 */
	char SEPARATE_CHAR = ',';

	private boolean isWhiteSpace(char c) {
		if (c == ' ' || c == '\t') {
			return true;
		}

		return false;
	}

	/**
	 * 読み取ったデータを保持する。
	 */
	protected ArrayList<ArrayList<String>> data;

	/**
	 * 読み取ったデータを取得する。
	 *
	 * @return 読み取ったデータ。ArrayList<String>を要素とするArrayList。各 ArrayList<String>は CSVファイルの各レコード(行)に対応する。
	 */
	public ArrayList<ArrayList<String>> getData() {
		return data;
	}

	/**
	 * CSVの解析。
	 *
	 * @param line
	 * @throws CsvMalFormedException
	 */
	private void analyze(Builder builder, int numberOfLine, String line) throws CsvMalFormedException {
		int length = line.length();
		for (int i = 0 ; i < length ; i++) {
			char c = line.charAt(i);

			// デバッグする場合
			// System.out.println( "" + readState.toString() + "   " + c );

			if (readState == ReadState.NONE) {	// 行頭、カンマの読み取り直後
				if (c == QUOTE_CHAR) {
					readState = ReadState.QUOTE;
				}
				else if (c == SEPARATE_CHAR) {
					builder.appendItem();
				}
				else if (isWhiteSpace(c)) {
					if (this.analyzePreference == AnalyzePreference.STRICT_ANALYZE) {
						builder.appendItem();
					}
					else {
						; // 何もしない
					}
				}
				else {
					readState = ReadState.FLAT;
					builder.appendChar(c);
				}
			}
			else if (readState == ReadState.FLAT) {	// クオートされない文字列の読み取り中
				if (c == QUOTE_CHAR) {
					if (this.analyzePreference == AnalyzePreference.STRICT_ANALYZE) {
						// クオートされていない文字列の中に、クオート用の文字は来ないとして、
						// 妥当でないCSVファイルをエラーにしてしまう場合は以下。
						throw new CsvMalFormedException("Unexpected quote char in unquoted string(line " + numberOfLine + ").");
					}
					else if (this.analyzePreference == AnalyzePreference.LOOSE_ANALYZE) {
						// 無視して、追加してしまうか。
						builder.appendChar(QUOTE_CHAR);
					}
				}
				else if (c == SEPARATE_CHAR) {
					builder.appendItem();
					readState = ReadState.NONE;
				}
				else {
					builder.appendChar(c);
				}
			}
			else if (readState == ReadState.QUOTE) {
				if (c == QUOTE_CHAR) {
					readState = ReadState.QUOTE_IN_QUOTE;
				}
				else {
					builder.appendChar(c);
				}
			}
			else if (readState == ReadState.QUOTE_IN_QUOTE) {
				if (c == QUOTE_CHAR) {
					builder.appendChar(QUOTE_CHAR);
					readState = ReadState.QUOTE;
				}
				else if (c == SEPARATE_CHAR) {
					builder.appendItem();
					readState = ReadState.NONE;;
				}
				else {
					// クオートされた文字列の読み取り中、クオートの後がカンマでもクオートでもない場合
					if (this.analyzePreference == AnalyzePreference.STRICT_ANALYZE) {
						// 妥当でないCSVファイル
						throw new CsvMalFormedException("Unexpected char " + c + " after quote char in quoted string.");
					}
					else if (this.analyzePreference == AnalyzePreference.LOOSE_ANALYZE) {
						// 適当でもいい場合も難しいが、クオートを戻して読み取りを続ける。
						builder.appendChar(QUOTE_CHAR);
						readState = ReadState.QUOTE;
					}
				}
			}
		}

		// 行末になった時の動作 1/2
		if (readState == ReadState.NONE) {
			;	// 処理の必要なし
		}
		else if (readState == ReadState.FLAT) {
			builder.appendItem();
			readState = ReadState.NONE;
		}
		else if (readState == ReadState.QUOTE) {
			;	// 処理の必要なし
		}
		else if (readState == ReadState.QUOTE_IN_QUOTE) {
			builder.appendItem();
			readState = ReadState.NONE;;
		}

		// 行末になった時の動作 2/2
		if (readState == ReadState.NONE) {
			builder.appendLine();
		}
		else if (readState == ReadState.QUOTE) {
			builder.appendChar('\n');
		}
	}

	/**
	 * ファイル終端に到着した時の処理
	 *
	 * @throws CsvMalFormedException
	 */
	void eofRecieved(Builder builder) throws CsvMalFormedException {
		if (readState == ReadState.NONE) {
			;	// 処理の必要なし
		}
		else if (readState == ReadState.FLAT) {
			// 内部状態がおかしい
			throw new CsvMalFormedException("Bad Internal State(at EOF).");
		}
		else if (readState == ReadState.QUOTE) {
			// クオートされた文字列の読み取り中にファイルが終了した場合
			if (this.analyzePreference == AnalyzePreference.STRICT_ANALYZE) {
				// 厳密にはCSVフォーマットがおかしいので読み取りを打ち切る
				throw new CsvMalFormedException("Reached EOF in unquoted string.");
			}
			else if (this.analyzePreference == AnalyzePreference.LOOSE_ANALYZE) {
				// 適当でもいい場合は、無視して追加してしまう
				builder.appendItem();
				builder.appendLine();
			}
		}
		else if (readState == ReadState.QUOTE_IN_QUOTE) {
			// 内部状態がおかしい
			throw new CsvMalFormedException("Bad Internal State(at EOF).");
		}

		this.data = builder.buildData;
	}

	/**
	 * CSVファイルの解析を実行します。
	 *
	 * @param path
	 * @param encode
	 * @throws IOException
	 * @throws CsvMalFormedException
	 */
	private void build(String path, String encode) throws IOException, CsvMalFormedException {
		this.readState = ReadState.NONE;

		// 現在読み取り中の行数。 エラーメッセージを出力する場合に使用する。
		int numberOfLine = 1;

		Builder builder = new Builder();
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream( new File(path) ), encode));

		while(true) {
			String line = reader.readLine();
			if (line == null) {
				break;
			}
			analyze(builder, numberOfLine, line);
			numberOfLine++;
		}
		eofRecieved(builder);
		reader.close();

		this.data = builder.buildData;
	}

	String path;
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	String encode;
	public void read(String path) throws IOException, CsvMalFormedException {
		build(path, encode);
		this.path = path;
	}

	/**
	 * CSVファイルの書き出し。
	 *
	 * @param path 出力先のパス
	 * @throws IOException
	 */
	public void write(String path)  throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream( new File(path) ), encode));
		for (ArrayList<String> record : data) {
			boolean isFirst = true;
			for (String item : record) {
				if (isFirst) {
					isFirst = false;
				}
				else {
					writer.write(',');
				}

				if (item.indexOf(this.QUOTE_CHAR) == -1 || item.indexOf(this.SEPARATE_CHAR) == -1) {
					writer.write(item);
				}
				else {
					writer.write("\"" + item.replaceAll("\"", "\"\"") + "\"");
				}
			}
			writer.write('\n');
		}

		writer.close();
	}

	public void setEncode(String encode) {
		this.encode = encode;
	}

	public String getEncode() {
		return this.encode;
	}

	public Csv(String encode, AnalyzePreference prefer) throws IOException, CsvMalFormedException {
		this.encode = encode;
		this.analyzePreference =prefer;
	}
}
