//package com.email_services.smart_email_ai_assistant;
//
//import org.springframework.stereotype.Service;
//import org.springframework.web.reactive.function.client.WebClient;
//import org.springframework.web.reactive.function.client.WebClientResponseException;
//
//import tools.jackson.databind.JsonNode;
//import tools.jackson.databind.ObjectMapper;
//
//@Service
//public class EmailGeneratorService {
//
//    private final WebClient webClient;
//
//    public EmailGeneratorService(
//            WebClient.Builder webClientBuilder,
//            @org.springframework.beans.factory.annotation.Value("${gemini_api.url}")
//            String baseUrl) {
//
//        System.out.println("========== DEBUG ==========");
//        System.out.println("Base URL = " + baseUrl);
//        System.out.println("===========================");
//
//        this.webClient = webClientBuilder
//                .baseUrl(baseUrl)
//                .build();
//    }
//
//    /**
//     * Generates:
//     * - Intent
//     * - Priority
//     * - Sentiment
//     * - Confidence
//     * - Key points
//     * - Email reply
//     */
//    public EmailAnalysisResponse generateEmailReply(
//            EmailRequest emailRequest) {
//
//        System.out.println("Calling Gemini API...");
//
//        // -----------------------------------------
//        // 1. Get API key provided by user
//        // -----------------------------------------
//
//        String apiKey = emailRequest.getApiKey();
//
//        if (apiKey == null || apiKey.trim().isEmpty()) {
//
//            throw new RuntimeException(
//                    "Gemini API key was not provided."
//            );
//        }
//
//        // -----------------------------------------
//        // 2. Validate email content
//        // -----------------------------------------
//
//        if (emailRequest.getEmailContent() == null
//                || emailRequest.getEmailContent().trim().isEmpty()) {
//
//            throw new RuntimeException(
//                    "Email content was not provided."
//            );
//        }
//
//        // -----------------------------------------
//        // 3. Build AI prompt
//        // -----------------------------------------
//
//        String prompt = buildPrompt(emailRequest);
//
//        // -----------------------------------------
//        // 4. Build Gemini JSON request body
//        // -----------------------------------------
//
//        String requestBody = requestBody(prompt);
//
//        System.out.println("Sending request to Gemini API...");
//        System.out.println("User API key received: YES");
//
//        // -----------------------------------------
//        // 5. Call Gemini
//        // -----------------------------------------
//
//        try {
//
//            String response = webClient.post()
//                    .uri(uriBuilder -> uriBuilder
//                            .path("/v1beta/models/gemini-3.6-flash:generateContent")
//                            .build())
//                    .header("x-goog-api-key", apiKey)
//                    .header("Content-Type", "application/json")
//                    .bodyValue(requestBody)
//                    .retrieve()
//                    .bodyToMono(String.class)
//                    .block();
//
//            // -----------------------------------------
//            // 6. Extract Gemini text
//            // -----------------------------------------
//
//            String aiResponse =
//                    extractResponseContent(response);
//
//            // -----------------------------------------
//            // 7. Parse AI JSON
//            // -----------------------------------------
//
//            return parseAnalysisResponse(aiResponse);
//
//        } catch (WebClientResponseException e) {
//
//            System.err.println(
//                    "Gemini API Error: "
//                            + e.getStatusCode()
//                            + " - "
//                            + e.getResponseBodyAsString()
//            );
//
//            int statusCode = e.getStatusCode().value();
//
//            if (statusCode == 503) {
//
//                throw new RuntimeException(
//                        "Gemini is temporarily unavailable. "
//                                + "Please try again in a few seconds."
//                );
//
//            } else if (statusCode == 429) {
//
//                throw new RuntimeException(
//                        "Gemini API rate limit reached. "
//                                + "Please try again later."
//                );
//
//            } else if (statusCode == 401
//                    || statusCode == 403) {
//
//                throw new RuntimeException(
//                        "Invalid or unauthorized Gemini API key."
//                );
//
//            } else {
//
//                throw new RuntimeException(
//                        "Gemini API request failed: "
//                                + e.getStatusCode()
//                );
//            }
//
//        } catch (Exception e) {
//
//            System.err.println(
//                    "Unexpected Gemini error: "
//                            + e.getMessage()
//            );
//
//            throw new RuntimeException(
//                    "Unable to generate AI email response: "
//                            + e.getMessage(),
//                    e
//            );
//        }
//    }
//
//    // =====================================================
//    // BUILD AI PROMPT
//    // =====================================================
//
//    private String buildPrompt(
//            EmailRequest emailRequest) {
//
//        StringBuilder prompt = new StringBuilder();
//
//        prompt.append("""
//                You are an intelligent AI Email Analysis and Reply Assistant.
//
//                Your job is to analyze an email and generate an appropriate reply.
//
//                You MUST perform these tasks:
//
//                1. Determine the primary intent of the email.
//                2. Determine the priority of the email.
//                3. Determine the sentiment of the sender.
//                4. Provide a confidence score between 0 and 1.
//                5. Extract 2 to 5 important points.
//                6. Generate a suitable email reply.
//
//                -----------------------------
//                INTENT CLASSIFICATION
//                -----------------------------
//
//                INTENT must be exactly one of:
//
//                MEETING_REQUEST
//                INFORMATION_REQUEST
//                JOB_OPPORTUNITY
//                JOB_APPLICATION
//                CUSTOMER_COMPLAINT
//                FOLLOW_UP
//                THANK_YOU
//                INFORMATION_PROVIDED
//                BUSINESS_PROPOSAL
//                INTERVIEW_INVITATION
//                CANCELLATION
//                REQUEST
//                URGENT_REQUEST
//                OTHER
//
//                -----------------------------
//                PRIORITY CLASSIFICATION
//                -----------------------------
//
//                PRIORITY must be exactly one of:
//
//                LOW
//                MEDIUM
//                HIGH
//                URGENT
//
//                Priority guidelines:
//
//                LOW:
//                Normal informational emails with no urgency.
//
//                MEDIUM:
//                Emails requiring a response but without a strict deadline.
//
//                HIGH:
//                Emails involving important deadlines, complaints,
//                business decisions, meetings, or time-sensitive requests.
//
//                URGENT:
//                Emails explicitly indicating emergencies,
//                immediate action, critical problems, or extremely
//                time-sensitive situations.
//
//                Do NOT classify a normal email as URGENT.
//
//                -----------------------------
//                SENTIMENT CLASSIFICATION
//                -----------------------------
//
//                SENTIMENT must be exactly one of:
//
//                POSITIVE
//                NEUTRAL
//                NEGATIVE
//                MIXED
//
//                -----------------------------
//                ANALYSIS GUIDELINES
//                -----------------------------
//
//                • Base the analysis only on the provided email.
//                • Do not invent facts.
//                • Do not assume information that is not present.
//                • Detect urgency from the actual email.
//                • Identify the sender's emotional tone.
//                • Extract only meaningful information.
//                • Confidence must be between 0 and 1.
//                • Extract between 2 and 5 key points.
//
//                -----------------------------
//                REPLY GUIDELINES
//                -----------------------------
//
//                • Generate a natural email reply.
//                • Follow the requested tone.
//                • Respond to the actual intent of the sender.
//                • Do not invent commitments.
//                • Do not invent dates.
//                • Do not invent prices.
//                • Do not invent names.
//                • Do not invent facts.
//                • Do not include a subject line.
//                • Do not use Markdown.
//                • Keep the reply concise.
//                • Preserve a professional email structure.
//
//                -----------------------------
//                REQUIRED JSON FORMAT
//                -----------------------------
//
//                Return ONLY valid JSON.
//
//                Do NOT return:
//                ```json
//
//                Do NOT return:
//                ```
//
//                Return exactly this structure:
//
//                {
//                  "intent": "MEETING_REQUEST",
//                  "priority": "HIGH",
//                  "sentiment": "POSITIVE",
//                  "confidence": 0.94,
//                  "keyPoints": [
//                    "Important point 1",
//                    "Important point 2"
//                  ],
//                  "reply": "Generated email reply"
//                }
//
//                """);
//
//        // -----------------------------------------
//        // Requested tone
//        // -----------------------------------------
//
//        if (emailRequest.getTone() != null
//                && !emailRequest.getTone().isBlank()) {
//
//            prompt.append(
//                    "Requested reply tone: "
//            );
//
//            prompt.append(
//                    emailRequest.getTone()
//            );
//
//            prompt.append("\n\n");
//        }
//
//        // -----------------------------------------
//        // Custom instruction
//        // -----------------------------------------
//
//        if (emailRequest.getCustomInstruction() != null
//                && !emailRequest.getCustomInstruction().isBlank()) {
//
//            prompt.append(
//                    "Custom instruction from the user:\n"
//            );
//
//            prompt.append(
//                    emailRequest.getCustomInstruction()
//            );
//
//            prompt.append("\n\n");
//        }
//
//        // -----------------------------------------
//        // Email content
//        // -----------------------------------------
//
//        prompt.append(
//                "EMAIL TO ANALYZE:\n\n"
//        );
//
//        prompt.append(
//                emailRequest.getEmailContent()
//        );
//
//        return prompt.toString();
//    }
//
//    // =====================================================
//    // BUILD GEMINI REQUEST BODY
//    // =====================================================
//
//    private String requestBody(String prompt) {
//
//        try {
//
//            ObjectMapper mapper = new ObjectMapper();
//
//            JsonNode root = mapper.createObjectNode();
//
//            var contents =
//                    ((tools.jackson.databind.node.ObjectNode) root)
//                            .putArray("contents");
//
//            var content =
//                    contents.addObject();
//
//            var parts =
//                    content.putArray("parts");
//
//            var part =
//                    parts.addObject();
//
//            part.put("text", prompt);
//
//            return mapper.writeValueAsString(root);
//
//        } catch (Exception e) {
//
//            throw new RuntimeException(
//                    "Unable to create Gemini request body",
//                    e
//            );
//        }
//    }
//
//    // =====================================================
//    // EXTRACT GEMINI RESPONSE
//    // =====================================================
//
//    private String extractResponseContent(
//            String response) {
//
//        try {
//
//            ObjectMapper mapper = new ObjectMapper();
//
//            JsonNode root =
//                    mapper.readTree(response);
//
//            JsonNode candidates =
//                    root.path("candidates");
//
//            if (!candidates.isArray()
//                    || candidates.isEmpty()) {
//
//                throw new RuntimeException(
//                        "Gemini returned no candidates. "
//                                + "Response: "
//                                + response
//                );
//            }
//
//            JsonNode parts =
//                    candidates
//                            .get(0)
//                            .path("content")
//                            .path("parts");
//
//            if (!parts.isArray()
//                    || parts.isEmpty()) {
//
//                throw new RuntimeException(
//                        "Gemini response contains no text. "
//                                + "Response: "
//                                + response
//                );
//            }
//
//            return parts
//                    .get(0)
//                    .path("text")
//                    .asText();
//
//        } catch (Exception e) {
//
//            throw new RuntimeException(
//                    "Unable to extract Gemini response: "
//                            + e.getMessage(),
//                    e
//            );
//        }
//    }
//
//    // =====================================================
//    // PARSE AI ANALYSIS JSON
//    // =====================================================
//
//    private EmailAnalysisResponse parseAnalysisResponse(
//            String response) {
//
//        try {
//
//            ObjectMapper mapper =
//                    new ObjectMapper();
//
//            // Remove Markdown code fences if Gemini
//            // accidentally returns them.
//
//            String cleanedResponse =
//                    response
//                            .replace("```json", "")
//                            .replace("```JSON", "")
//                            .replace("```", "")
//                            .trim();
//
//            JsonNode root =
//                    mapper.readTree(cleanedResponse);
//
//            // -----------------------------------------
//            // Intent
//            // -----------------------------------------
//
//            String intent =
//                    root.path("intent").asText();
//
//            // -----------------------------------------
//            // Priority
//            // -----------------------------------------
//
//            String priority =
//                    root.path("priority").asText();
//
//            // -----------------------------------------
//            // Sentiment
//            // -----------------------------------------
//
//            String sentiment =
//                    root.path("sentiment").asText();
//
//            // -----------------------------------------
//            // Confidence
//            // -----------------------------------------
//
//            double confidence =
//                    root.path("confidence").asDouble();
//
//            // -----------------------------------------
//            // Key points
//            // -----------------------------------------
//
//            java.util.List<String> keyPoints =
//                    new java.util.ArrayList<>();
//
//            JsonNode keyPointsNode =
//                    root.path("keyPoints");
//
//            if (keyPointsNode.isArray()) {
//
//                for (JsonNode point : keyPointsNode) {
//
//                    keyPoints.add(
//                            point.asText()
//                    );
//                }
//            }
//
//            // -----------------------------------------
//            // Generated reply
//            // -----------------------------------------
//
//            String reply =
//                    root.path("reply").asText();
//
//            // -----------------------------------------
//            // Basic validation
//            // -----------------------------------------
//
//            if (intent.isBlank()) {
//
//                throw new RuntimeException(
//                        "AI response did not contain intent."
//                );
//            }
//
//            if (priority.isBlank()) {
//
//                throw new RuntimeException(
//                        "AI response did not contain priority."
//                );
//            }
//
//            if (sentiment.isBlank()) {
//
//                throw new RuntimeException(
//                        "AI response did not contain sentiment."
//                );
//            }
//
//            if (reply.isBlank()) {
//
//                throw new RuntimeException(
//                        "AI response did not contain a reply."
//                );
//            }
//
//            // Make sure confidence stays in valid range.
//
//            confidence =
//                    Math.max(
//                            0.0,
//                            Math.min(
//                                    1.0,
//                                    confidence
//                            )
//                    );
//
//            return new EmailAnalysisResponse(
//                    intent,
//                    priority,
//                    sentiment,
//                    confidence,
//                    keyPoints,
//                    reply
//            );
//
//        } catch (Exception e) {
//
//            throw new RuntimeException(
//                    "Unable to parse AI analysis response: "
//                            + e.getMessage(),
//                    e
//            );
//        }
//    }
//}


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
            @org.springframework.beans.factory.annotation.Value("${gemini_api.url}")
            String baseUrl) {

        System.out.println("========== DEBUG ==========");
        System.out.println("Base URL = " + baseUrl);
        System.out.println("===========================");

        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * Generates:
     * - Intent
     * - Priority
     * - Sentiment
     * - Confidence
     * - Key points
     * - Context-aware email reply
     */
    public EmailAnalysisResponse generateEmailReply(
            EmailRequest emailRequest) {

        System.out.println("Calling Gemini API...");

        // -----------------------------------------
        // 1. Get API key provided by user
        // -----------------------------------------

        String apiKey = emailRequest.getApiKey();

        if (apiKey == null || apiKey.trim().isEmpty()) {

            throw new RuntimeException(
                    "Gemini API key was not provided."
            );
        }

        // -----------------------------------------
        // 2. Validate thread content
        // -----------------------------------------

        if (emailRequest.getThreadContent() == null
                || emailRequest.getThreadContent().trim().isEmpty()) {

            throw new RuntimeException(
                    "Email thread content was not provided."
            );
        }

        // -----------------------------------------
        // 3. Validate latest message
        // -----------------------------------------

        if (emailRequest.getLatestMessage() == null
                || emailRequest.getLatestMessage().trim().isEmpty()) {

            throw new RuntimeException(
                    "Latest email message was not provided."
            );
        }

        // -----------------------------------------
        // 4. Build AI prompt
        // -----------------------------------------

        String prompt = buildPrompt(emailRequest);

        // -----------------------------------------
        // 5. Build Gemini JSON request body
        // -----------------------------------------

        String requestBody = requestBody(prompt);

        System.out.println("Sending request to Gemini API...");
        System.out.println("User API key received: YES");

        // -----------------------------------------
        // 6. Call Gemini
        // -----------------------------------------

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

            // -----------------------------------------
            // 7. Extract Gemini text
            // -----------------------------------------

            String aiResponse =
                    extractResponseContent(response);

            // -----------------------------------------
            // 8. Parse AI JSON
            // -----------------------------------------

            return parseAnalysisResponse(aiResponse);

        } catch (WebClientResponseException e) {

            System.err.println(
                    "Gemini API Error: "
                            + e.getStatusCode()
                            + " - "
                            + e.getResponseBodyAsString()
            );

            int statusCode = e.getStatusCode().value();

            if (statusCode == 503) {

                throw new RuntimeException(
                        "Gemini is temporarily unavailable. "
                                + "Please try again in a few seconds."
                );

            } else if (statusCode == 429) {

                throw new RuntimeException(
                        "Gemini API rate limit reached. "
                                + "Please try again later."
                );

            } else if (statusCode == 401
                    || statusCode == 403) {

                throw new RuntimeException(
                        "Invalid or unauthorized Gemini API key."
                );

            } else {

                throw new RuntimeException(
                        "Gemini API request failed: "
                                + e.getStatusCode()
                );
            }

        } catch (Exception e) {

            System.err.println(
                    "Unexpected Gemini error: "
                            + e.getMessage()
            );

            throw new RuntimeException(
                    "Unable to generate AI email response: "
                            + e.getMessage(),
                    e
            );
        }
    }

    // =====================================================
    // BUILD AI PROMPT
    // =====================================================

    private String buildPrompt(
            EmailRequest emailRequest) {

        StringBuilder prompt = new StringBuilder();

        prompt.append("""
                You are an intelligent AI Email Analysis and Reply Assistant.

                You are analyzing an EMAIL CONVERSATION THREAD.

                Your job is to understand the conversation history,
                identify what the latest message means in context,
                and generate an appropriate reply.

                IMPORTANT:

                The conversation may contain multiple messages.

                Earlier messages provide context.

                The LATEST MESSAGE is the message that requires a response.

                You MUST understand the previous conversation before
                generating the reply.

                -----------------------------------------
                THREAD INTELLIGENCE
                -----------------------------------------

                Use the conversation history to identify:

                • What the participants discussed previously.
                • Requests made earlier.
                • Commitments or promises made earlier.
                • Dates or deadlines mentioned earlier.
                • Questions that may still be unresolved.
                • Follow-up messages.
                • References such as "it", "that", "the report",
                  "the meeting", etc.

                Resolve references using the previous conversation
                whenever possible.

                Do NOT invent missing information.

                -----------------------------------------
                ANALYSIS TASKS
                -----------------------------------------

                You MUST perform these tasks:

                1. Determine the primary intent of the LATEST MESSAGE.
                2. Determine the priority of the LATEST MESSAGE.
                3. Determine the sentiment of the sender.
                4. Provide a confidence score between 0 and 1.
                5. Extract 2 to 5 important points from the conversation
                   that are relevant to the latest message.
                6. Generate a suitable reply to the LATEST MESSAGE.

                -----------------------------------------
                INTENT CLASSIFICATION
                -----------------------------------------

                INTENT must be exactly one of:

                MEETING_REQUEST
                INFORMATION_REQUEST
                JOB_OPPORTUNITY
                JOB_APPLICATION
                CUSTOMER_COMPLAINT
                FOLLOW_UP
                THANK_YOU
                INFORMATION_PROVIDED
                BUSINESS_PROPOSAL
                INTERVIEW_INVITATION
                CANCELLATION
                REQUEST
                URGENT_REQUEST
                OTHER

                IMPORTANT:

                If the latest message is checking on a previous
                request, promise, task, or unanswered question,
                consider FOLLOW_UP.

                -----------------------------------------
                PRIORITY CLASSIFICATION
                -----------------------------------------

                PRIORITY must be exactly one of:

                LOW
                MEDIUM
                HIGH
                URGENT

                Priority guidelines:

                LOW:
                Normal informational emails with no urgency.

                MEDIUM:
                Emails requiring a response but without a strict deadline.

                HIGH:
                Emails involving important deadlines, complaints,
                business decisions, meetings, or time-sensitive requests.

                URGENT:
                Emails explicitly indicating emergencies,
                immediate action, critical problems, or extremely
                time-sensitive situations.

                Do NOT classify a normal email as URGENT.

                -----------------------------------------
                SENTIMENT CLASSIFICATION
                -----------------------------------------

                SENTIMENT must be exactly one of:

                POSITIVE
                NEUTRAL
                NEGATIVE
                MIXED

                Determine sentiment primarily from the latest message,
                while using conversation history for context.

                -----------------------------------------
                ANALYSIS GUIDELINES
                -----------------------------------------

                • Use the entire conversation for context.
                • Focus the classification on the latest message.
                • Do not invent facts.
                • Do not assume information that is not present.
                • Detect urgency from the actual conversation.
                • Identify the sender's emotional tone.
                • Extract only meaningful information.
                • Confidence must be between 0 and 1.
                • Extract between 2 and 5 key points.
                • Resolve references using previous messages when possible.

                -----------------------------------------
                REPLY GUIDELINES
                -----------------------------------------

                • Reply specifically to the LATEST MESSAGE.
                • Use previous messages to understand context.
                • Maintain consistency with previous commitments.
                • If the sender is following up on something,
                  acknowledge that context.
                • Do not invent commitments.
                • Do not invent dates.
                • Do not invent prices.
                • Do not invent names.
                • Do not invent facts.
                • Do not contradict the conversation history.
                • Do not include a subject line.
                • Do not use Markdown.
                • Keep the reply concise.
                • Preserve a professional email structure.

                -----------------------------------------
                REQUIRED JSON FORMAT
                -----------------------------------------

                Return ONLY valid JSON.

                Do NOT return:
                ```json

                Do NOT return:
                ```

                Return exactly this structure:

                {
                  "intent": "FOLLOW_UP",
                  "priority": "MEDIUM",
                  "sentiment": "NEUTRAL",
                  "confidence": 0.94,
                  "keyPoints": [
                    "Previous report was requested",
                    "The user previously promised to send it",
                    "The latest message follows up on the report"
                  ],
                  "reply": "Thank you for following up. I haven't sent the report yet, but I will share it as soon as possible."
                }

                """);

        // -----------------------------------------
        // Requested tone
        // -----------------------------------------

        if (emailRequest.getTone() != null
                && !emailRequest.getTone().isBlank()) {

            prompt.append(
                    "Requested reply tone: "
            );

            prompt.append(
                    emailRequest.getTone()
            );

            prompt.append("\n\n");
        }

        // -----------------------------------------
        // Custom instruction
        // -----------------------------------------

        if (emailRequest.getCustomInstruction() != null
                && !emailRequest.getCustomInstruction().isBlank()) {

            prompt.append(
                    "Custom instruction from the user:\n"
            );

            prompt.append(
                    emailRequest.getCustomInstruction()
            );

            prompt.append("\n\n");
        }

        // -----------------------------------------
        // COMPLETE THREAD
        // -----------------------------------------

        prompt.append(
                "=========================================\n"
        );

        prompt.append(
                "COMPLETE EMAIL CONVERSATION\n"
        );

        prompt.append(
                "=========================================\n\n"
        );

        prompt.append(
                emailRequest.getThreadContent()
        );

        prompt.append("\n\n");

        // -----------------------------------------
        // LATEST MESSAGE
        // -----------------------------------------

        prompt.append(
                "=========================================\n"
        );

        prompt.append(
                "LATEST MESSAGE - RESPOND TO THIS MESSAGE\n"
        );

        prompt.append(
                "=========================================\n\n"
        );

        prompt.append(
                emailRequest.getLatestMessage()
        );

        prompt.append("\n\n");

        prompt.append(
                "Analyze the latest message using the complete "
                        + "conversation as context and generate the reply."
        );

        return prompt.toString();
    }

    // =====================================================
    // BUILD GEMINI REQUEST BODY
    // =====================================================

    private String requestBody(String prompt) {

        try {

            ObjectMapper mapper = new ObjectMapper();

            JsonNode root = mapper.createObjectNode();

            var contents =
                    ((tools.jackson.databind.node.ObjectNode) root)
                            .putArray("contents");

            var content =
                    contents.addObject();

            var parts =
                    content.putArray("parts");

            var part =
                    parts.addObject();

            part.put("text", prompt);

            return mapper.writeValueAsString(root);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to create Gemini request body",
                    e
            );
        }
    }

    // =====================================================
    // EXTRACT GEMINI RESPONSE
    // =====================================================

    private String extractResponseContent(
            String response) {

        try {

            ObjectMapper mapper = new ObjectMapper();

            JsonNode root =
                    mapper.readTree(response);

            JsonNode candidates =
                    root.path("candidates");

            if (!candidates.isArray()
                    || candidates.isEmpty()) {

                throw new RuntimeException(
                        "Gemini returned no candidates. "
                                + "Response: "
                                + response
                );
            }

            JsonNode parts =
                    candidates
                            .get(0)
                            .path("content")
                            .path("parts");

            if (!parts.isArray()
                    || parts.isEmpty()) {

                throw new RuntimeException(
                        "Gemini response contains no text. "
                                + "Response: "
                                + response
                );
            }

            return parts
                    .get(0)
                    .path("text")
                    .asText();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to extract Gemini response: "
                            + e.getMessage(),
                    e
            );
        }
    }

    // =====================================================
    // PARSE AI ANALYSIS JSON
    // =====================================================

    private EmailAnalysisResponse parseAnalysisResponse(
            String response) {

        try {

            ObjectMapper mapper =
                    new ObjectMapper();

            String cleanedResponse =
                    response
                            .replace("```json", "")
                            .replace("```JSON", "")
                            .replace("```", "")
                            .trim();

            JsonNode root =
                    mapper.readTree(cleanedResponse);

            // -----------------------------------------
            // Intent
            // -----------------------------------------

            String intent =
                    root.path("intent").asText();

            // -----------------------------------------
            // Priority
            // -----------------------------------------

            String priority =
                    root.path("priority").asText();

            // -----------------------------------------
            // Sentiment
            // -----------------------------------------

            String sentiment =
                    root.path("sentiment").asText();

            // -----------------------------------------
            // Confidence
            // -----------------------------------------

            double confidence =
                    root.path("confidence").asDouble();

            // -----------------------------------------
            // Key points
            // -----------------------------------------

            java.util.List<String> keyPoints =
                    new java.util.ArrayList<>();

            JsonNode keyPointsNode =
                    root.path("keyPoints");

            if (keyPointsNode.isArray()) {

                for (JsonNode point : keyPointsNode) {

                    keyPoints.add(
                            point.asText()
                    );
                }
            }

            // -----------------------------------------
            // Generated reply
            // -----------------------------------------

            String reply =
                    root.path("reply").asText();

            // -----------------------------------------
            // Basic validation
            // -----------------------------------------

            if (intent.isBlank()) {

                throw new RuntimeException(
                        "AI response did not contain intent."
                );
            }

            if (priority.isBlank()) {

                throw new RuntimeException(
                        "AI response did not contain priority."
                );
            }

            if (sentiment.isBlank()) {

                throw new RuntimeException(
                        "AI response did not contain sentiment."
                );
            }

            if (reply.isBlank()) {

                throw new RuntimeException(
                        "AI response did not contain a reply."
                );
            }

            // -----------------------------------------
            // Confidence validation
            // -----------------------------------------

            confidence =
                    Math.max(
                            0.0,
                            Math.min(
                                    1.0,
                                    confidence
                            )
                    );

            return new EmailAnalysisResponse(
                    intent,
                    priority,
                    sentiment,
                    confidence,
                    keyPoints,
                    reply
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to parse AI analysis response: "
                            + e.getMessage(),
                    e
            );
        }
    }
}