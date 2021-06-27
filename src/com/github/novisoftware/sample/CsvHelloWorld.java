package com.github.novisoftware.sample;

import java.io.IOException;
import java.util.ArrayList;

import com.github.novisoftware.csv.Csv;
import com.github.novisoftware.csv.CsvMalFormedException;

/**
 * 単純なサンプルプログラム
 *
 */
public class CsvHelloWorld {
	/**
	 * CSVファイルを読み込み、整形式XML を標準出力に出力する。
	 *
	 * @param arg
	 * @throws IOException
	 * @throws CsvMalFormedException
	 */
	static public void main(String arg[]) throws IOException, CsvMalFormedException {
		Csv csv = new Csv("UTF-8", Csv.AnalyzePreference.STRICT_ANALYZE);
		csv.read(arg[0]);

		ArrayList<ArrayList<String>> a = csv.getData();

		System.out.println("<CSV>");
		for (ArrayList<String> a1 : a) {
			System.out.println("<RECORD>");
			for (String a2 : a1) {
				System.out.print("<ITEM>" + a2 + "</ITEM>");
			}
			System.out.println("</RECORD>");
		}
		System.out.println("</CSV>");
	}
}