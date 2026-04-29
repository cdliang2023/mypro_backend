package com.example.mydatabackend.controller;

import com.example.mydatabackend.dto.UserSaveRequest;
import com.example.mydatabackend.mapper.UserMapper;
import com.example.mydatabackend.result.ApiResult;
import com.example.mydatabackend.result.PageResult;
import com.example.mydatabackend.vo.UserManageVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class UserManageController {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserManageController(UserMapper userMapper,
                                PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/users")
    public ApiResult<PageResult<UserManageVO>> pageUsers(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        if (page < 1) {
            page = 1;
        }

        if (size < 1) {
            size = 10;
        }

        int offset = (page - 1) * size;
        String realKeyword = keyword == null ? "" : keyword.trim();

        List<UserManageVO> records = userMapper.findUserPage(realKeyword, offset, size);
        long total = userMapper.countUserPage(realKeyword);

        PageResult<UserManageVO> pageResult = new PageResult<>(records, total, page, size);

        return ApiResult.ok("用户查询成功", pageResult);
    }

    @PostMapping("/users")
    public ApiResult<Void> addUser(@RequestBody UserSaveRequest request) {
        if (request.getUsrName() == null || request.getUsrName().trim().isEmpty()) {
            return ApiResult.fail("用户名不能为空");
        }

        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            return ApiResult.fail("密码不能为空");
        }

        String usrName = request.getUsrName().trim();
        String rawPassword = request.getPassword().trim();

        int exists = userMapper.countByUsrName(usrName);

        if (exists > 0) {
            return ApiResult.fail("用户名已存在");
        }

        String avatar = request.getAvatar();

        if (avatar == null || avatar.trim().isEmpty()) {
            avatar = "default.jpg";
        }

        Integer downloadRight = request.getDownloadRight();

        if (downloadRight == null) {
            downloadRight = 0;
        }

        /*
         * 新增用户时，必须把明文密码加密后再保存。
         * 数据库中保存的是 BCrypt 密文，不再保存明文密码。
         */
        String encodedPassword = passwordEncoder.encode(rawPassword);

        userMapper.insertUser(
                usrName,
                encodedPassword,
                request.getQqnum(),
                request.getAddress(),
                avatar,
                downloadRight
        );

        return ApiResult.ok("新增用户成功", null);
    }

    @PutMapping("/users/{usrName}")
    public ApiResult<Void> updateUser(
            @PathVariable String usrName,
            @RequestBody UserSaveRequest request
    ) {
        if (usrName == null || usrName.trim().isEmpty()) {
            return ApiResult.fail("用户名不能为空");
        }

        String realUsrName = usrName.trim();

        int exists = userMapper.countByUsrName(realUsrName);

        if (exists <= 0) {
            return ApiResult.fail("用户不存在");
        }

        String avatar = request.getAvatar();

        if (avatar == null || avatar.trim().isEmpty()) {
            avatar = "default.jpg";
        }

        Integer downloadRight = request.getDownloadRight();

        if (downloadRight == null) {
            downloadRight = 0;
        }

        /*
         * 修改用户时：
         * 1. 如果密码为空：不修改原密码
         * 2. 如果密码不为空：把新密码 BCrypt 加密后保存
         */
        String encodedPassword = "";

        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            encodedPassword = passwordEncoder.encode(request.getPassword().trim());
        }

        userMapper.updateUser(
                realUsrName,
                encodedPassword,
                request.getQqnum(),
                request.getAddress(),
                avatar,
                downloadRight
        );

        return ApiResult.ok("修改用户成功", null);
    }

    @DeleteMapping("/users/{usrName}")
    public ApiResult<Void> deleteUser(@PathVariable String usrName) {
        if (usrName == null || usrName.trim().isEmpty()) {
            return ApiResult.fail("用户名不能为空");
        }

        String realUsrName = usrName.trim();

        int exists = userMapper.countByUsrName(realUsrName);

        if (exists <= 0) {
            return ApiResult.fail("用户不存在");
        }

        userMapper.deleteUser(realUsrName);

        return ApiResult.ok("删除用户成功", null);
    }
}