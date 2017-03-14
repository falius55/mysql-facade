package mysqlfacade;

/**
 * データベースの列を表すインタフェースです。
 */
public interface DatabaseColumn {
    public static final String TABLE_NAME_METHOD = "tableName";

    /**
     * その列の値のデータ型を文字列で返します。
     * <pre>
     * {@code
     * "varchr(256)"
     * "int"
     * "enum('male', 'female')"
     * "datetime"
     * }
     * </pre>
     */
    String type();

    /**
     * その列を作成する際に、CREATE TABLE文に渡す文字列を返します。<br>
     * 例：<br>
     * {@code
     * "name varchar(256) not null unique key"
     * }
     */
    String columnString();

    /**
     * @return カラム名
     */
    String toString();
}
