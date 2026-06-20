package com.gmailintelligence.web;

import com.gmailintelligence.ingestion.GmailImportService;
import com.gmailintelligence.ingestion.ImportJobService;
import com.gmailintelligence.ingestion.ImportJobStatus;
import com.gmailintelligence.ingestion.ImportResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@Controller
class ImportController {
    private final GmailImportService importService;
    private final ImportJobService jobService;

    ImportController(GmailImportService importService, ImportJobService jobService) {
        this.importService = importService;
        this.jobService = jobService;
    }

    @GetMapping("/import")
    String form(Model model) {
        addFormOptions(model, ImportPeriod.THIRTY_DAYS, 100);
        return "import";
    }

    @PostMapping("/import/start")
    @ResponseBody
    ResponseEntity<Map<String, String>> startImport(
            @RequestParam(defaultValue = "THIRTY_DAYS") ImportPeriod period,
            @RequestParam(defaultValue = "100") int maxMessages) {
        int limit = Math.max(1, Math.min(maxMessages, 500));
        try {
            UUID jobId = jobService.start(period.gmailQuery(), limit);
            return ResponseEntity.accepted().body(Map.of("jobId", jobId.toString()));
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);
        }
    }

    @GetMapping("/import/status/{jobId}")
    @ResponseBody
    ImportJobStatus importStatus(@PathVariable UUID jobId) {
        try {
            return jobService.status(jobId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }

    @PostMapping("/import")
    String importMessages(
            @RequestParam(defaultValue = "THIRTY_DAYS") ImportPeriod period,
            @RequestParam(defaultValue = "100") int maxMessages,
            Model model) {
        ImportResult result = importService.importMessages(
                period.gmailQuery(), Math.max(1, Math.min(maxMessages, 500)));
        model.addAttribute("result", result);
        addFormOptions(model, period, maxMessages);
        return "import";
    }

    private void addFormOptions(Model model, ImportPeriod period, int maxMessages) {
        model.addAttribute("periods", ImportPeriod.values());
        model.addAttribute("selectedPeriod", period);
        model.addAttribute("maxMessages", maxMessages);
    }
}
