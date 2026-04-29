package com.example.mydatabackend.controller;

import com.example.mydatabackend.mapper.ArticleMapper;
import com.example.mydatabackend.mapper.UserMapper;
import com.example.mydatabackend.result.ApiResult;
import com.example.mydatabackend.result.PageResult;
import com.example.mydatabackend.vo.ArticleVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ArticleController {

    private final ArticleMapper articleMapper;
    private final UserMapper userMapper;

    @Value("${paper.root-path}")
    private String paperRootPath;

    public ArticleController(ArticleMapper articleMapper, UserMapper userMapper) {
        this.articleMapper = articleMapper;
        this.userMapper = userMapper;
    }

    @GetMapping("/articles")
    public ApiResult<PageResult<ArticleVO>> pageArticles(
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

        List<ArticleVO> records = articleMapper.findPage(offset, size);
        long total = articleMapper.countAll();

        /*
         * 数据库 article.down_url 保存的是文件名或相对路径，例如：
         * 1.pdf
         * 2026/3.pdf
         *
         * 返回前端时，改成后端下载接口地址。
         */
        for (ArticleVO article : records) {
            article.setDownloadUrl("/api/articles/" + article.getId() + "/download");
        }

        PageResult<ArticleVO> pageResult = new PageResult<>(records, total, page, size);

        return ApiResult.ok("文章加载成功", pageResult);
    }

    @GetMapping("/articles/{id}/download")
    public ResponseEntity<ByteArrayResource> downloadArticle(
            @PathVariable Integer id,
            @RequestParam String usrName
    ) {
        try {
            if (usrName == null || usrName.trim().isEmpty()) {
                return ResponseEntity.status(403).build();
            }

            Integer downloadRight = userMapper.findDownloadRightByUsrName(usrName.trim());

            if (downloadRight == null || downloadRight <= 0) {
                return ResponseEntity.status(403).build();
            }

            ArticleVO article = articleMapper.findById(id);

            if (article == null) {
                return ResponseEntity.notFound().build();
            }

            /*
             * 这里的 downloadUrl 来自数据库 article.down_url。
             * 例如：
             * 1.pdf
             * 2.pdf
             * 2026/3.pdf
             */
            if (article.getDownloadUrl() == null || article.getDownloadUrl().trim().isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            String fileRelativePath = article.getDownloadUrl().trim();

            Path rootPath = Paths.get(paperRootPath).toAbsolutePath().normalize();
            Path filePath = rootPath.resolve(fileRelativePath).normalize();

            // 防止路径穿越
            if (!filePath.startsWith(rootPath)) {
                return ResponseEntity.status(403).build();
            }

            if (!Files.exists(filePath) || !Files.isRegularFile(filePath) || !Files.isReadable(filePath)) {
                return ResponseEntity.notFound().build();
            }

            /*
             * 关键改动：
             * 先把网络共享文件完整读入内存。
             * 这样避免 Spring 在写响应过程中又去读取 SMB 文件流导致中途网络错误。
             */
            byte[] fileBytes = Files.readAllBytes(filePath);

            ByteArrayResource resource = new ByteArrayResource(fileBytes);

            String fileName = filePath.getFileName().toString();

            String encodedFileName = URLEncoder
                    .encode(fileName, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            return ResponseEntity.ok()
                    .contentLength(fileBytes.length)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                    .body(resource);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
}