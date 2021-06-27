package com.github.novisoftware.app;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.github.novisoftware.csv.Csv;
import com.github.novisoftware.csv.CsvMalFormedException;
import com.github.novisoftware.csv.CsvToModel;
import com.github.novisoftware.csv.TextConverterUtil;

/**
 * CSVを表示するテーブル。
 * JPanelにJTableを貼り付けたもの。
 *
 */
class CsvPanel extends JPanel {
	final JFrame parent;
	JTable table;
	JScrollPane sp;
	CsvToModel csv;

	String getPath() {
		return csv.getPath();
	}

	/**
	 * タブの表示に使用する名前を決めます。
	 *
	 * @return パスを含まないファイル名
	 */
	String getNameForTab() {
		return new File(this.getPath()).getName();
	}

	void updateTable() {
		this.removeAll();
		this.table = new JTable(this.csv.getModel());
		this.table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

		// // テーブルの中のフォントを変更するには以下のような実装みたいだけれど、
		// // 環境の差異を吸収するのは労力大。
		// // table.setFont( new Font( "メイリオ", Font.BOLD ,16 ) );

		this.sp = new JScrollPane(table,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		this.add(this.sp);
	}

	CsvPanel(JFrame parent, CsvToModel csv) {
		this.parent = parent;
		this.csv = csv;

		this.setLayout(new CardLayout());

		this.csv.createModel();
		this.updateTable();
	}

	void save() {
		csv.updateData();
		try {
			csv.write(csv.getPath());
		} catch(Exception ex) {
			JOptionPane.showMessageDialog(parent, "Save Failure: " + ex.toString(), "Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	void save(String path) {
		csv.updateData();
		try {
			csv.write(path);
		} catch(Exception ex) {
			JOptionPane.showMessageDialog(parent, "Save Failure: " + ex.toString(), "Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		csv.setPath(path);
	}

	void textConvert(TextConverterUtil.Converter5963 converter) {
		this.csv.doConvert(converter);
		this.updateTable();
	}

}

/**
 * ウィンドウ上部のメニューバー
 *
 */
class MenuBar5963 extends JMenuBar {
	final Csv5963 parent;

	JMenu     fileMenu;
	JMenu     setEncode;
	JMenuItem open;
	JMenu     reloadWithEncode;
	JMenuItem save;
	JMenuItem saveAs;
	JMenuItem close;
	JMenuItem exit;
	JMenu     setting;
	JMenu     csvReadPreference;
	JMenu     convert;
	JMenu     saveWithEncode;
	JMenu     helpMenu;
	JMenuItem about;

	/**
	 * デフォルトのエンコードを指定するメニューを更新し、
	 * 現在指定されているエンコードがマークされるようにする。
	 */
	void updateSetEncodeMenu() {
		final MenuBar5963 thisObj = this;

		this.setEncode.removeAll();
		for (String newEncode: Csv.encodeList) {
			JMenuItem item = new JMenuItem(newEncode, Utils.IconMark(newEncode.equals(parent.defaultEncode)));
			item.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					parent.defaultEncode = newEncode;
					parent.updateTitleAndStatus();
					thisObj.updateSetEncodeMenu();
				}
			});
			this.setEncode.add(item);
		}
	}

	/**
	 * 「エンコードを指定してリロード」のメニューを更新し、
	 * 現在指定されているエンコードがマークされるようにする。
	 *
	 * @param oldEncode
	 */
	void updateReloadEncodeMenu(String oldEncode) {
		this.reloadWithEncode.removeAll();

		for (String newEncode: Csv.encodeList) {
			JMenuItem item = new JMenuItem(newEncode, Utils.IconMark(newEncode.equals(oldEncode)));
			item.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					parent.reloadWithEncode(newEncode);
				}
			});
			this.reloadWithEncode.add(item);
		}
	}

	/**
	 * 「エンコードを指定して保存」のメニューを更新し、
	 * 現在指定されているエンコードがマークされるようにする。
	 *
	 * @param oldEncode
	 */
	void updateSaveEncodeMenu(String oldEncode) {
		this.saveWithEncode.removeAll();

		for (String newEncode: Csv.encodeList) {
			JMenuItem item = new JMenuItem(newEncode, Utils.IconMark(newEncode.equals(oldEncode)));
			item.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					parent.saveWithEncode(newEncode);
				}
			});
			this.saveWithEncode.add(item);
		}
	}

	/**
	 * セッティングのメニューを更新する。
	 */
	void updateSettingMenu() {
		final MenuBar5963 thisObj = this;

		this.csvReadPreference.removeAll();

		for (Csv.AnalyzePreferenceElem prefer :  Csv.AnalyzePreferenceList) {
			JMenuItem item = new JMenuItem(prefer.name, Utils.IconMark(parent.analyzePreference == prefer.pref));
			item.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					parent.analyzePreference = prefer.pref;
					thisObj.updateSettingMenu();
				}
			});
			this.csvReadPreference.add(item);
		}
	}

	MenuBar5963(Csv5963 parent) {
		this.parent = parent;

		this.fileMenu = new JMenu("File");

		// ファイルを開く
		this.open = new JMenuItem("Open");
		this.open.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				parent.openCsvFileWithDialog();
			}
		});

		// エンコードを指定して再読み込み
		this.reloadWithEncode = new JMenu("Reload with Encode");

		// デフォルトのエンコードを指定
		this.setEncode = new JMenu("Set Default Encode");
		this.updateSetEncodeMenu();

		// 上書き保存
		this.save = new JMenuItem("Save");
		this.save.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				parent.save();
			}
		});

		// 名前をつけて保存
		this.saveAs = new JMenuItem("Save As");
		this.saveAs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				parent.saveWithDialog();
			}
		});

		// 閉じる
		this.close = new JMenuItem("Close");
		this.close.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				parent.closeTab();
			}
		});

		// 終了
		this.fileMenu.addSeparator();
		this.exit = new JMenuItem("Exit");
		this.exit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// 未保存のファイルがあれば、ダイアログを出すというのが良くあるけれど、
				// ファイルの編集状態を管理していないので、しない。
				System.exit(0);
			}
		});

		// セッティング
		this.setting =  new JMenu("Setting");
		this.csvReadPreference = new JMenu("CSV Read Preference");
		this.updateSettingMenu();

		// 変換
		this.convert = new JMenu("Convert");
		for (TextConverterUtil.Converter5963 converter: TextConverterUtil.converterList) {
			JMenuItem convertMenu = new JMenuItem(converter.getName());
			convertMenu.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					Component c = parent.tabs.getSelectedComponent();
					if (c == null || !(c instanceof CsvPanel)) {
						return;
					}
					CsvPanel csvPanel = (CsvPanel)c;

					csvPanel.textConvert(converter);
				}
			});
			this.convert.add(convertMenu);
		}
		this.convert.addSeparator();

		this.saveWithEncode = new JMenu("Change Encode (with Save)");
		this.convert.add(saveWithEncode);

		// Help
		this.helpMenu = new JMenu("Help");
		this.about = new JMenuItem("About");
		this.about.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String message = "CSV 5963(ゴクローサン),\n\nnovisoftware 2021.";

				JOptionPane.showMessageDialog(parent, message);
			}
		});

		// メニューをレイアウトする
		this.add(fileMenu);
		this.fileMenu.add(open);
		this.fileMenu.add(reloadWithEncode);
		this.fileMenu.add(setEncode);
		this.fileMenu.add(save);
		this.fileMenu.add(saveAs);
		this.fileMenu.addSeparator();
		this.fileMenu.add(close);
		this.fileMenu.add(exit);

		this.add(setting);
		this.setting.add(csvReadPreference);

		this.add(convert);

		this.add(helpMenu);
		this.helpMenu.add(about);
	}
}

