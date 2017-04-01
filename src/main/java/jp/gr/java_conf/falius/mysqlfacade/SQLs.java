package jp.gr.java_conf.falius.mysqlfacade;

import java.sql.Timestamp;
import java.util.Map;

public class SQLs {

    private SQLs() {}
    /**
     *
     * @param whereClause 条件節。nullか空文字を渡すと条件なしになる。
     */
    public static String createSelectSql(String table, String[] columns, String whereClause) {
        // SELECT col1,col2,col3 FROM table
        // SELECT col1,col2,col3 FROM table WHERE whereClause
        StringBuilder sql = new StringBuilder("SELECT ");

        for (int i = 0; i < columns.length; i++) {
            if (i != 0) {
                sql.append(",");
            }
            sql.append(columns[i]);
        }

        sql.append(" FROM ").append(table);
        if (whereClause != null && whereClause.length() > 0) {
            sql.append(" WHERE ").append(whereClause);
        }
        return sql.toString();
    }

    public static <T extends DatabaseColumn> String createSelectSql(String table, T[] columns, String whereClause) {
        String[] strings = new String[columns.length];
        for (int i = 0; i < columns.length; i++) {
            strings[i] = columns[i].toString();
        }
        return createSelectSql(table, strings, whereClause);
    }

    public static String createSelectFuncSql(String funcName, String table, String column, String whereClause) {
    // SELECT funcName(col) FROM table
    // SELECT funcName(col) FROM table WHERE whereClause
        StringBuilder sql = new StringBuilder("SELECT ");
        sql.append(funcName)
            .append("(").append(column)
            .append(") FROM ").append(table);
        if (whereClause != null && whereClause.length() > 0) {
            sql.append(" WHERE ").append(whereClause);
        }
        return sql.toString();
    }

    public static String createUpdateSql(String table, Map<? extends DatabaseColumn, ?> values, String whereClause) {
        // UPDATE table SET key = Object, key = Object, key = Object
        // UPDATE table SET key = Object, key = Object, key = Object WHERE whereClause
        StringBuilder sql = new StringBuilder("UPDATE ")
            .append(table)
            .append(" SET ");

        int cnt = 0;
        for (Map.Entry<? extends DatabaseColumn, ?> mapEntry : values.entrySet()) {
            if (cnt++ != 0) {
                sql.append(",");
            }

            DatabaseColumn column = mapEntry.getKey();
            Object value = mapEntry.getValue();

            sql.append(column.toString());
            sql.append(" = ");
            if (value instanceof String) {
                sql.append("'").append(value.toString()).append("'");
            } else {
                sql.append(value.toString());
            }
        }

        if (whereClause != null && whereClause.length() > 0) {
            sql.append(" WHERE ").append(whereClause);
        }
        return sql.toString();
    }

    public static String createInsertSql(String table, Map<? extends DatabaseColumn, ?> values) {
        // INSERT INTO table (col1,col2,col3) VALUES (val1,val2,val3)
        StringBuilder sql = new StringBuilder("INSERT INTO ")
            .append(table)
            .append(" (");
        int cnt = 0;
        for (DatabaseColumn column : values.keySet()) {
            if (cnt++ != 0) {
                sql.append(",");
            }
            sql.append(column.toString()); }
        sql.append(") VALUES (");

        cnt = 0;
        for (Object value : values.values()) {
            if (cnt++ != 0) {
                sql.append(",");
            }

            if (value instanceof String) {
                sql.append("'").append(value.toString()).append("'");
            } else {
                sql.append(value.toString());
            }
        }
        sql.append(")");
        return sql.toString();
    }

    public static String createDeleteSql(String table, String whereClause) {
        // DELETE FROM table WHERE
        // DELETE FROM table WHERE whereClause
        StringBuilder sql
            = new StringBuilder("DELETE FROM ")
            .append(table);

        if (whereClause != null && whereClause.length() > 0) {
            sql.append(" WHERE ").append(whereClause);
        }
        return sql.toString();
    }

    public static String createCreateTableSql(String table, DatabaseColumn[] columns) {
        // CREATE TABLE table {colStr1,colStr2,colStr3}
        StringBuilder sql = new StringBuilder("CREATE TABLE ").append(table)
            .append(" (");
        int cnt = 0;
        for (DatabaseColumn column : columns) {
            if (cnt++ != 0) {
                sql.append(",");
            }
            sql.append(column.columnString());
        }
        sql.append(")");
        return sql.toString();
    }

    public static String formatString(Timestamp timestamp, String format) {
        long millis = timestamp.getTime();
        return formatString(millis, format);
    }

    public static String formatString(long millis, String format) {
        java.util.Date date = new java.util.Date(millis); // java.sql.Date()の場合、時分秒が切り捨てられてしまうので、java.util.Date()を使う必要がある
        return new java.text.SimpleDateFormat(format).format(date);
    }

    /**
     * <p>
     * 更新する値などにSelect文を埋め込むなど式で表現したい場合に使用します。
     * 文字列で表した式を直接各値に指定すると'(シングルくオーテーション)で囲まれてしまうため、
     * このクラスを利用して式を表現します。
     *
     * <p>
     * プレイスホルダーには対応していません。
     * 条件節に式を使いたい場合には、プレイスホルダーを使わずに直接条件節に組み込んでください。
     *
     */
//    public static class Expression {
//        private final String mExpression;
//
//        public Expression(String expression) {
//            mExpression = "(" + expression + ")";
//        }
//
//        @Override
//        public String toString() {
//            return mExpression;
//        }
//    }
}
