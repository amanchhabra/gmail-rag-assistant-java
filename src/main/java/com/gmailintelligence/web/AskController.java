package com.gmailintelligence.web;

import com.gmailintelligence.rag.EmailAnswerService;
import com.gmailintelligence.rag.EmailQuestion;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
class AskController {
    private final EmailAnswerService answerService;

    AskController(EmailAnswerService answerService) {
        this.answerService = answerService;
    }

    @GetMapping("/ask")
    String ask() {
        return "ask";
    }

    @PostMapping("/ask")
    String answer(@RequestParam String question, Model model) {
        model.addAttribute("question", question);
        model.addAttribute("answer", answerService.answer(new EmailQuestion(question)));
        return "ask";
    }
}
