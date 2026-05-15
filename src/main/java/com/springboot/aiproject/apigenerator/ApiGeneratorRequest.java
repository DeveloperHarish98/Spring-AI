package com.springboot.aiproject.apigenerator;

import java.util.List;

public record ApiGeneratorRequest(
        String entityName,      // e.g. "Employee"
        List<String> fields     // e.g. ["id:Long", "name:String", "salary:Double"]
) {}