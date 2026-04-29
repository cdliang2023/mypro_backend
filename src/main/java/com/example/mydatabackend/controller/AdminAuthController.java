package com.example.mydatabackend.controller;

import com.example.mydatabackend.dto.AdminCodeRequest;
import com.example.mydatabackend.result.ApiResult;
import com.example.mydatabackend.service.AdminCodeService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminAuthController {

    private final AdminCodeService adminCodeService;

    public AdminAuthController(AdminCodeService adminCodeService) {
        this.adminCodeService = adminCodeService;
    }

    @PostMapping("/user-manage/send-code")
    public ApiResult<Void> sendCode(@RequestBody AdminCodeRequest request) {
        boolean success = adminCodeService.sendCode(request.getAdminName());

        if (!success) {
            return ApiResult.fail("管理员名称不正确");
        }

        return ApiResult.ok("动态密码已发送至管理员手机", null);
    }

    @PostMapping("/user-manage/verify")
    public ApiResult<Void> verify(@RequestBody AdminCodeRequest request) {
        boolean success = adminCodeService.verifyCode(
                request.getAdminName(),
                request.getCode()
        );

        if (!success) {
            return ApiResult.fail("管理员名称或动态密码错误");
        }

        return ApiResult.ok("管理员验证成功", null);
    }
}
