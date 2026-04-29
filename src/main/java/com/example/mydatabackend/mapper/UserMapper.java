package com.example.mydatabackend.mapper;

import com.example.mydatabackend.vo.UserLoginVO;
import com.example.mydatabackend.vo.UserManageVO;
import com.example.mydatabackend.vo.UserVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface UserMapper {

    /*
     * BCrypt 加密后，登录不能再用：
     * WHERE usr_name = ? AND password = ?
     *
     * 应该先根据用户名查出用户和数据库中的加密密码，
     * 然后在 AuthController 里用 passwordEncoder.matches() 判断密码是否正确。
     */
    @Select("""
        SELECT 
            usr_name AS usrName,
            password,
            qqnum,
            address,
            avatar,
            download_right AS downloadRight
        FROM `user`
        WHERE usr_name = #{usrName}
    """)
    UserLoginVO findLoginUserByUsrName(@Param("usrName") String usrName);


    /*
     * 这个旧登录方法如果你的 AuthController 已经不用了，可以删除。
     * 如果暂时保留，也不要再用它做登录，因为它只能匹配明文密码。
     */
    @Select("""
        SELECT 
            usr_name AS usrName,
            qqnum,
            address,
            avatar,
            download_right AS downloadRight
        FROM `user`
        WHERE usr_name = #{usrName}
          AND password = #{password}
    """)
    UserVO login(@Param("usrName") String usrName,
                 @Param("password") String password);


    /*
     * 判断用户名是否已经存在
     */
    @Select("""
        SELECT COUNT(*)
        FROM `user`
        WHERE usr_name = #{usrName}
    """)
    int countByUsrName(@Param("usrName") String usrName);


    /*
     * 注册用户
     * 注意：这里传入的 password 应该已经在 AuthController 中用 BCrypt 加密。
     */
    @Insert("""
        INSERT INTO `user` 
            (usr_name, password, qqnum, address, avatar)
        VALUES 
            (#{usrName}, #{password}, #{qqnum}, #{address}, #{avatar})
    """)
    int register(@Param("usrName") String usrName,
                 @Param("password") String password,
                 @Param("qqnum") String qqnum,
                 @Param("address") String address,
                 @Param("avatar") String avatar);


    /*
     * 下载接口根据用户名查询下载权限
     */
    @Select("""
        SELECT download_right
        FROM `user`
        WHERE usr_name = #{usrName}
    """)
    Integer findDownloadRightByUsrName(@Param("usrName") String usrName);


    /*
     * 用户管理：分页查询用户
     */
    @Select("""
        SELECT 
            usr_name AS usrName,
            qqnum,
            address,
            avatar,
            download_right AS downloadRight
        FROM `user`
        WHERE 
            #{keyword} IS NULL
            OR #{keyword} = ''
            OR usr_name LIKE CONCAT('%', #{keyword}, '%')
            OR qqnum LIKE CONCAT('%', #{keyword}, '%')
            OR address LIKE CONCAT('%', #{keyword}, '%')
        ORDER BY usr_name ASC
        LIMIT #{size} OFFSET #{offset}
    """)
    List<UserManageVO> findUserPage(@Param("keyword") String keyword,
                                    @Param("offset") int offset,
                                    @Param("size") int size);


    /*
     * 用户管理：统计查询结果总数
     */
    @Select("""
        SELECT COUNT(*)
        FROM `user`
        WHERE 
            #{keyword} IS NULL
            OR #{keyword} = ''
            OR usr_name LIKE CONCAT('%', #{keyword}, '%')
            OR qqnum LIKE CONCAT('%', #{keyword}, '%')
            OR address LIKE CONCAT('%', #{keyword}, '%')
    """)
    long countUserPage(@Param("keyword") String keyword);


    /*
     * 用户管理：新增用户
     * 注意：password 应该在 UserManageController 中先加密。
     */
    @Insert("""
        INSERT INTO `user`
            (usr_name, password, qqnum, address, avatar, download_right)
        VALUES
            (#{usrName}, #{password}, #{qqnum}, #{address}, #{avatar}, #{downloadRight})
    """)
    int insertUser(@Param("usrName") String usrName,
                   @Param("password") String password,
                   @Param("qqnum") String qqnum,
                   @Param("address") String address,
                   @Param("avatar") String avatar,
                   @Param("downloadRight") Integer downloadRight);


    /*
     * 用户管理：修改用户
     * 如果 password 为空字符串，则不修改密码。
     * 如果 password 不为空，应该是 UserManageController 中加密后的密码。
     */
    @Update("""
        <script>
        UPDATE `user`
        SET
            qqnum = #{qqnum},
            address = #{address},
            avatar = #{avatar},
            download_right = #{downloadRight}
            <if test="password != null and password != ''">
                , password = #{password}
            </if>
        WHERE usr_name = #{usrName}
        </script>
    """)
    int updateUser(@Param("usrName") String usrName,
                   @Param("password") String password,
                   @Param("qqnum") String qqnum,
                   @Param("address") String address,
                   @Param("avatar") String avatar,
                   @Param("downloadRight") Integer downloadRight);


    /*
     * 用户管理：删除用户
     */
    @Delete("""
        DELETE FROM `user`
        WHERE usr_name = #{usrName}
    """)
    int deleteUser(@Param("usrName") String usrName);
}