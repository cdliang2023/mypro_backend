package com.example.mydatabackend.mapper;

import com.example.mydatabackend.vo.ArticleVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface ArticleMapper {

    @Select("""
        SELECT 
            id,
            title AS title,
            abstractTxt AS abstractTxt,
            author,
            down_url AS downloadUrl
        FROM article
        ORDER BY id DESC
        LIMIT #{size} OFFSET #{offset}
    """)
    List<ArticleVO> findPage(@Param("offset") int offset,
                             @Param("size") int size);

    @Select("""
        SELECT COUNT(*)
        FROM article
    """)
    long countAll();

    @Select("""
        SELECT 
            id,
            title AS title,
            abstractTxt AS abstractTxt,
            author,
            down_url AS downloadUrl
        FROM article
        WHERE id = #{id}
    """)
    ArticleVO findById(@Param("id") Integer id);

    @Select("""
        SELECT 
            id,
            title AS title,
            abstractTxt AS abstractTxt,
            author,
            down_url AS downloadUrl
        FROM article
        WHERE
            #{keyword} IS NULL
            OR #{keyword} = ''
            OR title LIKE CONCAT('%', #{keyword}, '%')
            OR abstractTxt LIKE CONCAT('%', #{keyword}, '%')
            OR author LIKE CONCAT('%', #{keyword}, '%')
            OR down_url LIKE CONCAT('%', #{keyword}, '%')
        ORDER BY id DESC
        LIMIT #{size} OFFSET #{offset}
    """)
    List<ArticleVO> findPaperPage(@Param("keyword") String keyword,
                                  @Param("offset") int offset,
                                  @Param("size") int size);

    @Select("""
        SELECT COUNT(*)
        FROM article
        WHERE
            #{keyword} IS NULL
            OR #{keyword} = ''
            OR title LIKE CONCAT('%', #{keyword}, '%')
            OR abstractTxt LIKE CONCAT('%', #{keyword}, '%')
            OR author LIKE CONCAT('%', #{keyword}, '%')
            OR down_url LIKE CONCAT('%', #{keyword}, '%')
    """)
    long countPaperPage(@Param("keyword") String keyword);

    @Select("""
        SELECT COUNT(*)
        FROM article
        WHERE id = #{id}
    """)
    int countById(@Param("id") Integer id);

    @Insert("""
        INSERT INTO article
            (title, abstractTxt, author, down_url)
        VALUES
            (#{title}, #{abstractTxt}, #{author}, #{downloadUrl})
    """)
    int insertPaper(@Param("title") String title,
                    @Param("abstractTxt") String abstractTxt,
                    @Param("author") String author,
                    @Param("downloadUrl") String downloadUrl);

    @Update("""
        UPDATE article
        SET
            title = #{title},
            abstractTxt = #{abstractTxt},
            author = #{author},
            down_url = #{downloadUrl}
        WHERE id = #{id}
    """)
    int updatePaper(@Param("id") Integer id,
                    @Param("title") String title,
                    @Param("abstractTxt") String abstractTxt,
                    @Param("author") String author,
                    @Param("downloadUrl") String downloadUrl);

    @Delete("""
        DELETE FROM article
        WHERE id = #{id}
    """)
    int deletePaper(@Param("id") Integer id);
    @Select("""
    SELECT COUNT(*)
    FROM article
    WHERE author = #{author}
""")
int countByAuthor(@Param("author") String author);
}