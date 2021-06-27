# Csv5963
Analyze CSV File.
And sample GUI Application(Java Swing).

CSVファイルを解析する処理です。
あと、サンプル的な GUI アプリケーションを作成しています。

## Usage

### Class
Sample:
```
Csv csv = new Csv("UTF-8", Csv.AnalyzePreference.STRICT_ANALYZE);
csv.read(filename);
ArrayList<ArrayList<String>> a = csv.getData();
```

### Sample Application
```
java com.github.novisoftware.app.Csv5963
```
or
```
java -jar csv5963.jar
```

以下、日本語です。

## 概要
CSVファイルを解析する処理です。
あと、サンプル的な GUI アプリケーションを作成しています。

## 使い方(プログラムからの呼び出し方法)

Sample:
```
Csv csv = new Csv("UTF-8", Csv.AnalyzePreference.STRICT_ANALYZE);
csv.read(filename);
ArrayList<ArrayList<String>> a = csv.getData();
```

## 使い方(サンプルプログラム)

```
java com.github.novisoftware.app.Csv5963
```
または
```
java -jar csv5963.jar
```

## 処理の説明

### そもそもCSVについて
表計算ソフト Microsoft Excel 等が有名にしたフォーマットですが、2005年になって RFC4180 がまとめられています。

- 邦訳: http://www.kasai.fm/wiki/rfc4180jp
- 原文: http://www.ietf.org/rfc/rfc4180.txt

CSVというフォーマットは、 [[チョムスキー階層 ]]( https://ja.wikipedia.org/wiki/%E3%83%81%E3%83%A7%E3%83%A0%E3%82%B9%E3%82%AD%E3%83%BC%E9%9A%8E%E5%B1%A4 ) で一番 Easy な [[正規言語]](https://ja.wikipedia.org/wiki/%E6%AD%A3%E8%A6%8F%E8%A8%80%E8%AA%9E) に属しています。
RFC4180 では [[バッカス・ナウア記法]](https://ja.wikipedia.org/wiki/%E3%83%90%E3%83%83%E3%82%AB%E3%82%B9%E3%83%BB%E3%83%8A%E3%82%A6%E3%82%A2%E8%A8%98%E6%B3%95) の一種 ABNF ( https://ja.wikipedia.org/wiki/ABNF ) で定義されます。

バッカス・ナウア記法自体は、[[文脈自由文法]](https://ja.wikipedia.org/wiki/%E3%83%90%E3%83%83%E3%82%AB%E3%82%B9%E3%83%BB%E3%83%8A%E3%82%A6%E3%82%A2%E8%A8%98%E6%B3%95)を定義するに用いられます。

### CSV の ABNF

RFC4180 の記載を抜粋すると以下です。

```
 file = [header CRLF] record *(CRLF record) [CRLF]
 header = name *(COMMA name)
 record = field *(COMMA field)
 name = field
 field = (escaped / non-escaped)
 escaped = DQUOTE *(TEXTDATA / COMMA / CR / LF / 2DQUOTE) DQUOTE
 non-escaped = *TEXTDATA
 COMMA = %x2C
 CR = %x0D ;as per section 6.1 of RFC 2234 [2]
 DQUOTE =  %x22 ;as per section 6.1 of RFC 2234 [2]
 LF = %x0A ;as per section 6.1 of RFC 2234 [2]
 CRLF = CR LF ;as per section 6.1 of RFC 2234 [2]
 TEXTDATA =  %x20-21 / %x23-2B / %x2D-7E
```

- header というのがあるのですが、これは無視します。
- TEXTDATA は、 ASCII のみが対象になりますが、 ここでは 改行、カンマ、ダブルクォート以外をテキストとします。
- 改行は、 Java の処理系が改行と判定したものを改行とします。

### CSVを受理する有限オートマトン

BNFを踏まえて、CSVを受理する有限オートマトンを考えます。 ごく状態数の少ない決定性有限オートマトン ( https://ja.wikipedia.org/wiki/%E6%B1%BA%E5%AE%9A%E6%80%A7%E6%9C%89%E9%99%90%E3%82%AA%E3%83%BC%E3%83%88%E3%83%9E%E3%83%88%E3%83%B3 )になります。

![図1: CSVを受理する有限オートマトン](/doc/image/state.png) 

あとは、やってきた入力に沿って状態変数が書き換わるような単純なプログラム( https://github.com/novisoftware/Csv5963/blob/main/src/com/github/novisoftware/csv/Csv.java )を書けばいいです。

### いい加減なファイルも読み取る
CSVファイルというのは、極端には「カンマで区切られていれば、なんでも」該当してしまう・名乗れてしまうし、 RFC4180 に準拠するとは限らないので、
世の中には、様々なCSVファイルがあるようで、なんか少し変なCSVファイルもあり、多少はいい加減なものも読み取るよう、
エラーとなる場所で適当に動作するように動作を切り替えることができるようにしています( Csv.analyzePreference )。

クラス Csv (com.github.novisoftware.csv) のコンストラクタの第二引数で指定するか、 setAnalyzePreference() で切り替えます。

## GUI の アプリケーションについて
CSV を 表示したり、書き換えたり保存したりするようなサンプルプログラムを Swing で作成してみました。
画面イメージです。

![図2: CSVファイルを表示したりするサンプルプログラム](/doc/image/app.png) 

引数無しで起動して、適当にファイルをドラッグ&ドロップしてみてください。

「国勢調査 時系列データ CSV形式による主要時系列データ」( https://www.e-stat.go.jp/stat-search/files?page=1&layout=datalist&toukei=00200521&tstat=000001011777&cycle=0&tclass1=000001094741 )を表示しています。

