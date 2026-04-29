package com.example.mydatabackend.controller;

import com.example.mydatabackend.mapper.ArticleMapper;
import com.example.mydatabackend.result.ApiResult;
import com.example.mydatabackend.result.PageResult;
import com.example.mydatabackend.vo.ArticleVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;


@RestController
@RequestMapping("/api")
public class PaperManageController {

    private final ArticleMapper articleMapper;

    @Value("${paper.root-path}")
    private String paperRootPath;

    public PaperManageController(ArticleMapper articleMapper) {
        this.articleMapper = articleMapper;
    }

    @GetMapping("/papers")
    public ApiResult<PageResult<ArticleVO>> pagePapers(
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

        List<ArticleVO> records = articleMapper.findPaperPage(realKeyword, offset, size);
        long total = articleMapper.countPaperPage(realKeyword);

        PageResult<ArticleVO> pageResult = new PageResult<>(records, total, page, size);

        return ApiResult.ok("论文查询成功", pageResult);
    }

    /**
     * 新增论文：论文信息 + 真实文件一起上传。
     */
    @PostMapping(value = "/papers", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ApiResult<Void> addPaper(
        @RequestParam String title,
        @RequestParam(required = false) String abstractTxt,
        @RequestParam String author,
        @RequestPart("file") MultipartFile file
) {
    try {
        if (title == null || title.trim().isEmpty()) {
            return ApiResult.fail("论文标题不能为空");
        }

        if (author == null || author.trim().isEmpty()) {
            return ApiResult.fail("作者不能为空，因为系统需要根据作者名生成文件名");
        }

        if (file == null || file.isEmpty()) {
            return ApiResult.fail("请上传论文文件");
        }

        String savedRelativePath = savePaperFile(file, author.trim());

        articleMapper.insertPaper(
                title.trim(),
                abstractTxt == null ? "" : abstractTxt.trim(),
                author.trim(),
                savedRelativePath
        );

        return ApiResult.ok("新增论文成功", null);
    } catch (Exception e) {
        e.printStackTrace();
        return ApiResult.fail("新增论文失败：" + e.getMessage());
    }
}

    /**
     * 修改论文：可以只修改文字信息；如果重新选择文件，则替换 down_url。
     */
    @PutMapping(value = "/papers/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResult<Void> updatePaper(
            @PathVariable Integer id,
            @RequestParam String title,
            @RequestParam(required = false) String abstractTxt,
            @RequestParam(required = false) String author,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        try {
            if (id == null) {
                return ApiResult.fail("论文 ID 不能为空");
            }

            ArticleVO oldPaper = articleMapper.findById(id);

            if (oldPaper == null) {
                return ApiResult.fail("论文不存在");
            }

            if (title == null || title.trim().isEmpty()) {
                return ApiResult.fail("论文标题不能为空");
            }

            String downloadUrl = oldPaper.getDownloadUrl();

            if (file != null && !file.isEmpty()) {
                downloadUrl = savePaperFile(file,author.trim());

                // 可选：删除旧文件。如果你不想自动删除旧文件，可以注释掉下一行。
                deletePhysicalFileQuietly(oldPaper.getDownloadUrl());
            }

            articleMapper.updatePaper(
                    id,
                    title.trim(),
                    abstractTxt == null ? "" : abstractTxt.trim(),
                    author == null ? "" : author.trim(),
                    downloadUrl
            );

            return ApiResult.ok("修改论文成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.fail("修改论文失败：" + e.getMessage());
        }
    }

    /**
     * 删除论文：删除数据库记录，同时尝试删除本地论文文件。
     */
    @DeleteMapping("/papers/{id}")
    public ApiResult<Void> deletePaper(@PathVariable Integer id) {
        try {
            if (id == null) {
                return ApiResult.fail("论文 ID 不能为空");
            }

            ArticleVO oldPaper = articleMapper.findById(id);

            if (oldPaper == null) {
                return ApiResult.fail("论文不存在");
            }

            articleMapper.deletePaper(id);

            // 可选：删除真实文件。如果只想删除数据库记录，可以注释掉这一行。
            deletePhysicalFileQuietly(oldPaper.getDownloadUrl());

            return ApiResult.ok("删除论文成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.fail("删除论文失败：" + e.getMessage());
        }
    }

    /**
     * 保存上传的论文文件，并返回保存到数据库的相对路径。
     * 数据库 article.down_url 保存这个返回值。
     */
    private String savePaperFile(MultipartFile file, String author) throws Exception {
    String originalName = file.getOriginalFilename();

    if (originalName == null || originalName.trim().isEmpty()) {
        throw new RuntimeException("文件名不能为空");
    }

    String safeOriginalName = Paths.get(originalName).getFileName().toString();
    String lowerName = safeOriginalName.toLowerCase();

    String ext;

    if (lowerName.endsWith(".pdf")) {
        ext = ".pdf";
    } else if (lowerName.endsWith(".doc")) {
        ext = ".doc";
    } else if (lowerName.endsWith(".docx")) {
        ext = ".docx";
    } else if (lowerName.endsWith(".txt")) {
        ext = ".txt";
    } else {
        throw new RuntimeException("只允许上传 pdf、doc、docx、txt 文件");
    }

    /*
     * 清理作者名，避免 Windows 文件名非法字符。
     * 例如：
     * chen → chen
     * chen/li → chen_li
     */
    String safeAuthor = author.trim().replaceAll("[\\\\/:*?\"<>|\\s]+", "_");

    if (safeAuthor.isEmpty()) {
        safeAuthor = "unknown";
    }

    /*
     * 计算该作者已经上传过多少篇论文。
     * 如果 chen 已经有 2 条记录，则下一篇为 chen_3.pdf。
     */
    int uploadedCount = articleMapper.countByAuthor(author.trim());
    int nextIndex = uploadedCount + 1;

    Path rootPath = Paths.get(paperRootPath).toAbsolutePath().normalize();

    Files.createDirectories(rootPath);

    Path targetPath;
    String savedFileName;

    /*
     * 防止文件已经存在。
     * 如果 chen_3.pdf 已经存在，则自动尝试 chen_4.pdf。
     */
    while (true) {
        savedFileName = safeAuthor + "_" + nextIndex + ext;
        targetPath = rootPath.resolve(savedFileName).normalize();

        if (!targetPath.startsWith(rootPath)) {
            throw new RuntimeException("文件路径非法");
        }

        if (!Files.exists(targetPath)) {
            break;
        }

        nextIndex++;
    }

    file.transferTo(targetPath.toFile());

    /*
     * 返回写入数据库 article.down_url 的相对路径。
     */
    return savedFileName;
}

    private void deletePhysicalFileQuietly(String relativePath) {
        try {
            if (relativePath == null || relativePath.trim().isEmpty()) {
                return;
            }

            Path rootPath = Paths.get(paperRootPath).toAbsolutePath().normalize();
            Path filePath = rootPath.resolve(relativePath.trim()).normalize();

            if (!filePath.startsWith(rootPath)) {
                return;
            }

            Files.deleteIfExists(filePath);
        } catch (Exception ignored) {
        }
    }
}