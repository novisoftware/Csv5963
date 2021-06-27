package com.github.novisoftware.csv;

import java.text.Normalizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



/**
 * 簡単な文字種変換
 *
 */
public class TextConverterUtil {
	public interface Converter5963 {
		/**
		 * 表示用の名前を取得します。
		 *
		 * @return 表示用の名前
		 */
		public String getName();

		/**
		 * 変換を実行します。
		 *
		 * @param src 変換前の文字列
		 * @return 変換後の文字列
		 */
		public String convert(String src);
	}

	/**
	 * コンバーターの一覧
	 */
	static public Converter5963[] converterList = {
			new NormarizeConverter(),
			new ToUpperConverter(),
			new ToLowerConverter(),
			new WhiteSpaceConverter()
	};

	/**
	 * Unicode NFKC を実行します。
	 *
	 * https://www.unicode.org/reports/tr15/tr15-23.html
	 */
	static public class NormarizeConverter implements Converter5963  {
		public String getName() {
			return "Normalizer (Unicode NFKC)";
		}

		public String convert(String src) {
			return Normalizer.normalize(src, Normalizer.Form.NFKC);
		}
	}

	/**
	 * 大文字に揃えます。
	 */
	static public class ToUpperConverter  implements Converter5963 {
		public String getName() {
			return "Upper Case";
		}

		public String convert(String src) {
			return src.toUpperCase();
		}
	}

	/**
	 * 小文字に揃えます。
	 */
	static public class ToLowerConverter  implements Converter5963 {
		public String getName() {
			return "Lower Case";
		}

		public String convert(String src) {
			return src.toLowerCase();
		}
	}

	/**
	 * 連続する空白の除去をします。先頭、末尾の空白は除去します。
	 */
	static public class WhiteSpaceConverter implements Converter5963 {
		public String getName() {
			return "Trim Consecutive Whitespace";
		}

		Pattern pat = Pattern.compile("\\s+");
		Pattern head = Pattern.compile("^\\s");
		Pattern tail = Pattern.compile("\\s$");

		public String convert(String s0) {
			Matcher m1 = pat.matcher(s0);
			String s1 = m1.replaceAll(" ");
			Matcher m2 = head.matcher(s1);
			String s2 = m2.replaceAll("");
			Matcher m3 = tail.matcher(s2);

			return m3.replaceAll("");
		}
	}
}