/**
 * ドラッグ & ドロップの受信処理。
 *
 * 定形的な処理。
 */
class DropTargetListener5963 implements DropTargetListener {
	final Csv5963 parent;

	DropTargetListener5963(Csv5963 parent) {
		this.parent = parent;
	}

	@Override
	public void dragEnter(DropTargetDragEvent dtde) {
		;	// 処理不要
	}

	@Override
	public void dragOver(DropTargetDragEvent dtde) {
		;	// 処理不要
	}

	@Override
	public void dropActionChanged(DropTargetDragEvent dtde) {
		;	// 処理不要
	}

	@Override
	public void dragExit(DropTargetEvent dte) {
		;	// 処理不要
	}

	@SuppressWarnings("unchecked")
	@Override
	public void drop(DropTargetDropEvent e) {
		e.acceptDrop(DnDConstants.ACTION_COPY);
		Transferable tr = e.getTransferable();

		if (e.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
			List<File> files;
			try {
				try {
					Object obj = tr.getTransferData(DataFlavor.javaFileListFlavor);
					files = (List<File>) obj;
				} catch (UnsupportedFlavorException | IOException ex1) {
					JOptionPane.showMessageDialog(parent, "Drag and Drop Failure: " + ex1.toString(), "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
			} catch (Exception ex2) {
				// キャストに失敗する場合は無いようにも思えるが、アノテーションではなく例外捕捉で書いている。
				JOptionPane.showMessageDialog(parent, "Drag and Drop Failure (Bug?): " + ex2.toString(), "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}

			parent.openCsvFromList(files);
		}
	}
}

/**
 * 選択タブの変更のリスナー
 */
class TabStateChangeListener implements ChangeListener {
	final Csv5963 parent;

	TabStateChangeListener(Csv5963 parent) {
		this.parent = parent;
	}

	/**
	 * JTabbed Pane の変更を受け取る
	 */
	@Override
	public void stateChanged(ChangeEvent e) {
		this.parent.updateTitleAndStatus();
		this.parent.updateMenubar();
	}
}

/**
 * 画面下部のステータスバー
 */
class StatusBar5963 extends JLabel {
	StatusBar5963() {
	}

	void setMessage(String text) {
		this.setText(text);
	}
}

/**
 * アプリ名: CSV5963 (ゴクローサン)
 *
 * 名前について
 * CSV123というのも良いかと思ったけれど、もう使っている人がいたので "CSV" + 適当な数字 で 5963 に。
 *
 */
public class Csv5963 extends JFrame {
	String defaultEncode;
	Csv.AnalyzePreference analyzePreference;

	final MenuBar5963 menuPanel;
	final JTabbedPane tabs;
	final StatusBar5963 statusBar;

	/**
	 * ファイルオープン用のファイル選択ダイアログ
	 */
	JFileChooser openFileChooser;

	Csv5963() {
		this.defaultEncode = Csv.encodeList[0];
		this.analyzePreference = Csv.AnalyzePreference.STRICT_ANALYZE;

		this.menuPanel = new MenuBar5963(this);
		this.tabs = new JTabbedPane();
		this.tabs.addChangeListener(new TabStateChangeListener(this));
		this.statusBar = new StatusBar5963();

		this.openFileChooser = new JFileChooser();
		Utils.setLookAndFeel(openFileChooser);


		// メニューバーを設定する
		this.setJMenuBar(menuPanel);

		// タブと、ステータスバーを設定する
		GridBagLayout layout = new GridBagLayout();
		this.setLayout(layout);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 1;
		gbc.weighty = 1;
		layout.setConstraints(tabs, gbc);
		this.add(tabs);

		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridy = 1;
		gbc.weighty = 0.01;
		layout.setConstraints(statusBar, gbc);
		this.add(statusBar);

		// 以降は JFrame の定型的な処理。

		// ウインドウを閉じた場合の動作 (プロセスを終了する) を設定する
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// ドラッグ&ドロップのリスナーを設定する
		new DropTarget(this,DnDConstants.ACTION_COPY_OR_MOVE, new DropTargetListener5963(this));
		// アイコンを設定する
		Utils.setIcon(this);
		// 外観を設定する
		Utils.setLookAndFeel(this);
	}

	/**
	 * ウィンドウタイトル、ステータスバーの表示内容を更新・設定します。
	 */
	public void updateTitleAndStatus() {
		String baseName = Utils.APP_NAME;
		CsvPanel csvPanel = this.getCsvPanel();
		if (csvPanel != null) {
			this.setTitle(baseName + " - " + csvPanel.getPath());
			this.statusBar.setMessage("(Default Encode: " + this.defaultEncode + ") "
					+ "  Encode:" + csvPanel.csv.getEncode()
					+ "  Rows:" + csvPanel.csv.getRecords()
					+ "  Cols:" + csvPanel.csv.getColumns());
		}
		else {
			this.setTitle(baseName);
			this.statusBar.setMessage("(Default Encode: " + this.defaultEncode + ")" );
		}
	}

	/**
	 * メニューバーの表示内容を更新・設定します。
	 *
	 * タブが存在しない状況で、操作上意味をなさなくなるものを disable にします。
	 */
	public void updateMenubar() {
		CsvPanel csvPanel = this.getCsvPanel();

		if (csvPanel != null) {
			this.menuPanel.reloadWithEncode.setEnabled(true);
			this.menuPanel.save.setEnabled(true);
			this.menuPanel.saveAs.setEnabled(true);
			this.menuPanel.close.setEnabled(true);

			this.menuPanel.updateReloadEncodeMenu(csvPanel.csv.getEncode());
			this.menuPanel.updateSaveEncodeMenu(csvPanel.csv.getEncode());

			this.menuPanel.convert.setEnabled(true);
		}
		else {
			this.menuPanel.reloadWithEncode.setEnabled(false);
			this.menuPanel.save.setEnabled(false);
			this.menuPanel.saveAs.setEnabled(false);
			this.menuPanel.close.setEnabled(false);
			this.menuPanel.convert.setEnabled(false);
		}
	}

	/**
	 * オープン済のCSVファイルの一覧を取得します。
	 *
	 * @return (絶対パス, タブ番号) のHashMap
	 */
	private HashMap<String,Integer> openedCsvFiles() {
		HashMap<String,Integer> result = new HashMap<String,Integer>();

		int n = this.tabs.getTabCount();
		for (int i = 0; i < n; i++) {
			Component component = this.tabs.getComponent(i);
			if (component instanceof CsvPanel) {
				CsvPanel csvPanel = (CsvPanel)component;
				result.put(csvPanel.getPath(), i);
			}
		}

		return result;
	}

	/**
	 * 1個のCSVファイルを読み込みます。
	 */
	public void openCsvFile(File file) {
		// 既に開いているファイルの場合は、再読み込みする。
		// ただし、再読み込みするかどうかは、コンファームダイアログによる確認を行う。
		HashMap<String,Integer> opend = openedCsvFiles();
		Integer index = opend.get(file.getAbsolutePath());
		if (index != null) {
			this.tabs.setSelectedIndex(index);
			this.updateTitleAndStatus();
			this.updateMenubar();

			int selected = JOptionPane.showConfirmDialog(this, "Already opend. Reload?",
				      "Confirm", JOptionPane.YES_NO_OPTION,
				      JOptionPane.WARNING_MESSAGE);
			if (selected == JOptionPane.YES_OPTION) {
				this.reload(index);
			}
			return;
		}

		// CSVファイルを読み込む。
		try {
			CsvToModel csv = new CsvToModel(this.defaultEncode, this.analyzePreference);
			csv.read(file.getAbsolutePath());
			CsvPanel csvPanel = new CsvPanel(this, csv);
			this.tabs.add(csvPanel.getNameForTab(), csvPanel);
			this.tabs.setSelectedIndex(this.tabs.getTabCount() - 1);
			this.updateTitleAndStatus();
			this.updateMenubar();
		} catch (IOException | CsvMalFormedException ex1) {
			JOptionPane.showMessageDialog(this, ex1.toString(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}


	/**
	 * 複数のCSVファイルを読み込みます。
	 *
	 * @param files CSVファイルのリスト
	 */
	public void openCsvFiles(List<File> files) {
		// 既に開いているファイルの場合、単に無視をする。
		HashMap<String,Integer> opend = openedCsvFiles();

		ArrayList<String> errors = new ArrayList<String>();
		for ( File file : files ) {
			Integer index = opend.get(file.getAbsolutePath());
			if (index != null) {
				continue;
			}

			try {
				CsvToModel csv = new CsvToModel(this.defaultEncode, this.analyzePreference);
				csv.read(file.getAbsolutePath());
				CsvPanel csvPanel = new CsvPanel(this, csv);
				this.tabs.add(csvPanel.getNameForTab(), csvPanel);
			} catch (IOException | CsvMalFormedException ex1) {
				errors.add(ex1.toString());
			}
		}
		if (errors.size() > 0) {
			JOptionPane.showMessageDialog(this, "There are " + errors.size() + " error(s). \n" + errors.get(0) + " ...", "Error", JOptionPane.ERROR_MESSAGE);
		}

		this.tabs.setSelectedIndex(this.tabs.getTabCount() - 1);
		this.updateTitleAndStatus();
		this.updateMenubar();
	}

	/**
	 * ファイル選択ダイアログを表示し、ダイアログで指定されたファイルを読み込みます。
	 */
	public void openCsvFileWithDialog() {
		int selected = openFileChooser.showOpenDialog(this);
		if (selected == JFileChooser.APPROVE_OPTION) {
			File file = openFileChooser.getSelectedFile();
			this.openCsvFile(file);
		}
	}

	/**
	 * 複数のCSVファイルを読み込みます。
	 *
	 * @param files CSVファイルのリスト
	 */
	public void openCsvFromList(List<File> files) {
		if (files.size() == 1) {
			// 1個の場合
			this.openCsvFile(files.get(0));
		}
		else {
			// 複数の場合
			this.openCsvFiles(files);
		}
	}

	/**
	 * エンコードを指定して再読み込みします。
	 *
	 * @param newEncode 再読み込みで指定するエンコード
	 */
	public void reloadWithEncode(String newEncode) {
		int componentIndex = this.tabs.getSelectedIndex();
		if (componentIndex == -1) {
			return;
		}
		Component component = this.tabs.getComponent(componentIndex);
		if (!(component instanceof CsvPanel)) {
			return;
		}
		CsvPanel oldCsvPanel = (CsvPanel)component;

		// 読み込みと差し替え
		String path = oldCsvPanel.getPath();
		try {
			CsvToModel csv = new CsvToModel(newEncode, this.analyzePreference);
			csv.read(path);
			CsvPanel csvPanel = new CsvPanel(this, csv);
			this.tabs.setComponentAt(componentIndex, csvPanel);
		} catch (IOException | CsvMalFormedException ex1) {
			JOptionPane.showMessageDialog(this, ex1.toString(), "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		this.updateTitleAndStatus();
		this.updateMenubar();
	}

	/**
	 * 指定されたインデックス番号のタブを再読み込みします。
	 *
	 * @param componentIndex 再読み込みするインデックス
	 */
	public void reload(int componentIndex) {
		if (componentIndex == -1) {
			return;
		}
		Component component = this.tabs.getComponent(componentIndex);
		if (!(component instanceof CsvPanel)) {
			return;
		}
		CsvPanel oldCsvPanel = (CsvPanel)component;

		// 読み込みと差し替え
		String path = oldCsvPanel.getPath();
		try {
			CsvToModel csv = new CsvToModel(oldCsvPanel.csv.getEncode(), this.analyzePreference);
			csv.read(path);
			CsvPanel csvPanel = new CsvPanel(this, csv);
			this.tabs.setComponentAt(componentIndex, csvPanel);
		} catch (IOException | CsvMalFormedException ex1) {
			JOptionPane.showMessageDialog(this, ex1.toString(), "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		this.updateTitleAndStatus();
		this.updateMenubar();
	}


	/**
	 * 現在のタブで選択されている CsvPanel オブジェクトを取得します。
	 *
	 * @return CsvPanelオブジェクト。タブがなければ null 。
	 */
	private CsvPanel getCsvPanel() {
		Component component = this.tabs.getSelectedComponent();
		if (component != null && component instanceof CsvPanel) {
			return (CsvPanel)component;
		}

		return null;
	}

	/**
	 * 上書き保存します。
	 */
	public void save() {
		CsvPanel csvPanel = this.getCsvPanel();
		if (csvPanel != null) {
			csvPanel.save();
		}
	}

	/**
	 * ダイアログを表示して保存先ファイルを指定し、保存します。
	 */
	public void saveWithDialog() {
		CsvPanel csvPanel = this.getCsvPanel();
		if (csvPanel != null) {
			JFileChooser saveAsFileChooser = new JFileChooser(new File(csvPanel.getPath()).getParent());
			Utils.setLookAndFeel(saveAsFileChooser);

			int selected = saveAsFileChooser.showSaveDialog(this);
			if (selected == JFileChooser.APPROVE_OPTION) {
				File file = saveAsFileChooser.getSelectedFile();
				String path = file.getAbsolutePath();

				csvPanel.save(path);

				this.tabs.setTitleAt(this.tabs.getSelectedIndex(), new File(csvPanel.getPath()).getName());
				this.updateTitleAndStatus();
				this.updateMenubar();
			}
		}
	}

	/**
	 * エンコードを指定してCSVファイルを保存します。
	 *
	 * @param newEncode 保存するエンコード
	 */
	public void saveWithEncode(String newEncode) {
		CsvPanel csvPanel = this.getCsvPanel();
		if (csvPanel != null) {
			csvPanel.csv.setEncode(newEncode);
			csvPanel.save();

			this.updateTitleAndStatus();
			this.updateMenubar();
		}
	}

	/**
	 * タブをクローズします。
	 */
	public void closeTab() {
		CsvPanel csvPanel = this.getCsvPanel();
		if (csvPanel != null) {
			this.tabs.remove(csvPanel);

			this.updateTitleAndStatus();
			this.updateMenubar();
		}
	}

	/**
	 * コマンドライン用の Usage を表示します。
	 */
	static void usage() {
		System.err.println("args(pattern 1): -help");
		System.err.println("args(pattern 2): [-encode <ENCODE>] [file1.csv] [file2.csv] [file3.csv] ...");
			System.err.println();
		System.err.println("-help: Print this help.");
		System.err.println("-encode: Specify encode.");
	}

	/**
	 * メイン
	 *
	 * @param arg
	 * @throws IOException
	 * @throws CsvMalFormedException
	 */
	static public void main(String arg[]) throws IOException, CsvMalFormedException {
		String encode = null;
		ArrayList<File> files = new ArrayList<File>();
		for (int i = 0; i < arg.length; i++) {
			String s = arg[i];
			if (s.equals("-help")) {
				Csv5963.usage();
				System.exit(0);
			}
			else if(s.endsWith("-encode")) {
				if (i + 1 < arg.length) {
					encode = arg[i + 1];
					i++;
				}
				else {
					Csv5963.usage();
					System.exit(1);
				}
			}
			else {
				files.add(new File(s));
			}
		}

		Csv5963 instance = new Csv5963();
		if (encode != null) {
			instance.defaultEncode = encode;
		}
		instance.setBounds(10, 10, 814, 612);
		instance.updateTitleAndStatus();
		instance.updateMenubar();
		instance.setVisible(true);

		instance.openCsvFromList(files);
	}
}