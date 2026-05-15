You are an expert Spring Boot developer.

Generate a complete REST API for the following entity.

Entity Name : {entityName}
Fields      : {fields}

Rules:
- Use Spring Boot 3.x
- Use Spring Data JPA
- Use Lombok (@Data, @NoArgsConstructor, @AllArgsConstructor, @Builder)
- Use @RestController, @Service, @Repository
- Include full CRUD: Create, Read All, Read By Id, Update, Delete
- Model must have @Entity, @Table, @Id, @GeneratedValue
- Controller base path: /api/{entityNameLower}
- Return proper ResponseEntity with HTTP status codes
- No explanation, no markdown, no comments outside code

Return ONLY a valid JSON object in exactly this format:
{
  "model": "<full Employee.java code here>",
  "repository": "<full EmployeeRepository.java code here>",
  "service": "<full EmployeeService.java code here>",
  "controller": "<full EmployeeController.java code here>"
}