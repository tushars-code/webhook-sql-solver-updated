package org.example.webhook_project;

import jakarta.annotation.PostConstruct;
import org.example.webhook_project.service.WebhookService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WebhookProjectApplication {

    private final WebhookService webhookService;

    public WebhookProjectApplication(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    public static void main(String[] args) {
        SpringApplication.run(WebhookProjectApplication.class, args);
    }

    @PostConstruct
    public void triggerWebhookFlow() {
        webhookService.execute();
    }
}
