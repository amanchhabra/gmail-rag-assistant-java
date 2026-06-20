package com.gmailintelligence.web;

import com.gmailintelligence.auth.GmailConnectionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
class GmailController {
    private final GmailConnectionService connectionService;

    GmailController(GmailConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    @GetMapping("/gmail")
    String gmail(Model model) {
        model.addAttribute("connectedEmail", connectionService.connectedEmail().orElse(null));
        return "gmail";
    }

    @PostMapping("/gmail/connect")
    String connect(HttpSession session) {
        return "redirect:" + connectionService.authorizationUrl(session);
    }

    @GetMapping("/oauth2/gmail/callback")
    String callback(HttpSession session, @RequestParam String state, @RequestParam String code) {
        connectionService.completeConnection(session, state, code);
        return "redirect:/gmail";
    }

    @PostMapping("/gmail/disconnect")
    String disconnect() {
        connectionService.disconnect();
        return "redirect:/gmail";
    }
}
