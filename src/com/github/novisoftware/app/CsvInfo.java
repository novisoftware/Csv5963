package com.github.novisoftware.app;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.table.TableModel;

/**
 * CSVの情報表示 (列数、値の種類、カーディナリティ、列と列の対応関係)。
 * 表示中のテーブルの内容 (編集後を含む) を対象とする。
 */
class CsvInfo {
	/** 列と列の対応を調べる場合の、値の種類の上限 */
	static final int MAX_CATEGORY = 300;

	/** 列の要約で、値を列挙する種類数の上限 */
	private static final int MAX_LISTED_VALUES = 5;

	private static final String EMPTY_LABEL = "(empty)";

	private final TableModel model;
	private final String path;
	private final int rows;
	private final int cols;
	private final String[] names;

	/** 列ごとの (値, 出現数)。出現数の降順。 */
	private final List<List<Map.Entry<String, Integer>>> valueCounts = new ArrayList<List<Map.Entry<String, Integer>>>();

	CsvInfo(String path, TableModel model) {
		this.path = path;
		this.model = model;
		this.rows = model.getRowCount();
		this.cols = model.getColumnCount();
		this.names = new String[cols];
		for (int c = 0; c < cols; c++) {
			this.names[c] = model.getColumnName(c);
			this.valueCounts.add(count(c));
		}
	}

	private String cell(int row, int col) {
		Object o = model.getValueAt(row, col);
		return o == null ? "" : o.toString();
	}

	private List<Map.Entry<String, Integer>> count(int col) {
		Map<String, Integer> map = new HashMap<String, Integer>();
		for (int r = 0; r < rows; r++) {
			String v = cell(r, col);
			Integer n = map.get(v);
			map.put(v, n == null ? 1 : n + 1);
		}
		List<Map.Entry<String, Integer>> list = new ArrayList<Map.Entry<String, Integer>>(map.entrySet());
		Collections.sort(list, (a, b) -> {
			int d = b.getValue() - a.getValue();
			return d != 0 ? d : a.getKey().compareTo(b.getKey());
		});
		return list;
	}

	private int unique(int col) {
		return valueCounts.get(col).size();
	}

	private static String label(String value) {
		return value.isEmpty() ? EMPTY_LABEL : value;
	}

	private static String percent(int n, int total) {
		return total == 0 ? "0.0%" : String.format("%.1f%%", 100.0 * n / total);
	}

	private String colLabel(int col) {
		return "[" + (col + 1) + "] " + names[col];
	}

	/** 列 a の値が決まると列 b の値が一意に決まるか。 */
	private boolean determines(int a, int b) {
		Map<String, String> map = new HashMap<String, String>();
		for (int r = 0; r < rows; r++) {
			String va = cell(r, a);
			String vb = cell(r, b);
			String old = map.put(va, vb);
			if (old != null && !old.equals(vb)) {
				return false;
			}
		}
		return true;
	}

	private boolean isCategorical(int col) {
		return unique(col) <= MAX_CATEGORY;
	}

	String summary() {
		StringBuilder sb = new StringBuilder();
		sb.append("File: ").append(path).append('\n');
		sb.append("Rows (excluding header): ").append(rows).append('\n');
		sb.append("Columns: ").append(cols).append('\n');
		sb.append("Cardinality = unique values / rows (1.0: all values distinct)\n");

		for (int c = 0; c < cols; c++) {
			List<Map.Entry<String, Integer>> list = valueCounts.get(c);
			int empty = 0;
			int minLen = Integer.MAX_VALUE;
			int maxLen = 0;
			for (Map.Entry<String, Integer> e : list) {
				if (e.getKey().isEmpty()) {
					empty = e.getValue();
				}
				minLen = Math.min(minLen, e.getKey().length());
				maxLen = Math.max(maxLen, e.getKey().length());
			}

			sb.append('\n').append(colLabel(c)).append('\n');
			sb.append("  Unique values: ").append(list.size());
			sb.append("   Cardinality: ").append(rows == 0 ? "-" : String.format("%.4f", (double) list.size() / rows));
			sb.append("   Empty: ").append(empty).append('\n');
			if (!list.isEmpty()) {
				sb.append("  Length: ").append(minLen).append(" - ").append(maxLen).append('\n');
			}

			int shown = Math.min(list.size(), MAX_LISTED_VALUES);
			if (shown > 0) {
				sb.append(list.size() <= MAX_LISTED_VALUES ? "  Values:\n" : "  Top " + shown + " values:\n");
			}
			for (int i = 0; i < shown; i++) {
				Map.Entry<String, Integer> e = list.get(i);
				sb.append("    ").append(label(e.getKey())).append("  ").append(e.getValue())
					.append(" (").append(percent(e.getValue(), rows)).append(")\n");
			}
		}

		sb.append('\n').append("Column relations (columns with 2 to ").append(MAX_CATEGORY).append(" unique values)\n");
		boolean found = false;
		for (int a = 0; a < cols; a++) {
			if (!isCategorical(a) || unique(a) < 2) {
				continue;
			}
			for (int b = 0; b < cols; b++) {
				if (a == b || !isCategorical(b) || unique(b) < 2) {
					continue;
				}
				boolean ab = determines(a, b);
				if (!ab) {
					continue;
				}
				boolean ba = determines(b, a);
				if (ba && a > b) {
					continue;	// 双方向は片方のみ表示
				}
				found = true;
				sb.append("  ").append(colLabel(a)).append(ba ? " <-> " : " -> ").append(colLabel(b))
					.append(ba ? "  (one-to-one)" : "  (each value determines the other)").append('\n');
			}
		}
		if (!found) {
			sb.append("  (none)\n");
		}

		return sb.toString();
	}

