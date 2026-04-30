package com.example.mydatabackend.controller;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@CrossOrigin(exposedHeaders = "captcha-id")
@RestController
public class CaptchaController {

    private final DefaultKaptcha defaultKaptcha;
    private final StringRedisTemplate stringRedisTemplate;

    public CaptchaController(DefaultKaptcha defaultKaptcha,
                             StringRedisTemplate stringRedisTemplate) {
        this.defaultKaptcha = defaultKaptcha;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @GetMapping("/api/captcha")
    public void getCaptcha(HttpServletResponse response) throws IOException {
        String captchaId = UUID.randomUUID().toString();
        String code = defaultKaptcha.createText();

        String redisKey = "captcha:" + captchaId;

        // 验证码有效期 5 分钟
        stringRedisTemplate.opsForValue().set(redisKey, code, 5, TimeUnit.MINUTES);

        response.setHeader("captcha-id", captchaId);
        response.setContentType("image/jpeg");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        BufferedImage image = defaultKaptcha.createImage(code);
        ImageIO.write(image, "jpg", response.getOutputStream());
    }
}