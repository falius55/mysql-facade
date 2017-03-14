package mysqlfacade;

public interface DatabaseColumn {
    public String tableNameMethod = "tableName";

    String type();

    String columnString();

    /**
     * @return カラム名
     */
    String toString();
}
