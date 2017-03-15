package jp.gr.java_conf.falius.mysqlfacade;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 *
 * <p>
 * テーブル名を指定するにはtableNameメソッドを持つクラスのクラスオブジェクトを使用します。<br>
 * あるクラスがテーブルを表すにはそのstaticメソッドtableName()がテーブル名をStringで返す必要があります。<br>
 * DatabaseColumn実装クラスにtableNameメソッドを持たせてテーブルを表すという形を想定していますが、必ずしもDatabaseColumnがテーブル名を表す必要はありません。<br>
 * もしこのメソッドを持っていないクラスのクラスオブジェクトが渡された場合、IllegalArgumentExceptionが投げられます。
 *
 * <p>
 * テーブルを指定するクラスが持つ必要のあるメソッドのシグニチャ<br>
 * {@code
 * public static String tableName();
 * }
 *
 * <p>
 * テーブルの構成は{@link DatabaseColumn}インタフェースを実装した列挙型のクラスを使用します。<br>
 * テーブル名もこのクラスで表すのが良いでしょう。<br>
 */
public interface SQLDatabase extends AutoCloseable {

    /**
     * SQLのSelect文を実行します。
     * @param table static変数tableNameにテーブル名を保持しているクラス
     * @param columns 選択するカラム
     * @param whereClause 条件節
     * @param whereArgs 条件節に?が含まれていれば、埋め込む値。数値はラッパークラスで渡してください。
     */
    <T extends DatabaseColumn> ResultSet select(
            Class<?> table, T[] columns, String whereClause, Object... whereArgs) throws SQLException;

    /**
     * 条件に合致した行のすべての列を取得します。
     */
    ResultSet selectAllColumns(Class<?> table, String whereClause, Object... whereArgs) throws SQLException;

    /**
     * whereColumnの値がwhereArgである行のすべての列を取得します。
     */
    ResultSet selectAllColumns(Class<?> table, DatabaseColumn whereColumn, Object whereArg) throws SQLException;

    /**
     * すべての行のすべての列を取得します。
     */
    ResultSet selectAll(Class<?> table) throws SQLException;

    /**
     * 条件に合致した行を更新します。
     * @param table static変数tableNameにテーブル名を保持しているクラス
     * @param values 更新列からその新しい値へのマップ。値がString型であれば''(シングルくオーテーション)で囲み、そうでなければtoString()の戻り値がそのままSQL文に埋め込まれます。
     * @param whereClause 条件節
     * @param whereArgs 条件節に?が含まれていれば、埋め込む値。数値はラッパークラスで渡してください。
     */
    int update(Class<?> table, Map<? extends DatabaseColumn, ?> values, String whereClause, Object... whereArgs)
        throws SQLException;

    int update(Class<?> table, Map<? extends DatabaseColumn, ?> values, DatabaseColumn whereColumn, Object whereArg) throws SQLException;

    /**
     * 新しいレコードを作成します。
     * @param values カラムからその値へのマップ
     * @return 最後に挿入したカラムのID。なければ-1
     */
    long insert( Class<?> table, Map<? extends DatabaseColumn, ?> values) throws SQLException;

    /**
     * 条件に合致したレコードを削除します。
     */
    int delete(Class<?> table, String whereClause, Object... whereArgs) throws SQLException;

    /**
     * whereColumnの値がwhereArgである行をすべて削除します。
     */
    int delete(Class<?> table, DatabaseColumn whereColumn, Object whereArg) throws SQLException;

    /**
     * 渡された列挙型クラスが表わす内容でテーブルを作成します。
     */
    <T extends Enum<T> & DatabaseColumn> void create(Class<T> table) throws SQLException;

    /**
     * 指定されたテーブルのすべてのレコードを削除します。
     */
    int empty(Class<?> table) throws SQLException;

    /**
     * 指定されたテーブルを削除します。
     */
    void drop(Class<?> table) throws SQLException;

    /**
     * テーブルが存在するかどうか。
     */
    boolean isExistTable(Class<?> table) throws SQLException;

