package com.springboot.aiproject.apigenerator;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/generator")
public class ApiGeneratorController {

    private final ApiGeneratorService apiGeneratorService;

    public ApiGeneratorController(ApiGeneratorService apiGeneratorService) {
        this.apiGeneratorService = apiGeneratorService;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generate(@RequestBody ApiGeneratorRequest request) {
        try {
            GeneratedApi result = apiGeneratorService.generate(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity
                    .internalServerError()
                    .body("Generation failed: " + e.getMessage());
        }
    }
}
