package org.apache.ibatis.annotation;

import java.util.Map;

import static org.apache.ibatis.jdbc.SqlBuilder.*;

public class BlogSqlProvider {

    private final static String TABLE_NAME = "blog";

    public String getSql(Map<Integer, Object> parameter) {
        BEGIN();
        //SELECT("id,title,authername,date,content");
        SELECT("*");
        FROM(TABLE_NAME);
        //注意这里这种传递参数方式，#{}与map中的key对应，而map中的key又是注解param设置的
        WHERE("id = #{id}");
        return SQL();
    }

    public String getAllSql() {
        BEGIN();
        SELECT("*");
        FROM(TABLE_NAME);
        return SQL();
    }

    public String getSqlByTitle(Map<String, Object> parameter) {
        String title = (String) parameter.get("title");
        BEGIN();
        SELECT("*");
        FROM(TABLE_NAME);
        if (title != null)
            WHERE(" title like #{title}");
        return SQL();
    }

    public String insertSql() {
        BEGIN();
        INSERT_INTO(TABLE_NAME);
        VALUES("title", "#{title}");
        //  VALUES("title", "#{tt.title}");
        //这里是传递一个Blog对象的，如果是利用上面tt.方式，则必须利用Param来设置别名
        VALUES("date", "#{date}");
        VALUES("authername", "#{authername}");
        VALUES("content", "#{content}");
        return SQL();
    }

    public String deleteSql() {
        BEGIN();
        DELETE_FROM(TABLE_NAME);
        WHERE("id = #{id}");
        return SQL();
    }

    public String updateSql() {
        BEGIN();
        UPDATE(TABLE_NAME);
        SET("content = #{content}");
        WHERE("id = #{id}");
        return SQL();
    }

}
