package com.example.mydatabackend.controller;

import com.example.mydatabackend.dto.LoginRequest;
import com.example.mydatabackend.dto.RegisterRequest;
import com.example.mydatabackend.mapper.UserMapper;
import com.example.mydatabackend.result.ApiResult;
import com.example.mydatabackend.vo.UserLoginVO;
import com.example.mydatabackend.vo.UserVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserMapper userMapper,
                          PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ApiResult<UserVO> login(@RequestBody LoginRequest request) {
        if (request.getUsrName() == null || request.getUsrName().trim().isEmpty()) {
            return ApiResult.fail("用户名不能为空");
        }

        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            return ApiResult.fail("密码不能为空");
        }

        String usrName = request.getUsrName().trim();
        String password = request.getPassword().trim();

        /*
         * BCrypt 加密后，不能再用：
         * WHERE usr_name = ? AND password = ?
         *
         * 应该先根据用户名查出数据库中的加密密码，
         * 然后用 passwordEncoder.matches() 判断。
         */
        UserLoginVO loginUser = userMapper.findLoginUserByUsrName(usrName);

        if (loginUser == null) {
            return ApiResult.fail("用户名或密码错误");
        }

        String dbPassword = loginUser.getPassword();

        if (dbPassword == null || dbPassword.trim().isEmpty()) {
            return ApiResult.fail("用户名或密码错误");
        }

        boolean passwordMatched = passwordEncoder.matches(password, dbPassword);

        if (!passwordMatched) {
            return ApiResult.fail("用户名或密码错误");
        }

        /*
         * 登录成功后，不要把数据库中的 password 返回给前端。
         * 只返回前端需要显示和判断权限的信息。
         */
        UserVO userVO = new UserVO();
        userVO.setUsrName(loginUser.getUsrName());
        userVO.setQqnum(loginUser.getQqnum());
        userVO.setAddress(loginUser.getAddress());
        userVO.setAvatar(loginUser.getAvatar());
        userVO.setDownloadRight(loginUser.getDownloadRight());

        return ApiResult.ok("登录成功", userVO);
    }

    @PostMapping("/register")
    public ApiResult<Void> register(@RequestBody RegisterRequest request) {
        if (request.getUsrName() == null || request.getUsrName().trim().isEmpty()) {
            return ApiResult.fail("用户名不能为空");
        }

        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            return ApiResult.fail("密码不能为空");
        }

        String usrName = request.getUsrName().trim();
        String password = request.getPassword().trim();

        int exists = userMapper.countByUsrName(usrName);

        if (exists > 0) {
            return ApiResult.fail("用户名已存在");
        }

        String avatar = request.getAvatar();

        if (avatar == null || avatar.trim().isEmpty()) {
            avatar = "default.jpg";
        }

        /*
         * 注册时把明文密码加密后再保存到数据库。
         * 用户前端仍然输入原始密码，例如 123456。
         * 数据库存储的是 BCrypt 密文。
         */
        String encodedPassword = passwordEncoder.encode(password);

        userMapper.register(
                usrName,
                encodedPassword,
                request.getQqnum(),
                request.getAddress(),
                avatar
        );

        return ApiResult.ok("注册成功", null);
    }
}