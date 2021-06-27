package com.github.novisoftware.csv;

import java.util.ArrayList;

class Builder {
		/**
		 * 構築中のデータを保持する。
		 */
		ArrayList<ArrayList<String>> buildData;

		/**
		 * 構築用( 構築中の最終行 )。
		 */
		private ArrayList<String> lastLine;

		/**
		 * 構築用( 最後のアイテム )。
		 */
		private StringBuilder item;

		/**
		 * コンストラクタ
		 */
		Builder() {
			this.buildData = new ArrayList<ArrayList<String>>();
			this.lastLine = null;
			this.item = null;
		}

		/**
		 * 1文字を追加する
		 *
		 * @param c 文字
		 */
		void appendChar(char c) {
			if (item == null) {
				item = new StringBuilder();
			}
			item.append(c);
		}

		/**
		 * 現在の最終行に1項目追加する
		 */
		void appendItem() {
			if (lastLine == null) {
				lastLine = new ArrayList<String>();
			}

			if (item != null) {
				lastLine.add(item.toString());
				item = null;
			}
			else {
				lastLine.add("");
			}
		}

		/**
		 * 現在の読み込み済CSVに最終行を追加する
		 */
		void appendLine() {
			if (lastLine != null) {
				buildData.add(lastLine);
				lastLine = null;
			}
			else {
				buildData.add( new ArrayList<String>());
			}
		}
	}

