package com.springboot.aiproject;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    // GET /api/ask?question=What+is+Spring+AI
    @GetMapping("/ask")
    public String ask(@RequestParam String question) {
        return chatService.ask(question);
    }

    // POST /api/ask   body: { "question": "..." }
    @PostMapping("/ask")
    public String askPost(@RequestBody AskRequest request) {
        return chatService.ask(request.question());
    }

    record AskRequest(String question) {}
}