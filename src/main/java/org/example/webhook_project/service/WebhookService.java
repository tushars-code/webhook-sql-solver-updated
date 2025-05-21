package org.example.webhook_project.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class WebhookService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public WebhookService(WebClient webClient) {
        this.webClient = webClient;
        this.objectMapper = new ObjectMapper();
    }

    public void execute() {
        String registrationJson = """
                {
                    "name": "John Doe",
                    "regNo": "REG12347",
                    "email": "john@example.com"
                }
                """;

        webClient.post()
                .uri("https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA")
                .header("Content-Type", "application/json")
                .body(BodyInserters.fromValue(registrationJson))
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(response -> {
                    try {
                        JsonNode jsonNode = objectMapper.readTree(response);
                        String accessToken = jsonNode.get("accessToken").asText();
                        String webhookUrl = jsonNode.get("webhook").asText();

                        String finalQuery = """
                                SELECT 
                                    p.AMOUNT AS SALARY,
                                    CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME) AS NAME,
                                    TIMESTAMPDIFF(YEAR, e.DOB, CURDATE()) AS AGE,
                                    d.DEPARTMENT_NAME
                                FROM PAYMENTS p
                                JOIN EMPLOYEE e ON p.EMP_ID = e.EMP_ID
                                JOIN DEPARTMENT d ON e.DEPARTMENT = d.DEPARTMENT_ID
                                WHERE DAY(p.PAYMENT_TIME) != 1
                                ORDER BY p.AMOUNT DESC
                                LIMIT 1;
                                """;

                        String submitBody = String.format("""
                                {
                                    "finalQuery": "%s"
                                }
                                """, finalQuery.replace("\"", "\\\"").replace("\n", "\\n"));

                        return webClient.post()
                                .uri(webhookUrl)
                                .header("Authorization", accessToken)
                                .header("Content-Type", "application/json")
                                .body(BodyInserters.fromValue(submitBody))
                                .retrieve()
                                .bodyToMono(String.class);

                    } catch (Exception e) {
                        return Mono.error(new RuntimeException("Failed to parse response", e));
                    }
                })
                .doOnSuccess(resp -> System.out.println("✅ Submission Response: " + resp))
                .doOnError(err -> System.err.println("❌ Error: " + err.getMessage()))
                .subscribe();
    }
}