	/**
	 * 列 a の各値に対して、列 b の値がどのように対応するかの表を作る。
	 */
	String correlation(int a, int b) {
		StringBuilder sb = new StringBuilder();
		sb.append(colLabel(a)).append("  ->  ").append(colLabel(b)).append('\n');

		if (a == b) {
			sb.append("\nSelect two different columns.\n");
			return sb.toString();
		}
		if (!isCategorical(a) || !isCategorical(b)) {
			sb.append("\nNot available: both columns need at most ").append(MAX_CATEGORY).append(" unique values.\n");
			sb.append("  ").append(colLabel(a)).append(": ").append(unique(a)).append(" unique values\n");
			sb.append("  ").append(colLabel(b)).append(": ").append(unique(b)).append(" unique values\n");
			return sb.toString();
		}

		boolean ab = determines(a, b);
		boolean ba = determines(b, a);
		sb.append('\n');
		sb.append("Each value of ").append(colLabel(a)).append(ab ? " determines " : " does not determine ")
			.append("a single value of ").append(colLabel(b)).append(".\n");
		sb.append("Each value of ").append(colLabel(b)).append(ba ? " determines " : " does not determine ")
			.append("a single value of ").append(colLabel(a)).append(".\n");

		Map<String, Map<String, Integer>> cross = new LinkedHashMap<String, Map<String, Integer>>();
		for (Map.Entry<String, Integer> e : valueCounts.get(a)) {
			cross.put(e.getKey(), new HashMap<String, Integer>());
		}
		for (int r = 0; r < rows; r++) {
			Map<String, Integer> m = cross.get(cell(r, a));
			String vb = cell(r, b);
			Integer n = m.get(vb);
			m.put(vb, n == null ? 1 : n + 1);
		}

		for (Map.Entry<String, Map<String, Integer>> e : cross.entrySet()) {
			int total = 0;
			for (int n : e.getValue().values()) {
				total += n;
			}
			List<Map.Entry<String, Integer>> list = new ArrayList<Map.Entry<String, Integer>>(e.getValue().entrySet());
			Collections.sort(list, (x, y) -> {
				int d = y.getValue() - x.getValue();
				return d != 0 ? d : x.getKey().compareTo(y.getKey());
			});

			sb.append('\n').append(label(e.getKey())).append("  (").append(total).append(" rows)\n");
			for (Map.Entry<String, Integer> v : list) {
				sb.append("    ").append(label(v.getKey())).append("  ").append(v.getValue())
					.append(" (").append(percent(v.getValue(), total)).append(")\n");
			}
		}

		return sb.toString();
	}

	private static JTextArea createTextArea(String text) {
		JTextArea area = new JTextArea(text);
		area.setEditable(false);
		area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		area.setCaretPosition(0);
		return area;
	}

	void showSummary(java.awt.Component parent) {
		JScrollPane sp = new JScrollPane(createTextArea(summary()));
		sp.setPreferredSize(new Dimension(640, 480));
		JOptionPane.showMessageDialog(parent, sp, "Column Summary", JOptionPane.PLAIN_MESSAGE);
	}

	void showCorrelation(java.awt.Component parent) {
		if (cols < 2) {
			JOptionPane.showMessageDialog(parent, "At least 2 columns are required.", "Column Correlation",
					JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		String[] items = new String[cols];
		for (int c = 0; c < cols; c++) {
			items[c] = colLabel(c);
		}
		final JComboBox<String> comboA = new JComboBox<String>(items);
		final JComboBox<String> comboB = new JComboBox<String>(items);
		comboB.setSelectedIndex(1);

		final JTextArea area = createTextArea("");
		final ActionListener update = e -> {
			area.setText(correlation(comboA.getSelectedIndex(), comboB.getSelectedIndex()));
			area.setCaretPosition(0);
		};
		comboA.addActionListener(update);
		comboB.addActionListener(update);
		update.actionPerformed(null);

		JPanel selector = new JPanel(new GridLayout(2, 2, 4, 4));
		selector.add(new JLabel("Column N (source):"));
		selector.add(comboA);
		selector.add(new JLabel("Column M (target):"));
		selector.add(comboB);

		JScrollPane sp = new JScrollPane(area);
		sp.setPreferredSize(new Dimension(640, 440));

		JPanel panel = new JPanel(new BorderLayout(0, 8));
		panel.add(selector, BorderLayout.NORTH);
		panel.add(sp, BorderLayout.CENTER);

		JOptionPane.showMessageDialog(parent, panel, "Column Correlation", JOptionPane.PLAIN_MESSAGE);
	}
}
