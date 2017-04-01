import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import column.FailedColumn;
import column.TestColumn;
import jp.gr.java_conf.falius.mysqlfacade.DatabaseColumn;
import jp.gr.java_conf.falius.mysqlfacade.PreparedDatabase;
import jp.gr.java_conf.falius.mysqlfacade.SQLDatabase;
import jp.gr.java_conf.falius.mysqlfacade.SQLs;

public class DatabaseTest {
    private static Properties mProperties;
    private SQLDatabase mDB = null;

    @BeforeClass
    public static void setupProp() throws IOException {
        mProperties = new Properties();
        String propertiesPath = "build/resources/test/test.properties";
        try (InputStream is = new FileInputStream(propertiesPath)) {
            mProperties.load(is);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void failedDB() throws SQLException {
        String dbName = mProperties.getProperty("dbtest-database-name");
        String user = mProperties.getProperty("dbtest-database-user");
        String pass = mProperties.getProperty("dbtest-database-password");

        SQLDatabase db = new PreparedDatabase(dbName, user, pass);
        // FailedColumnにはstaticなtableNameメソッドを定義していないためIllegalArgumentException
        db.create(FailedColumn.class);
        db.close();
    }

    private void setupDB() throws SQLException {
        String dbName = mProperties.getProperty("dbtest-database-name");
        String user = mProperties.getProperty("dbtest-database-user");
        String pass = mProperties.getProperty("dbtest-database-password");

        mDB = new PreparedDatabase(dbName, user, pass);
        mDB.create(TestColumn.class);
        assertTrue(mDB.isExistTable(TestColumn.class));
    }

    private void closeDB() throws SQLException {
        mDB.drop(TestColumn.class);
        assertFalse(mDB.isExistTable(TestColumn.class));
        mDB.close();
        mDB = null;
    }

    @After
    public void after() throws SQLException {
        if (mDB != null && mDB.isExistTable(TestColumn.class)) {
            closeDB();
        }
    }

    private void insertDB() throws SQLException {
        Map<TestColumn, Object> values = new EnumMap<>(TestColumn.class);
        values.put(TestColumn.NAME, "name1");
        values.put(TestColumn.PASSWORD, "pass");
        values.put(TestColumn.SCORE, 80);
        values.put(TestColumn.SAVED, "2017-2-21 17:07:42");
        long id1 = mDB.insert(TestColumn.class, values);
        assertThat(id1, is(1L));

        Map<TestColumn, Object> values2 = new EnumMap<>(TestColumn.class);
        values2.put(TestColumn.NAME, "name2");
        values2.put(TestColumn.PASSWORD, "pass2");
        values2.put(TestColumn.SCORE, 45);
        values2.put(TestColumn.SEX, "female");
        values2.put(TestColumn.SAVED, "2014-12-1 14:16:2");
        long id2 = mDB.insert(TestColumn.class, values2);
        assertThat(id2, is(2L));

        assertThat(mDB.count(TestColumn.class), is(2));
    }

    private void checkSelectAll() throws SQLException {
        ResultSet rs = mDB.selectAll(TestColumn.class);
        assertTrue(rs.next());
        assertThat(rs.getString(TestColumn.NAME.toString()), is("name1"));
        assertThat(rs.getString(TestColumn.PASSWORD.toString()), is("pass"));
        assertThat(rs.getInt(TestColumn.SCORE.toString()), is(80));
        assertThat(rs.getString(TestColumn.SEX.toString()), is("male"));
        assertThat(rs.getString(TestColumn.SAVED.toString()), is("2017-02-21 17:07:42"));

        assertTrue(rs.next());
        assertThat(rs.getString(TestColumn.NAME.toString()), is("name2"));
        assertThat(rs.getString(TestColumn.PASSWORD.toString()), is("pass2"));
        assertThat(rs.getInt(TestColumn.SCORE.toString()), is(45));
        assertThat(rs.getString(TestColumn.SEX.toString()), is("female"));
        assertThat(rs.getString(TestColumn.SAVED.toString()), is("2014-12-01 14:16:02"));

        rs = mDB.selectAllColumns(TestColumn.class, TestColumn.ID, 1);
        assertTrue(rs.next());
        assertThat(rs.getString(TestColumn.NAME.toString()), is("name1"));
        assertThat(rs.getInt(TestColumn.SCORE.toString()), is(80));
    }

    private void updateDB() throws SQLException {
        Map<TestColumn, Object> values = new EnumMap<>(TestColumn.class);
        values.put(TestColumn.PASSWORD, "changed");
        values.put(TestColumn.SCORE, 62);
        int result = mDB.update(TestColumn.class, values, TestColumn.NAME.toString() + " = ?", "name2");
        assertThat(result, is(1));

        ResultSet rs = mDB.selectAllColumns(TestColumn.class, TestColumn.NAME.toString() + "=?", "name2");
        assertTrue(rs.next());
        assertThat(rs.getString(TestColumn.NAME.toString()), is("name2"));
        assertThat(rs.getString(TestColumn.PASSWORD.toString()), is("changed"));
        assertThat(rs.getInt(TestColumn.SCORE.toString()), is(62));
        assertThat(rs.getString(TestColumn.SEX.toString()), is("female"));
        assertThat(rs.getString(TestColumn.SAVED.toString()), is("2014-12-01 14:16:02"));
    }

    private void checkSumMaxMinDB() throws SQLException {
        int sum = mDB.sum(TestColumn.class, TestColumn.SCORE);
        assertThat(sum, is(80 + 62));

        int max = mDB.max(TestColumn.class, TestColumn.SCORE);
        assertThat(max, is(80));

        int min = mDB.min(TestColumn.class, TestColumn.SCORE);
        assertThat(min, is(62));
    }

    private void deleteRecord() throws SQLException {
        int result = mDB.delete(TestColumn.class, TestColumn.NAME.toString() + "=?", "name2");
        assertThat(result, is(1));

        ResultSet rs = mDB.selectAll(TestColumn.class);
        assertTrue(rs.next());
        assertThat(rs.getString(TestColumn.NAME.toString()), is("name1"));
        assertFalse(rs.next());

        assertThat(mDB.count(TestColumn.class), is(1));
    }

    private void singleWhere() throws SQLException {
        // where句がaaa=bbbの形であれば文字列で引数を与えないパターン
        Map<TestColumn, Object> values = new EnumMap<>(TestColumn.class);
        values.put(TestColumn.NAME, "single where");
        values.put(TestColumn.PASSWORD, "single password");
        values.put(TestColumn.SCORE, 63);
        values.put(TestColumn.SEX, "female");
        String saved = SQLs.formatString(System.currentTimeMillis(), "yyyy/MM/dd HH:mm:ss");
        values.put(TestColumn.SAVED, saved);
        long id = mDB.insert(TestColumn.class, values);

        // exist
        boolean isExist = mDB.isExistRecord(TestColumn.class, TestColumn.NAME, "single where");
        assertTrue(isExist);

        // select
        ResultSet rs = mDB.selectAllColumns(TestColumn.class, TestColumn.ID, id);
        assertTrue(rs.next());
        assertThat(rs.getLong(TestColumn.ID.toString()), is(id));
        assertThat(rs.getInt(TestColumn.SCORE.toString()), is(63));

        // update
        Map<TestColumn, Object> newValues = new EnumMap<>(TestColumn.class);
        newValues.put(TestColumn.SCORE, 72);
        newValues.put(TestColumn.PASSWORD, "new single password");
        int result = mDB.update(TestColumn.class, newValues, TestColumn.NAME, "single where");
        assertThat(result, is(1));

        ResultSet rs2 = mDB.selectAllColumns(TestColumn.class, TestColumn.ID, id);
        assertTrue(rs2.next());
        assertThat(rs2.getString(TestColumn.NAME.toString()), is("single where"));
        assertThat(rs2.getString(TestColumn.PASSWORD.toString()), is("new single password"));
        assertThat(rs2.getInt(TestColumn.SCORE.toString()), is(72));

        // delete
        int resultDelete = mDB.delete(TestColumn.class, TestColumn.ID, id);
        assertThat(resultDelete, is(1));

        boolean isExistEnd = mDB.isExistRecord(TestColumn.class, TestColumn.ID, id);
        assertFalse(isExistEnd);
    }

    private void empty() throws SQLException {
        mDB.empty(TestColumn.class);
        int count = mDB.count(TestColumn.class);
        assertThat(count, is(0));
    }

    @Test
    public void operateDatabase() throws SQLException {
        //  各メソッドに依存関係があるため、呼び出し順序を維持すること。
        setupDB();

        insertDB();
        checkSelectAll();
        updateDB();
        checkSumMaxMinDB();
        deleteRecord();
        singleWhere();

        empty();
    }

    @Test
    public void selectSqlString() {
        String table = "test_table";
        List<TestColumn> columns = new ArrayList<>();
        columns.add(TestColumn.ID);
        columns.add(TestColumn.NAME);
        columns.add(TestColumn.SCORE);

        String expected1 = "SELECT id,name,score FROM test_table";
        String result1 = SQLs.createSelectSql(table, columns.toArray(new TestColumn[0]), null);
        assertThat(result1, is(expected1));

        String whereClause = "id = ? and score = 40";
        String expected2 = "SELECT id,name,score FROM test_table WHERE id = ? and score = 40";
        String result2 = SQLs.createSelectSql(table, columns.toArray(new TestColumn[0]), whereClause);
        assertThat(result2, is(expected2));
    }

    @Test
    public void updateSqlString() {
        String table = "test_table";
        Map<DatabaseColumn, Object> values = new LinkedHashMap<>();
        values.put(TestColumn.SCORE, 43);
        values.put(TestColumn.SAVED, "2014-11-9 14:32:42");

        String expected1 = "UPDATE test_table SET score = 43,saved = '2014-11-9 14:32:42'";
        String result1 = SQLs.createUpdateSql(table, values, "");
        assertThat(result1, is(expected1));

        String whereClause = "id == ?";
        String expected2 = "UPDATE test_table SET score = 43,saved = '2014-11-9 14:32:42' WHERE id == ?";
        String result2 = SQLs.createUpdateSql(table, values, whereClause);
        assertThat(result2, is(expected2));
    }

//    public void updateSqlStringByExpression() {
//        String table = "test_table";
//        Map<DatabaseColumn, Object> values = new LinkedHashMap<>();
//        values.put(TestColumn.SCORE, new SQLs.Expression(SQLs.createSelectFuncSql("max", table, TestColumn.SCORE.toString(), null)));
//        String whereClause = "id == ?";
//
//        String expected = "UPDATE test_table SET score = (SELECT max(score) FROM test_table) WHERE id == ?";
//        String result = SQLs.createUpdateSql(table, values, whereClause);
//        assertThat(result, is(expected));
//    }

    @Test
    public void insertSqlString() {
        String table = "test_table";
        Map<DatabaseColumn, Object> values = new LinkedHashMap<>();
        values.put(TestColumn.NAME, "test name");
        values.put(TestColumn.PASSWORD, "password");
        values.put(TestColumn.SCORE, 56);
        values.put(TestColumn.SEX, "male");
        values.put(TestColumn.SAVED, "2017-2-21 9:14:42");

        String expected = "INSERT INTO test_table (name,password,score,sex,saved) VALUES ('test name','password',56,'male','2017-2-21 9:14:42')";
        String result = SQLs.createInsertSql(table, values);
        assertThat(result, is(expected));
    }

    @Test
    public void deleteSqlString() {
        // DELETE FROM table WHERE whereClause
        String table = "test_table";
        String whereClause = "id = 2";

        String expected = "DELETE FROM test_table WHERE id = 2";
        String result = SQLs.createDeleteSql(table, whereClause);
        assertThat(result, is(expected));
    }

    @Test
    public void createTableString() {
        // CREATE TABLE table {colStr1,colStr2,colStr3}
        String table = "test_table";

        // オプションがないカラムは最後にスペースが入るので注意
        String expected = "CREATE TABLE test_table (id int not null primary key auto_increment,name varchar(255) not null unique key,password varchar(32) not null,score int not null default 0,sex enum('male', 'female') default 'male',saved datetime )";
        String result = SQLs.createCreateTableSql(table, TestColumn.values());
        assertThat(result, is(expected));
    }
}
