package com.analytics.service.services;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Сохраняет загруженные картинки на диск и отдаёт относительный URL /images/{файл}.
 * Каталог настраивается свойством app.upload.dir (по умолчанию ./uploads).
 */
@Service
public class ImageStorageService {

    private final Path uploadDir;

    public ImageStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось создать каталог для картинок: " + uploadDir, e);
        }
    }

    /** Сохраняет один файл и возвращает URL вида /images/uuid.jpg */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Пустой файл картинки");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Можно загружать только изображения, а пришло: " + contentType);
        }

        String ext = extensionOf(file.getOriginalFilename());
        String filename = UUID.randomUUID() + ext;
        Path target = uploadDir.resolve(filename).normalize();

        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось сохранить файл " + filename, e);
        }
        return "/images/" + filename;
    }

    /** Сохраняет пачку файлов по порядку. */
    public List<String> storeAll(List<MultipartFile> files) {
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            urls.add(store(file));
        }
        return urls;
    }

    /** Удаляет файл по ранее выданному URL (используется при замене галереи). */
    public void delete(String url) {
        if (url == null || !url.startsWith("/images/")) {
            return;
        }
        String filename = url.substring("/images/".length());
        try {
            Files.deleteIfExists(uploadDir.resolve(filename).normalize());
        } catch (IOException ignored) {
            // файла может уже не быть — не критично
        }
    }

    private String extensionOf(String originalName) {
        if (originalName == null) {
            return "";
        }
        int dot = originalName.lastIndexOf('.');
        return (dot >= 0) ? originalName.substring(dot).toLowerCase() : "";
    }
}
