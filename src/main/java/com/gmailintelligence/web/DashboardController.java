package com.gmailintelligence.web;

import com.gmailintelligence.auth.GmailConnectionService;
import com.gmailintelligence.configuration.AppProperties;
import com.gmailintelligence.persistence.EmailChunkRepository;
import com.gmailintelligence.persistence.EmailRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class DashboardController {
    private final GmailConnectionService connectionService;
    private final EmailRepository emailRepository;
    private final EmailChunkRepository chunkRepository;
    private final AppProperties properties;

    DashboardController(
            GmailConnectionService connectionService,
            EmailRepository emailRepository,
            EmailChunkRepository chunkRepository,
            AppProperties properties) {
        this.connectionService = connectionService;
        this.emailRepository = emailRepository;
        this.chunkRepository = chunkRepository;
        this.properties = properties;
    }

    @GetMapping("/")
    String dashboard(Model model) {
        model.addAttribute("connectedEmail", connectionService.connectedEmail().orElse(null));
        model.addAttribute("emailCount", emailRepository.countEmails());
        model.addAttribute("chunkCount", chunkRepository.countChunks());
        model.addAttribute("gmailConfigured", properties.gmail().configured());
        model.addAttribute("aiConfigured", properties.ai().configured());
        return "dashboard";
    }
}
