package com.github.novisoftware.csv;

import java.io.IOException;
import java.util.ArrayList;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import com.github.novisoftware.app.Utils;

/**
 * CSVの読み取り用の処理に JTabbedPane の TableModel に関係する処理を追加したもの。
 *
 */
public class CsvToModel extends Csv {
	DefaultTableModel model;

	public int getRecords() {
		return data.size();
	}

	public int getColumns() {
		int max = 0;

		for (ArrayList<String> a : data) {
			if (a.size() > max) {
				max = a.size();
			}
		}

		return max;
	}

	/**
	 * 表示用データ本体を作成する。
	 *
	 * @return 作成した表示用データ。
	 */
	public String[][] getArray() {
		int records = this.getRecords();
		int columns = this.getColumns();

		Utils.debug("CSV の 行数: " + records);
		Utils.debug("CSV の 桁数: " + columns);

		String[][] arr = new String[records][columns];
		for (int r = 0 ; r < records; r++) {
			ArrayList<String> record = data.get(r);

			for (int col = 0; col < columns && col < record.size()  ; col++) {
				arr[r][col] = record.get(col);
			}
		}

		return arr;
	}

	/**
	 * ヘッダーを作成する。
	 * ここでは 1行目の内容をそのままヘッダーとしている。
	 *
	 * @return 作成した表示用データ。
	 */
	public String[] getHeader() {
		int records = this.getRecords();
		int columns = this.getColumns();

		if (records > 1) {
			String[] header = new String[columns];
			ArrayList<String> record = data.get(0);

			for (int col = 0; col < columns; col++) {
				if (col < record.size()) {
					header[col] = record.get(col);
				}
				else {
					header[col] = "";
				}
			}

			Utils.debug("CSVヘッダーの桁数: " + header.length);

			return header;
		}


		return null;
	}

	public void createModel() {
		this.model = new DefaultTableModel(this.getArray(), this.getHeader());
	}

	/**
	 * JTable の TableModel から モデルのデータを拾い直します。
	 */
	public void updateData() {
		ArrayList<ArrayList<String>> array = new ArrayList<ArrayList<String>>();

		int rowCount = this.model.getRowCount();
		int columnCount = this.model.getColumnCount();

		Utils.debug("rows = " + rowCount);
		Utils.debug("columns = " + columnCount);

		for (int row = 0; row < rowCount; row++) {
			ArrayList<String> record = new ArrayList<String>();
			for (int column = 0; column < columnCount; column++) {
				Object item = this.model.getValueAt(row, column);
				String itemString = "";
				if (item != null && item instanceof String) {
					itemString = (String)item;
				}
				record.add(itemString);
			}
			array.add(record);
		}

		this.data = array;
	}

	public TableModel getModel() {
		return this.model;
	}

	public CsvToModel(String encode, Csv.AnalyzePreference prefer) throws IOException, CsvMalFormedException {
		super(encode, prefer);
	}

	// 以降、簡単な文字種変換
	public void doConvert(TextConverterUtil.Converter5963 converter) {
		this.updateData();

		ArrayList<ArrayList<String>> array = new ArrayList<ArrayList<String>>();
		for (ArrayList<String> record : data) {
			ArrayList<String> newRecord = new ArrayList<String>();
			for (String item : record) {
				newRecord.add(converter.convert(item));
			}
			array.add(newRecord);
		}

		data = array;

		this.createModel();
	}

}
