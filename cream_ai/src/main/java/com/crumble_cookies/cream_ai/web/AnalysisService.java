package com.crumble_cookies.cream_ai.web;

import com.crumble_cookies.cream_ai.analysis.AnalysisOrchestrator;
import com.crumble_cookies.cream_ai.llm.LLMException;
import com.crumble_cookies.cream_ai.parser.DocumentParsingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Обрабатывает загрузку документа и запускает анализ асинхронно, храня
 * состояние прогонов в памяти (для хакатона достаточно — без БД).
 * Реальные результаты никогда не подменяются фиктивными: при ошибке LLM
 * или парсинга задача переходит в статус FAILED с понятным сообщением.
 */
@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".md", ".markdown", ".docx", ".txt");
    private static final long MAX_FILE_SIZE_BYTES = 25L * 1024 * 1024;

    private final AnalysisOrchestrator orchestrator;
    private final TaskExecutor analysisExecutor;
    private final Map<String, AnalysisJob> jobs = new ConcurrentHashMap<>();
    private final Path storageDir;

    public AnalysisService(AnalysisOrchestrator orchestrator, TaskExecutor analysisExecutor) {
        this.orchestrator = orchestrator;
        this.analysisExecutor = analysisExecutor;
        try {
            this.storageDir = Files.createTempDirectory("ria-uploads-");
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось создать временную директорию для загрузок", e);
        }
    }

    public String submit(MultipartFile file) {
        validate(file);
        String id = UUID.randomUUID().toString();
        String originalName = sanitizeFileName(file.getOriginalFilename());

        Path dest = storageDir.resolve(id + "-" + originalName);
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            throw new UnsupportedFileException("Не удалось сохранить загруженный файл: " + e.getMessage());
        }

        AnalysisJob job = new AnalysisJob(id, originalName);
        jobs.put(id, job);

        analysisExecutor.execute(() -> runAnalysis(job, dest, originalName));
        return id;
    }

    private void runAnalysis(AnalysisJob job, Path filePath, String fileName) {
        try {
            var result = orchestrator.run(filePath, fileName, job::setStageFromOrchestrator);
            job.setResult(result);
            job.setStatus(AnalysisJob.Status.DONE);
        } catch (DocumentParsingException e) {
            log.warn("Ошибка разбора документа для задачи {}: {}", job.getId(), e.getMessage());
            job.fail("DOCUMENT_PARSING_ERROR", e.getMessage());
        } catch (LLMException e) {
            log.warn("Ошибка LLM для задачи {}: {} ({})", job.getId(), e.getMessage(), e.kind());
            job.fail("LLM_ERROR_" + e.kind(), e.getMessage());
        } catch (Exception e) {
            log.error("Непредвиденная ошибка анализа для задачи {}", job.getId(), e);
            job.fail("INTERNAL_ERROR", "Непредвиденная ошибка: " + e.getMessage());
        } finally {
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException ignored) {
            }
        }
    }

    public AnalysisJob get(String id) {
        AnalysisJob job = jobs.get(id);
        if (job == null) {
            throw new AnalysisNotFoundException(id);
        }
        return job;
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new UnsupportedFileException("Файл не передан или пуст.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new UnsupportedFileException("Файл превышает максимально допустимый размер 25 МБ.");
        }
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank()) {
            throw new UnsupportedFileException("Имя файла не указано.");
        }
        String lower = name.toLowerCase(Locale.ROOT);
        boolean supported = ALLOWED_EXTENSIONS.stream().anyMatch(lower::endsWith);
        if (!supported) {
            throw new UnsupportedFileException("Неподдерживаемый формат файла. Поддерживаются: PDF, Markdown, DOCX, TXT.");
        }
    }

    private String sanitizeFileName(String name) {
        if (name == null) return "document";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
