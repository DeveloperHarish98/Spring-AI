package com.springboot.aiproject.apigenerator;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApiGeneratorService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public ApiGeneratorService(ChatClient.Builder builder, ObjectMapper objectMapper) {
        this.chatClient = builder
                .defaultSystem("""
                        You are a Spring Boot 3.x code generator.
                        Always return pure JSON only.
                        Never wrap response in markdown or code blocks.
                        Never add any explanation text outside the JSON.
                        Always use jakarta.persistence, never javax.persistence.
                        Always include the package statement in every Java file.
                        Keep code concise — avoid unnecessary comments.
                        """)
                .build();

        // Configure lenient ObjectMapper
        this.objectMapper = objectMapper
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true)
                .configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);
    }

    public GeneratedApi generate(ApiGeneratorRequest request) throws Exception {

        String fieldsFormatted = formatFields(request.fields());
        String basePackage = this.getClass().getPackageName().replace(".apigenerator", "");
        String entityLower = request.entityName().toLowerCase();

        String prompt = """
                Generate a complete layered Spring Boot REST API for the following entity.
                
                Entity Name  : %s
                Fields       : %s
                Base Package : %s

                Folder/Package Structure:
                - Model      : %s.model (Use @Entity, JPA)
                - DTO        : %s.dto (Use for request/response)
                - Repository : %s.repository (JpaRepository)
                - Service    : %s.service (Interface)
                - ServiceImpl: %s.service.impl (Implementation of Service)
                - Controller : %s.controller (REST Controller)
                - Exception  : %s.exception (GlobalExceptionHandler or Custom Exception)
                - Config     : %s.config (Any necessary configuration)

                Rules:
                - Spring Boot 3.x, Spring Data JPA, Lombok
                - Use jakarta.persistence (NOT javax.persistence)
                - Include proper package statements and imports in EVERY file.
                - Controller base path: /api/%s
                - Use ResponseEntity with correct HTTP status codes.
                - No markdown, no code blocks, no explanation outside JSON.
                - Keep code concise.

                Return ONLY a valid compact JSON in exactly this structure:
                {"model":"...","dto":"...","repository":"...","service":"...","serviceImpl":"...","controller":"...","exception":"...","config":"..."}
                """.formatted(request.entityName(), fieldsFormatted, basePackage, 
                              basePackage, basePackage, basePackage, basePackage, 
                              basePackage, basePackage, basePackage, basePackage, entityLower);

        String rawResponse = chatClient
                .prompt()
                .user(prompt)
                .call()
                .content();

        String cleanJson = cleanResponse(rawResponse);
        GeneratedApi generatedApi;

        try {
            generatedApi = objectMapper.readValue(cleanJson, GeneratedApi.class);
        } catch (Exception e) {
            // Attempt partial recovery if JSON is truncated
            generatedApi = recoverPartialJson(cleanJson, rawResponse);
        }

        // 💾 Save files to the local project directory
        String baseDir = "src/main/java/" + basePackage.replace(".", "/") + "/";
        saveFiles(generatedApi, request.entityName(), baseDir);

        return generatedApi;
    }

    private void saveFiles(GeneratedApi api, String entityName, String baseDir) {
        try {
            // Save to layered structure
            saveToLayer(baseDir + "model/", entityName + ".java", api.model());
            saveToLayer(baseDir + "dto/", entityName + "DTO.java", api.dto());
            saveToLayer(baseDir + "repository/", entityName + "Repository.java", api.repository());
            saveToLayer(baseDir + "service/", entityName + "Service.java", api.service());
            saveToLayer(baseDir + "service/impl/", entityName + "ServiceImpl.java", api.serviceImpl());
            saveToLayer(baseDir + "controller/", entityName + "Controller.java", api.controller());
            saveToLayer(baseDir + "exception/", "GlobalExceptionHandler.java", api.exception());
            saveToLayer(baseDir + "config/", "ProjectConfig.java", api.config());

        } catch (Exception e) {
            throw new RuntimeException("Failed to save generated files: " + e.getMessage());
        }
    }

    private void saveToLayer(String dirPath, String fileName, String content) throws Exception {
        if (content == null || content.trim().isEmpty() || content.equalsIgnoreCase("null")) {
            return;
        }
        java.nio.file.Path path = java.nio.file.Paths.get(dirPath);
        if (!java.nio.file.Files.exists(path)) {
            java.nio.file.Files.createDirectories(path);
        }
        java.nio.file.Files.writeString(path.resolve(fileName), content);
    }

    // If JSON is truncated, recover whatever fields were parsed successfully
    private GeneratedApi recoverPartialJson(String cleanJson, String raw) {
        String model      = extractField(raw, "model");
        String dto        = extractField(raw, "dto");
        String repository = extractField(raw, "repository");
        String service    = extractField(raw, "service");
        String serviceImpl = extractField(raw, "serviceImpl");
        String controller = extractField(raw, "controller");
        String exception  = extractField(raw, "exception");
        String config     = extractField(raw, "config");

        if (model == null && repository == null && service == null && controller == null) {
            throw new RuntimeException("AI response could not be parsed.\nRaw:\n" + raw);
        }
        return new GeneratedApi(model, dto, repository, service, serviceImpl, controller, exception, config);
    }

    // Extract a JSON string field value even from truncated JSON
    private String extractField(String json, String fieldName) {
        try {
            String key = "\"" + fieldName + "\":\"";
            int start = json.indexOf(key);
            if (start == -1) return null;
            start += key.length();

            // Walk through chars, respect escape sequences
            StringBuilder sb = new StringBuilder();
            int i = start;
            while (i < json.length()) {
                char c = json.charAt(i);
                if (c == '\\' && i + 1 < json.length()) {
                    char next = json.charAt(i + 1);
                    switch (next) {
                        case 'n'  -> sb.append('\n');
                        case 't'  -> sb.append('\t');
                        case 'r'  -> sb.append('\r');
                        case '"'  -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        default   -> sb.append(next);
                    }
                    i += 2;
                } else if (c == '"') {
                    break; // end of field value
                } else {
                    sb.append(c);
                    i++;
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String formatFields(List<String> fields) {
        return String.join(", ", fields.stream()
                .map(f -> {
                    String[] parts = f.split(":");
                    return parts.length == 2
                            ? parts[0].trim() + " (" + parts[1].trim() + ")"
                            : f;
                })
                .toList());
    }

    private String cleanResponse(String raw) {
        String cleaned = raw.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned
                    .replaceAll("(?s)```json\\s*", "")
                    .replaceAll("(?s)```\\s*", "")
                    .trim();
        }
        int start = cleaned.indexOf('{');
        int end   = cleaned.lastIndexOf('}');
        if (start != -1 && end != -1 && end > start) {
            cleaned = cleaned.substring(start, end + 1);
        }
        return cleaned;
    }
}