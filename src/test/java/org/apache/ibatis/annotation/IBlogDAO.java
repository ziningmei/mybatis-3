package org.apache.ibatis.annotation;

import org.apache.ibatis.annotations.*;
import org.apache.ibatis.type.JdbcType;

import java.util.List;

@CacheNamespace(size=100)
public interface IBlogDAO {

    @SelectProvider(type = BlogSqlProvider.class, method = "getSql")
    @Results(value ={
            @Result(id=true, property="id",column="id",javaType=Integer.class,jdbcType= JdbcType.INTEGER),
            @Result(property="title",column="title",javaType=String.class,jdbcType=JdbcType.VARCHAR),
            @Result(property="date",column="date",javaType=String.class,jdbcType=JdbcType.VARCHAR),
            @Result(property="authername",column="authername",javaType=String.class,jdbcType=JdbcType.VARCHAR),
            @Result(property="content",column="content",javaType=String.class,jdbcType=JdbcType.VARCHAR),
    })
    public Blog getBlog(@Param("id") int id);

    @SelectProvider(type = BlogSqlProvider.class, method = "getAllSql")
    @Results(value ={
            @Result(id=true, property="id",column="id",javaType=Integer.class,jdbcType=JdbcType.INTEGER),
            @Result(property="title",column="title",javaType=String.class,jdbcType=JdbcType.VARCHAR),
            @Result(property="date",column="date",javaType=String.class,jdbcType=JdbcType.VARCHAR),
            @Result(property="authername",column="authername",javaType=String.class,jdbcType=JdbcType.VARCHAR),
            @Result(property="content",column="content",javaType=String.class,jdbcType=JdbcType.VARCHAR),
    })
    public List<Blog> getAllBlog();

    @SelectProvider(type = BlogSqlProvider.class, method = "getSqlByTitle")
    @ResultMap(value = "sqlBlogsMap")
    // 这里调用resultMap，这个是SQL配置文件中的,必须该SQL配置文件与本接口有相同的全限定名
    // 注意文件中的namespace路径必须是使用@resultMap的类路径
    public List<Blog> getBlogByTitle(@Param("title")String title);

    @InsertProvider(type = BlogSqlProvider.class, method = "insertSql")
    public void insertBlog(Blog blog);

    @UpdateProvider(type = BlogSqlProvider.class, method = "updateSql")
    public void updateBlog(Blog blog);

    @DeleteProvider(type = BlogSqlProvider.class, method = "deleteSql")
    @Options(useCache = true, timeout = 10000)
    public void deleteBlog(int ids);

}
