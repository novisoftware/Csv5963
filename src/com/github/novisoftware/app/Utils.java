package com.github.novisoftware.app;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Utils {
	/**
	 * ウィンドウタイトルに表示するアプリケーション名
	 */
	static public final String APP_NAME = "CSV 5963";

	/**
	 * 外観の設定
	 */
	static void setLookAndFeel(Component target) {
		try {
			String lookAndFeel = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";

			UIManager.setLookAndFeel(lookAndFeel);
			SwingUtilities.updateComponentTreeUI(target);
		} catch (Exception e) {
			;	// 処理不要
		}
	}

	static final String ICON_RESOURCE_PATH = "/com/github/novisoftware/etc/icon.png";

	/**
	 * フレームにアイコンを設定する
	 *
	 * @param target 対象のフレーム
	 */
	static void setIcon(JFrame target) {
		try {
			target.setIconImage(ImageIO.read(target.getClass().getResource(ICON_RESOURCE_PATH)));
		} catch (IOException | java.lang.IllegalArgumentException e) {
			; // 無視でよい
		}
	}

	/**
	 * マーク有無を指定し、メニュー横に表示するアイコンを取得する。
	 *
	 * @param marked
	 * @return
	 */
	public static ImageIcon IconMark(boolean marked) {
		return marked ? Utils.iconChecked : Utils.iconUnchecked;
	}

	protected static final ImageIcon iconChecked = createCheckedMark(true);
	private static final ImageIcon iconUnchecked = createCheckedMark(false);

	/**
	 * アイコン(単なる丸印)を描画する
	 *
	 * @param marked  丸の有無
	 * @return
	 */
	private static ImageIcon createCheckedMark(boolean marked) {
		final int h = 16;
		// final int h = 22;
		final int w = 20;
		final int r = 5;
		// final int r = 13;

		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g2 = image.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		if (marked) {
			// デバッグ時に使用。
			// g2.setColor(Color.WHITE);
			// g2.fillRect(0, 0,  w-1,  h-1);
			g2.setColor(Color.BLACK);
			g2.fillOval(w/2 - r, h/2 -r, r*2, r*2);
		}

		return new ImageIcon( image );
	}

	/**
	 * 作成中の動作確認用。
	 *
	 * @param msg
	 */
	static public void debug(String msg) {
		System.out.println(msg);
	}
}