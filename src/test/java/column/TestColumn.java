package test.column;

import java.util.Map;
import java.util.HashMap;

import mysqlfacade.DatabaseColumn;

public enum TestColumn implements DatabaseColumn {
    ID("id", "int", "not null primary key auto_increment"),
    NAME("name", "varchar(255)", "not null unique key"),
    PASSWORD("password", "varchar(32)", "not null"),
    SCORE("score", "int", "not null default 0"),
    SEX("sex", "enum('male', 'female')", "default 'male'"),
    SAVED("saved", "datetime", "");

    private static Map<String, TestColumn> stringToEnum = new HashMap<>();

    static {
        for (TestColumn column : values()) {
            stringToEnum.put(column.toString(), column);
        }
    }

    public static TestColumn fromString(String name) {
        return stringToEnum.get(name);
    }

    public static String tableName() {
        return "test_table";
    }

    private final String mName;
    private final String mType;
    private final String mOption;

    TestColumn(String name, String type, String option) {
        mName = name;
        mType = type;
        mOption = option;
    }

    @Override
    public String toString() {
        return mName;
    }

    @Override
    public String type() {
        return mType;
    }

    @Override
    public String columnString() {
        return String.join(" ", mName, mType, mOption);
    }
}
