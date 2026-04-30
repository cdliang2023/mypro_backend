package com.example.mydatabackend.controller;

import com.example.mydatabackend.dto.LoginRequest;
import com.example.mydatabackend.dto.RegisterRequest;
import com.example.mydatabackend.mapper.UserMapper;
import com.example.mydatabackend.result.ApiResult;
import com.example.mydatabackend.vo.UserLoginVO;
import com.example.mydatabackend.vo.UserVO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate stringRedisTemplate;

    public AuthController(UserMapper userMapper,
                          PasswordEncoder passwordEncoder,
                          StringRedisTemplate stringRedisTemplate) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @PostMapping("/login")
    public ApiResult<UserVO> login(@RequestBody LoginRequest request) {
        if (request.getUsrName() == null || request.getUsrName().trim().isEmpty()) {
            return ApiResult.fail("用户名不能为空");
        }

        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            return ApiResult.fail("密码不能为空");
        }

        if (request.getCaptchaId() == null || request.getCaptchaId().trim().isEmpty()) {
            return ApiResult.fail("验证码编号不能为空，请刷新验证码");
        }

        if (request.getCaptchaCode() == null || request.getCaptchaCode().trim().isEmpty()) {
            return ApiResult.fail("验证码不能为空");
        }

        String redisKey = "captcha:" + request.getCaptchaId().trim();
        String redisCode = stringRedisTemplate.opsForValue().get(redisKey);

        if (redisCode == null) {
            return ApiResult.fail("验证码已过期，请刷新验证码");
        }

        if (!redisCode.equalsIgnoreCase(request.getCaptchaCode().trim())) {
            return ApiResult.fail("验证码错误");
        }

        // 验证码校验成功后立即删除，防止重复使用
        stringRedisTemplate.delete(redisKey);

        String usrName = request.getUsrName().trim();
        String password = request.getPassword().trim();

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