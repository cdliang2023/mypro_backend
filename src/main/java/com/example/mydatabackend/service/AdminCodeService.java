package com.example.mydatabackend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdminCodeService {

    @Value("${admin.manage.name:admin}")
    private String adminName;

    @Value("${admin.manage.phone:15961253171}")
    private String adminPhone;

    private final SecureRandom random = new SecureRandom();

    private final Map<String, CodeItem> codeCache = new ConcurrentHashMap<>();

    public boolean sendCode(String inputAdminName) {
        if (inputAdminName == null || !adminName.equals(inputAdminName.trim())) {
            return false;
        }

        String code = String.format("%06d", random.nextInt(1000000));

        CodeItem item = new CodeItem();
        item.code = code;
        item.expireTime = LocalDateTime.now().plusMinutes(5);

        codeCache.put(adminName, item);

        /*
         * 这里是关键：
         * 目前这个版本只是把验证码打印到后端控制台，方便你本地开发测试。
         * 如果要真的发到 15961253171，需要接入短信服务商，例如阿里云短信、腾讯云短信等。
         */
        System.out.println("管理员手机号：" + adminPhone);
        System.out.println("用户管理动态密码：" + code);
        System.out.println("有效期：5分钟");

        return true;
    }

    public boolean verifyCode(String inputAdminName, String inputCode) {
        if (inputAdminName == null || inputCode == null) {
            return false;
        }

        if (!adminName.equals(inputAdminName.trim())) {
            return false;
        }

        CodeItem item = codeCache.get(adminName);

        if (item == null) {
            return false;
        }

        if (LocalDateTime.now().isAfter(item.expireTime)) {
            codeCache.remove(adminName);
            return false;
        }

        boolean matched = item.code.equals(inputCode.trim());

        if (matched) {
            codeCache.remove(adminName);
        }

        return matched;
    }

    private static class CodeItem {
        private String code;
        private LocalDateTime expireTime;
    }
}
