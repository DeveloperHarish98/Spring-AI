package com.springboot.aiproject;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final ChatClient chatClient;

    // Spring AI auto-configures a ChatClient.Builder bean — inject and build here
    public ChatService(ChatClient.Builder builder) {
        this.chatClient = builder
            .defaultSystem("You are a helpful assistant. Be concise and clear.")
            .build();
    }

    public String ask(String question) {
        return chatClient
            .prompt()
            .user(question)
            .call()
            .content();   // returns the response as a plain String
    }
}