    boolean isExistRecord(Class<?> table, String whereClause, Object... whereArgs) throws SQLException;

    boolean isExistRecord(Class<?> table, DatabaseColumn whereColumn, Object whereArg) throws SQLException;

    /**
     * SQL文の実行準備をします。
     */
    Entry execute(String sql) throws SQLException;

    @Override
    void close() throws SQLException;

    /**
     * 内部に保持されているEntryオブジェクトすべてに終了処理を施し、Entryスタックを空にします。
     */
    void clear() throws SQLException;

    /**
     * 指定した列の値を合計した値を返します。
     */
    int sum(Class<?> table, DatabaseColumn column) throws SQLException;

    int sum(Class<?> table, DatabaseColumn column, String whereClause, Object... whereArgs) throws SQLException;

    int max(Class<?> table, DatabaseColumn column) throws SQLException;

    int max(Class<?> table, DatabaseColumn column, String whereClause, Object... whereArgs) throws SQLException;

    int min(Class<?> table, DatabaseColumn column) throws SQLException;

    int min(Class<?> table, DatabaseColumn column, String whereClause, Object... whereArgs) throws SQLException;

    /**
     * 指定されたテーブルの行数を返します。
     */
    int count(Class<?> table) throws SQLException;

    int count(Class<?> table, DatabaseColumn column) throws SQLException;

    int count(Class<?> table, DatabaseColumn column, String whereClause, Object... whereArgs) throws SQLException;

	/**
	 * データベースへの各問い合わせを担当するクラスのインタフェース
	 */
	interface Entry {

		/**
		 *	SQL文の問い合わせを実行します
		 *	@return 自身のインスタンス
         *	@throws SQLException 問い合わせに失敗した場合
		 */
		ResultSet query() throws SQLException;

		/**
		 *	データベースへの更新を実行します
		 *	@return	正常に処理が終了した行数
         *	@throws SQLException 更新に失敗した場合
		 */
		int update() throws SQLException;

        ResultSet getGeneratedKeys() throws SQLException;

		/**
		 * 終了処理を行います
         * @throws SQLException データベースアクセスエラーが発生した場合
		 */
		void close() throws SQLException;

		/**
		 *	SQL文のクエスチョンマークにint値をセットします
		 *	@param x セットする整数
		 *	@return 自らのインスタンス
         *	@throws SQLException setした回数がパラメータマーカーに対応しない場合、データベースアクセスエラーが発生した場合、またはクローズしたあとで実行された場合
		 */
		Entry setInt(int x) throws SQLException;

		/**
		 *	SQL文のクエスチョンマークに文字列をセットします
		 *	@param x セットする文字列
		 *	@return 自らのインスタンス
         *	@throws SQLException setした回数がパラメータマーカーに対応しない場合、データベースアクセスエラーが発生した場合、またはクローズしたあとで実行された場合
		 */
		Entry setString(String x) throws SQLException;

		/**
		 *	SQL文のクエスチョンマークに、double値をセットします
		 *	@param	x	セットするdouble値
		 *	@return 自らのインスタンス
         *	@throws SQLException setした回数がパラメータマーカーに対応しない場合、データベースアクセスエラーが発生した場合、またはクローズしたあとで実行された場合
		 */
		Entry setDouble(double x) throws SQLException;

		/**
		 *	SQL文のクエスチョンマークにfloat値をセットします
		 *	@param x セットするfloat値
		 *	@return 自らのインスタンス
         *	@throws SQLException setした回数がパラメータマーカーに対応しない場合、データベースアクセスエラーが発生した場合、またはクローズしたあとで実行された場合
		 */
		Entry setFloat(float x) throws SQLException;

		/**
		 *	SQL文のクエスチョンマークにlong値をセットします
		 *	@param x セットするlong値
		 *	@return 自らのインスタンス
         *	@throws SQLException setした回数がパラメータマーカーに対応しない場合、データベースアクセスエラーが発生した場合、またはクローズしたあとで実行された場合
		 */
		Entry setLong(long x) throws SQLException;
	}
}
