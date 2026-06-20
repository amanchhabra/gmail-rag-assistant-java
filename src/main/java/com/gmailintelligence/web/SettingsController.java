package com.gmailintelligence.web;

import com.gmailintelligence.persistence.EmailRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
class SettingsController {
    private final EmailRepository emailRepository;

    SettingsController(EmailRepository emailRepository) {
        this.emailRepository = emailRepository;
    }

    @GetMapping("/settings")
    String settings() {
        return "settings";
    }

    @PostMapping("/settings/delete-data")
    String deleteData() {
        emailRepository.deleteAllImportedData();
        return "redirect:/settings?deleted";
    }
}
