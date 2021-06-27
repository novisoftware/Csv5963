package com.github.novisoftware.csv;

public class CsvMalFormedException extends Exception {
	private static final long serialVersionUID = 1L;

	/**
	 * 妥当でないCSVの形式
	 *
	 * @param message 人間に読ませるためのエラーメッセージ
	 */
	CsvMalFormedException(String message) {
		super(message);
	}
}