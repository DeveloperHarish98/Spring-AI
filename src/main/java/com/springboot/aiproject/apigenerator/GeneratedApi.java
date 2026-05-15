package com.springboot.aiproject.apigenerator;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeneratedApi(
        String model,
        String dto,
        String repository,
        String service,
        String serviceImpl,
        String controller,
        String exception,
        String config
) {}