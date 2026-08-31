package com.email_services.smart_email_ai_assistant;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class EmailGeneratorService {

    private final WebClient webClient;

    public EmailGeneratorService(
            WebClient.Builder webClientBuilder,
            @org.springframework.beans.factory.annotation.Value("${gemini_api.url}") String baseUrl) {

        System.out.println("========== DEBUG ==========");
        System.out.println("Base URL = " + baseUrl);
        System.out.println("===========================");

        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .build();
    }

    public String generateEmailReply(EmailRequest emailRequest) {

        System.out.println("Calling Gemini API...");

        // Get API key provided by the user
        String apiKey = emailRequest.getApiKey();

        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new RuntimeException(
                    "Gemini API key was not provided."
            );
        }

        // Build prompt
        String prompt = buildPrompt(emailRequest);

        // Build JSON safely
        String requestBody = requestBody(prompt);

        System.out.println("Sending request to Gemini API...");
        System.out.println("User API key received: YES");

        try {

            String response = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                        .path("/v1beta/models/gemini-3.6-flash:generateContent")
                        .build())
                    .header("x-goog-api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return extractResponseContent(response);

        } catch (WebClientResponseException e) {

            System.err.println(
                    "Gemini API Error: "
                            + e.getStatusCode()
                            + " - "
                            + e.getResponseBodyAsString()
            );

            if (e.getStatusCode().value() == 503) {

                throw new RuntimeException(
                        "Gemini is temporarily unavailable. Please try again in a few seconds."
                );

            } else if (e.getStatusCode().value() == 429) {

                throw new RuntimeException(
                        "Gemini API rate limit reached. Please try again later."
                );

            } else if (e.getStatusCode().value() == 401
                    || e.getStatusCode().value() == 403) {

                throw new RuntimeException(
                        "Invalid or unauthorized Gemini API key."
                );

            } else {

                throw new RuntimeException(
                        "Gemini API request failed: "
                                + e.getStatusCode()
                );
            }
        }
    }


private String extractResponseContent(String response) {

    try {

        ObjectMapper mapper = new ObjectMapper();

        JsonNode root = mapper.readTree(response);

        JsonNode candidates = root.path("candidates");

        if (!candidates.isArray() || candidates.isEmpty()) {
            throw new RuntimeException(
                    "Gemini returned no candidates. Response: " + response
            );
        }

        JsonNode parts =
                candidates.get(0)
                        .path("content")
                        .path("parts");

        if (!parts.isArray() || parts.isEmpty()) {
            throw new RuntimeException(
                    "Gemini response contains no text. Response: " + response
            );
        }

        return parts.get(0)
                .path("text")
                .asText();

    } catch (Exception e) {

        throw new RuntimeException(
                "Unable to extract Gemini response: " + e.getMessage(),
                e
        );
    }
}
private String requestBody(String prompt) {

    try {

        ObjectMapper mapper = new ObjectMapper();

        var root = mapper.createObjectNode();

        var contents = root.putArray("contents");

        var content = contents.addObject();

        var parts = content.putArray("parts");

        var part = parts.addObject();

        part.put("text", prompt);

        return mapper.writeValueAsString(root);

    } catch (Exception e) {

        throw new RuntimeException(
                "Unable to create Gemini request body",
                e
        );
    }
}


private String buildPrompt(EmailRequest emailRequest) {

    StringBuilder prompt = new StringBuilder();

    prompt.append("""
            You are an intelligent AI Email Reply Assistant.

            Your goal is to generate a high-quality email reply based on the email provided.

            Guidelines:

            • Understand the intent and context of the sender.
            • Write a complete and professional reply.
            • Use a polite and respectful tone.
            • Be grammatically correct and easy to read.
            • Keep the response between 80 and 150 words unless the original email requires a shorter or longer reply.
            • Do not invent facts that are not present in the original email.
            • Do not include a subject line.
            • Do not use Markdown or bullet points.
            • Return only the email body.

            """);

    /*
     * Apply selected tone
     */
    if (emailRequest.getTone() != null
            && !emailRequest.getTone().isBlank()) {

        prompt.append("Use a ")
                .append(emailRequest.getTone())
                .append(" tone.\n\n");
    }

    /*
     * Add optional custom instruction
     *
     * If user did not enter anything,
     * this section is simply skipped.
     */
    if (emailRequest.getCustomInstruction() != null
            && !emailRequest.getCustomInstruction().isBlank()) {

        prompt.append("Custom instruction from the user:\n")
                .append(emailRequest.getCustomInstruction())
                .append("\nFollow this instruction while generating the reply.\n\n");
    }

    /*
     * Add original email
     */
    prompt.append("Original Email:\n")
            .append(emailRequest.getEmailContent());

    /*
     * Final instruction
     */
    prompt.append("\n\nGenerate only the reply email.");

    return prompt.toString();
}

}
