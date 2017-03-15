package jp.gr.java_conf.falius.mysqlfacade;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * {@inheritDoc}
 */
public class PreparedDatabase implements SQLDatabase {
    private static final boolean USE_SSL = false;
    private static final boolean USE_LEGACY_DATETIME_CODE = false;
    private static final String SERVER_TIME_ZONE = "JST";
    private final String mDBName;
    private final Deque<Entry> mEntries = new ArrayDeque<Entry>();
    private final Connection mConnection;

    /**
     * @param dbName 接続するデータベース名
     * @param user ユーザー名
     * @param password パスワード
     * @throws SQLException データベースに接続できなかった場合、ドライバをインスタンス化できなかった場合、データベースにアクセスできなかった場合
     */
    public PreparedDatabase(String dbName, String user, String password) throws SQLException {
        mDBName = dbName;
        try {
            // JDBCドライバをロードする
            Class.forName("com.mysql.cj.jdbc.Driver").newInstance();

            // Drivermanagerに接続(データベースへの接続)
            String url = String.format("jdbc:mysql://localhost/%s?"
                    + "useSSL=%b&useLegacyDatetimeCode=%b&serverTimezone=%s",
                    dbName, USE_SSL, USE_LEGACY_DATETIME_CODE, SERVER_TIME_ZONE);
            mConnection = DriverManager.getConnection(url, user, password);

        } catch (ClassNotFoundException | SQLException | InstantiationException | IllegalAccessException e) {
            throw new SQLException("database failed connect", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends DatabaseColumn> ResultSet select(Class<?> table, T[] columns, String whereClause,
            Object... whereArgs) throws SQLException {
        String tableName = tableName(table);
        String sql = SQLs.createSelectSql(tableName, columns, whereClause);

        Entry entry = execute(sql);
        setArgs(entry, whereArgs);
        return entry.query();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResultSet selectAllColumns(Class<?> table, String whereClause, Object... whereArgs) throws SQLException {
        String tableName = tableName(table);
        String sql = SQLs.createSelectSql(tableName, new String[] { "*" }, whereClause);

        Entry entry = execute(sql);
        setArgs(entry, whereArgs);
        return entry.query();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResultSet selectAllColumns(Class<?> table, DatabaseColumn whereColumn, Object whereArg) throws SQLException {
        return selectAllColumns(table, whereColumn.toString() + "=?", whereArg);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResultSet selectAll(Class<?> table) throws SQLException {
        return selectAllColumns(table, "");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int update(Class<?> table, Map<? extends DatabaseColumn, ?> values, String whereClause, Object... whereArgs)
            throws SQLException {
        String tableName = tableName(table);
        String sql = SQLs.createUpdateSql(tableName, values, whereClause);

        Entry entry = execute(sql);
        setArgs(entry, whereArgs);
        return entry.update();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int update(Class<?> table, Map<? extends DatabaseColumn, ?> values, DatabaseColumn whereColumn,
            Object whereArg) throws SQLException {
        return update(table, values, whereColumn.toString() + "=?", whereArg);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long insert(Class<?> table, Map<? extends DatabaseColumn, ?> values) throws SQLException {
        String tableName = tableName(table);
        String sql = SQLs.createInsertSql(tableName, values);
        Entry entry = execute(sql.toString());
        int result = entry.update();
        if (result == 0) {
            return -1;
        }

        ResultSet rs = entry.getGeneratedKeys();
        if (rs.next()) {
            return rs.getLong(1);
        }
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int delete(Class<?> table, String whereClause, Object... whereArgs) throws SQLException {
        String tableName = tableName(table);
        String sql = SQLs.createDeleteSql(tableName, whereClause);

        Entry entry = execute(sql);
        setArgs(entry, whereArgs);
        return entry.update();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int delete(Class<?> table, DatabaseColumn whereColumn, Object whereArg) throws SQLException {
        return delete(table, whereColumn.toString() + "=?", whereArg);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Enum<T> & DatabaseColumn> void create(Class<T> table) throws SQLException {
        String tableName = tableName(table);
        String sql = SQLs.createCreateTableSql(tableName, table.getEnumConstants());
        execute(sql).update();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int empty(Class<?> table) throws SQLException {
        return delete(table, "");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void drop(Class<?> table) throws SQLException {
        String tableName = tableName(table);
        execute("DROP TABLE " + tableName).update();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isExistTable(Class<?> table) throws SQLException {
        String tableName = tableName(table);
        return execute(String.format("show tables where Tables_in_%s like ?", mDBName))
                .setString(tableName).query().next();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isExistRecord(Class<?> table, String whereClause, Object... whereArgs) throws SQLException {
        ResultSet rs = selectAllColumns(table, whereClause, whereArgs);
        return rs.next();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isExistRecord(Class<?> table, DatabaseColumn whereColumn, Object whereArg) throws SQLException {
        return isExistRecord(table, whereColumn.toString() + "=?", whereArg);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Entry execute(String sql) throws SQLException {
        Entry entry = new PreparedEntry(mConnection, sql);
        mEntries.add(entry);
        return entry;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws SQLException {
        if (mConnection != null) {
            mConnection.close();
        }
        clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() throws SQLException {
        int size = mEntries.size();
        for (int i = 0; i < size; i++) {
            Entry entry = mEntries.pop();
            entry.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int sum(Class<?> table, DatabaseColumn column) throws SQLException {
        return sum(table, column, "");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int sum(Class<?> table, DatabaseColumn column, String whereClause, Object... whereArgs) throws SQLException {
        return execIntFunc("sum", table, column.toString(), whereClause, whereArgs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int max(Class<?> table, DatabaseColumn column) throws SQLException {
        return execIntFunc("max", table, column.toString(), "");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int max(Class<?> table, DatabaseColumn column, String whereClause, Object... whereArgs) throws SQLException {
        return execIntFunc("max", table, column.toString(), whereClause, whereArgs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int min(Class<?> table, DatabaseColumn column) throws SQLException {
        return execIntFunc("min", table, column.toString(), "");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int min(Class<?> table, DatabaseColumn column, String whereClause, Object... whereArgs) throws SQLException {
        return execIntFunc("min", table, column.toString(), whereClause, whereArgs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int count(Class<?> table) throws SQLException {
        return execIntFunc("count", table, "*", "");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int count(Class<?> table, DatabaseColumn column) throws SQLException {
        return count(table, column, "");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int count(Class<?> table, DatabaseColumn column, String whereClause, Object... whereArgs)
            throws SQLException {
        return execIntFunc("count", table, column.toString(), whereClause, whereArgs);
    }

    private int execIntFunc(String funcName, Class<?> table, String column, String whereClause, Object... whereArgs)
            throws SQLException {
        String tableName = tableName(table);
        String sql = SQLs.createSelectFuncSql(funcName, tableName, column, whereClause);

        Entry entry = execute(sql);
        setArgs(entry, whereArgs);
        ResultSet resultSet = entry.query();
        if (resultSet.next()) {
            return resultSet.getInt(1);
        } else {
            throw new NoSuchElementException();
        }
    }

    private void setArgs(Entry entry, Object... whereArgs) throws SQLException {
        for (Object arg : whereArgs) {
            setArg(entry, arg);
        }
    }

    private void setArg(Entry entry, Object whereArg) throws SQLException {
        if (whereArg instanceof Integer) {
            entry.setInt((Integer) whereArg);
        } else if (whereArg instanceof String) {
            entry.setString((String) whereArg);
        } else if (whereArg instanceof Long) {
            entry.setLong((Long) whereArg);
        } else if (whereArg instanceof Float) {
            entry.setFloat((Float) whereArg);
        } else if (whereArg instanceof Double) {
            entry.setDouble((Double) whereArg);
        } else {
            throw new IllegalArgumentException();
        }
    }

    private String tableName(Class<?> table) {
        try {
            Method method = table.getMethod(DatabaseColumn.TABLE_NAME_METHOD);
            return (String) method.invoke(null);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException("not found static tableName method\n"
                    + "require public 'static' String " + DatabaseColumn.TABLE_NAME_METHOD + "() in "
                    + table.getName());
        }
    }

    /**
     *	ミリ秒を"yyyy-MM-dd HH:mm:ss"のフォーマットに変換します
     *	@param millis 変換するミリ秒の値
     *	@return フォーマットされた文字列
     */
    private static String dateFormat(long millis) {
        java.util.Date date = new java.util.Date(millis); // java.sql.Date()の場合、時分秒が切り捨てられてしまうので、java.util.Date()を使う必要がある
        String saved = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
        return saved;
    }

    /**
     * データベースへの各問い合わせを担当するクラス
     */
    public static class PreparedEntry implements Entry {
        private final PreparedStatement mPreparedStatement;
        private int mIndexCounter = 0;

        /**
         * @param sql SQL文
         * @throws SQLException 生成失敗
         */
        private PreparedEntry(Connection connect, String sql) throws SQLException {
            try {
                mIndexCounter = 0;
                mPreparedStatement = connect.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            } catch (SQLException e) {
                throw new SQLException("fail new Entry", e);
            }
        }

        /**
         *	{@inheritDoc}
         */
        @Override
        public ResultSet query() throws SQLException {
            return mPreparedStatement.executeQuery();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int update() throws SQLException {
            return mPreparedStatement.executeUpdate();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ResultSet getGeneratedKeys() throws SQLException {
            return mPreparedStatement.getGeneratedKeys();
        }

        /**
         * 終了処理を行います
         * PreparedDatabaseのcloseによって自動的に実行されるため、必ずしもEntryインスタンスにおいて実行する必要はありません
         * すでにクローズされた状態でcloseメソッドを呼び出すと、操作は行われません
         * @throws SQLException データベースアクセスエラーが発生した場合
         */
        @Override
        public void close() throws SQLException {
            mPreparedStatement.close();
            // Statementをcloseすると、ResultSetも自動的にcloseされる
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Entry setInt(int x) throws SQLException {
            mIndexCounter++;
            mPreparedStatement.setInt(mIndexCounter, x);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Entry setString(String x) throws SQLException {
            mIndexCounter++;
            mPreparedStatement.setString(mIndexCounter, x);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Entry setDouble(double x) throws SQLException {
            mIndexCounter++;
            mPreparedStatement.setDouble(mIndexCounter, x);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Entry setFloat(float x) throws SQLException {
            mIndexCounter++;
            mPreparedStatement.setFloat(mIndexCounter, x);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Entry setLong(long x) throws SQLException {
            mIndexCounter++;
            mPreparedStatement.setLong(mIndexCounter, x);
            return this;
        }
    }
}
