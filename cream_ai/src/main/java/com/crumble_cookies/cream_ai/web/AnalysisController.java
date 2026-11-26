package com.crumble_cookies.cream_ai.web;

import com.crumble_cookies.cream_ai.model.AnalysisResult;
import com.crumble_cookies.cream_ai.model.ContradictionRecord;
import com.crumble_cookies.cream_ai.model.DuplicateRecord;
import com.crumble_cookies.cream_ai.model.Finding;
import com.crumble_cookies.cream_ai.model.Requirement;
import com.crumble_cookies.cream_ai.report.JsonReportWriter;
import com.crumble_cookies.cream_ai.report.MarkdownReportGenerator;
import com.crumble_cookies.cream_ai.web.dto.StatusResponse;
import com.crumble_cookies.cream_ai.web.dto.UploadResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * REST API для фронтенда: загрузка документа, статус анализа, результаты,
 * подробности по требованиям, системные замечания, скачивание отчёта.
 */
@RestController
@RequestMapping("/api/analyses")
public class AnalysisController {

    private final AnalysisService service;
    private final MarkdownReportGenerator markdownReportGenerator;
    private final JsonReportWriter jsonReportWriter;

    public AnalysisController(AnalysisService service,
                               MarkdownReportGenerator markdownReportGenerator,
                               JsonReportWriter jsonReportWriter) {
        this.service = service;
        this.markdownReportGenerator = markdownReportGenerator;
        this.jsonReportWriter = jsonReportWriter;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> upload(@RequestParam("file") MultipartFile file) {
        String id = service.submit(file);
        return ResponseEntity.ok(new UploadResponse(id));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<StatusResponse> status(@PathVariable String id) {
        return ResponseEntity.ok(StatusResponse.from(service.get(id)));
    }

    @GetMapping("/{id}/result")
    public ResponseEntity<AnalysisResult> result(@PathVariable String id) {
        return ResponseEntity.ok(requireDone(id));
    }

    @GetMapping("/{id}/requirements")
    public ResponseEntity<List<Requirement>> requirements(@PathVariable String id) {
        return ResponseEntity.ok(requireDone(id).getRequirements());
    }

    @GetMapping("/{id}/findings")
    public ResponseEntity<FindingsResponse> findings(@PathVariable String id) {
        AnalysisResult result = requireDone(id);
        return ResponseEntity.ok(new FindingsResponse(
                result.getIndividualFindings(),
                result.getSetLevelFindings(),
                result.getContradictions(),
                result.getDuplicates()));
    }

    @GetMapping(value = "/{id}/report", params = "format=json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> reportJson(@PathVariable String id) {
        AnalysisResult result = requireDone(id);
        String json = jsonReportWriter.toJson(result);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report-" + id + ".json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
    }

    @GetMapping(value = "/{id}/report", params = "!format", produces = "text/markdown")
    public ResponseEntity<byte[]> reportMarkdown(@PathVariable String id) {
        AnalysisResult result = requireDone(id);
        String md = markdownReportGenerator.generate(result);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report-" + id + ".md\"")
                .contentType(MediaType.parseMediaType("text/markdown; charset=UTF-8"))
                .body(md.getBytes(StandardCharsets.UTF_8));
    }

    private AnalysisResult requireDone(String id) {
        AnalysisJob job = service.get(id);
        if (job.getStatus() != AnalysisJob.Status.DONE) {
            throw new AnalysisNotReadyException(job.getStatus().name());
        }
        return job.getResult();
    }

    public record FindingsResponse(
            List<Finding> individualFindings,
            List<Finding> setLevelFindings,
            List<ContradictionRecord> contradictions,
            List<DuplicateRecord> duplicates
    ) {
    }
}
