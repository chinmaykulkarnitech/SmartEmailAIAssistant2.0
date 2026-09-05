
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
     * - Intelligence language
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
                            .path("/v1beta/models/gemini-3.7-flash:generateContent")
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

        java.time.LocalDate today =
                java.time.LocalDate.now();

        prompt.append(
                "Today's date is: "
                        + today
                        + "\n\n"
        );

        // -----------------------------------------
        // Base AI instructions
        // -----------------------------------------

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

                Do NOT return Markdown code fences.

                Do NOT return:
                ```json

                Do NOT return:
                ```

                Return exactly this structure:
                   
                
                {
                  "intent": "STABLE_INTENT_VALUE",
                  "priority": "STABLE_PRIORITY_VALUE",
                  "sentiment": "STABLE_SENTIMENT_VALUE",
                  "intelligenceLanguage": "actual language used for intelligence",
                  "confidence": 0.0,
                  "intentLabel": "translated intent label",
                  "priorityLabel": "translated priority label",
                  "sentimentLabel": "translated sentiment label",
                  "keyPoints": [
                    "translated key point 1",
                    "translated key point 2",
                    "translated key point 3"
                  ],
                  "reply": "generated email reply in the requested Reply Language",
                  "replyTranslation": "translation of the generated reply into the requested Intelligence Language",
                  "actionRequired": true,
                  "action": "Send the deployment report",
                  "actionStatus": "PENDING",
                  "deadlineDetected": true,
                  "deadline": "September 10, 2026 at 5:00 PM",
                  "deadlineDescription": "Deployment report must be sent by the requested deadline."
                }
                
                // -----------------------------------------
                // Action detection
                // -----------------------------------------
                
                
                        
                IMPORTANT STATUS RULE:
                
                If the conversation shows that the user still needs to send,
                submit, provide, complete, prepare, review, or perform something,
                the status MUST be "PENDING".
                
                If the required action has a future deadline and there is no clear
                evidence that the action has already been completed, the status
                MUST be "PENDING".
                
                If a deadline is detected for an action that the user still needs
                to perform, "actionStatus" MUST be "PENDING".
                
                Only use "COMPLETED" when the conversation clearly confirms that
                the required action has already been completed.
                
                Only use "WAITING" when the user has already completed their part
                and is waiting for another person.
                
                Never use "NONE" when "actionRequired" is true.
                
                
                IMPORTANT:

                "intent" must always contain the stable internal classification value.

                "priority" must always contain the stable internal priority value.

                "sentiment" must always contain the stable internal sentiment value.

                "intelligenceLanguage" must contain the actual language used
                for the AI Intelligence information.

                If the requested intelligence language is:

                "english" → return "english"

                "hindi" → return "hindi"

                "marathi" → return "marathi"

                "spanish" → return "spanish"

                "french" → return "french"

                "german" → return "german"

                If the requested intelligence language is "auto":

                1. Detect the language of the LATEST MESSAGE.
                2. Use that detected language ONLY for the AI Intelligence information.
                3. Return the detected language in the "intelligenceLanguage" JSON field.
                4. The reply language must still follow the separate
                   "Requested reply language".
                5. Do NOT use the reply language to determine the
                   intelligence language.

                Examples:

                French latest message → "intelligenceLanguage": "french"

                Hindi latest message → "intelligenceLanguage": "hindi"

                Marathi latest message → "intelligenceLanguage": "marathi"

                English latest message → "intelligenceLanguage": "english"

                "intentLabel" must contain the human-readable intent
                in the intelligence language.

                "priorityLabel" must contain the human-readable priority
                in the intelligence language.

                "sentimentLabel" must contain the human-readable sentiment
                in the intelligence language.

                "keyPoints" must be written entirely in the intelligence language.

                "reply" must be written entirely in the requested reply language.

                IMPORTANT SEPARATION RULE:

                The intelligence language and reply language are independent.

                Changing the intelligence language MUST NOT change
                the reply language.

                Changing the reply language MUST NOT change
                the intelligence language.

                Example:

                Requested reply language = french
                Requested intelligence language = marathi

                Then:

                - "reply" must be French.
                - "intentLabel" must be Marathi.
                - "priorityLabel" must be Marathi.
                - "sentimentLabel" must be Marathi.
                - "keyPoints" must be Marathi.
                - "intelligenceLanguage" must be "marathi".

                If intelligence language is "auto", detect it from the
                LATEST MESSAGE independently of the requested reply language.

                Never translate the values of:

                intent
                priority
                sentiment

                Only translate their corresponding Label fields.

                """);
        prompt.append("""
                
                        ACTION DETECTION:
                
                        Determine whether the conversation requires an action from
                        the user.
                
                        Set "actionRequired" to true ONLY when the user clearly needs
                        to perform an action based on the conversation.
                
                        Set "actionRequired" to false when no action is required.
                
                        If actionRequired is true:
                        - "action" must clearly and briefly describe what the user needs
                          to do.
                        - "actionStatus" must normally be "PENDING" when the user still
                          needs to perform the action.
                
                        ACTION STATUS VALUES:
                
                        PENDING:
                        The user still needs to perform the required action.
                
                        WAITING:
                        The user has completed their part and is waiting for another
                        person to perform an action.
                
                        COMPLETED:
                        The required action has already been completed.
                
                        NONE:
                        No meaningful action is required.
                
                        IMPORTANT:
                        - If actionRequired is true because the user needs to do something,
                          do NOT use NONE.
                        - If actionRequired is true and the action has not yet been completed,
                          use PENDING.
                        - If actionRequired is false, use NONE.
                        - Never invent an action.
                        - Use the entire conversation to determine whether an earlier
                          commitment has already been completed.
                
                        """);
        prompt.append("""
        
        ACTION DETECTION:

        Determine whether the conversation requires an action from
        the user.

        Set "actionRequired" to true ONLY when the user clearly needs
        to perform an action based on the conversation.

        Set "actionRequired" to false when no action is required.

        If actionRequired is true:

        - "action" must clearly describe what the user needs to do.
        - "actionStatus" must be PENDING if the action is not completed.

        ACTION STATUS VALUES:

        PENDING:
        The user still needs to perform the required action.

        WAITING:
        The user completed their part and is waiting for another person.

        COMPLETED:
        The required action has already been completed.

        NONE:
        No action is required.

        IMPORTANT:

        Never use NONE when actionRequired is true.

        If the user still needs to send, submit, provide, prepare,
        complete, review, or perform something, use PENDING.

        If there is a future deadline for an incomplete action,
        use PENDING.
        Only use COMPLETED when the conversation clearly confirms
        that the action has already been completed.

        Only use WAITING when the user has completed their part
        and is waiting for another person.

        """);

        prompt.append("""
    
    ACCURACY RULES:
    - Use only information contained in the provided email conversation.
    - Never invent names, dates, deadlines, amounts, attachments, decisions, or facts.
    - Do not assume information that is not explicitly stated.
    - If the sender asks a question that cannot be answered from the conversation, write a safe and appropriate reply acknowledging the question instead of inventing an answer.
    - Preserve the factual meaning of the conversation.
    
    """);

        prompt.append("""
    
    REPLY INTENT RULES:
    - Identify what the sender expects from the recipient.
    - If the sender asks a question, answer it when the answer is supported by the conversation.
    - If the sender requests an action, acknowledge the request and respond appropriately.
    - If the sender provides information, acknowledge the information when appropriate.
    - If the sender asks for confirmation, clearly confirm or explain when possible.
    - If the sender is waiting for something, address that expectation directly.
    - Do not create commitments that are not supported by the conversation.
    
    """);

        // -----------------------------------------
        // Requested tone
        // -----------------------------------------

        if (emailRequest.getTone() != null
                && !emailRequest.getTone().isBlank()) {

            prompt.append("Requested reply tone: ");
            prompt.append(emailRequest.getTone());
            prompt.append("\n\n");
        }

        // -----------------------------------------
        // Requested reply language
        // -----------------------------------------

        if (emailRequest.getLanguage() != null
                && !emailRequest.getLanguage().isBlank()) {

            prompt.append("Requested reply language: ");
            prompt.append(emailRequest.getLanguage());
            prompt.append("\n\n");

            prompt.append("""
                    LANGUAGE RULES:

                    Supported reply languages:

                    English
                    Hindi
                    Marathi
                    Spanish
                    French
                    German

                    If the requested language is "auto":
                    Detect the language of the LATEST MESSAGE
                    and generate the reply in that same language.

                    If the requested language is "english":
                    Generate the reply entirely in English.

                    If the requested language is "hindi":
                    Generate the reply entirely in Hindi.

                    If the requested language is "marathi":
                    Generate the reply entirely in Marathi.

                    If the requested language is "spanish":
                    Generate the reply entirely in Spanish.

                    If the requested language is "french":
                    Generate the reply entirely in French.

                    If the requested language is "german":
                    Generate the reply entirely in German.

                    Do not mix languages unless the original email
                    naturally requires it.

                    """);
        }

        prompt.append("""
    
    REPLY TRANSLATION:
    - Generate the email reply in the requested Reply Language.
    - Also provide a translation of that exact generated reply in the requested Intelligence Language.
    - The translation must preserve the meaning, intent, tone, and important details of the generated reply.
    - Do not generate a second or different reply.
    - replyTranslation must be only the translated version of reply.
    - If Reply Language and Intelligence Language are the same, replyTranslation should still contain the same reply in that language.
    
    """);

        // -----------------------------------------
        // Requested reply length
        // -----------------------------------------

        if (emailRequest.getReplyLength() != null
                && !emailRequest.getReplyLength().isBlank()) {

            prompt.append("Requested reply length: ");
            prompt.append(emailRequest.getReplyLength());
            prompt.append("\n\n");

            prompt.append("""
                    REPLY LENGTH RULES:

                    These rules apply ONLY to the generated email reply.
                    They do NOT change the amount of AI Intelligence information.

                    If the requested length is "short":
                    Keep the reply concise, approximately 1 to 3 sentences.

                    If the requested length is "medium":
                    Generate a natural reply of approximately 3 to 5 sentences.

                    If the requested length is "long":
                    Generate a more detailed reply of approximately 5 to 8 sentences.

                    Do not add unnecessary information just to increase length.

                    """);
        }

        // -----------------------------------------
        // Intelligence language
        // -----------------------------------------

        if (emailRequest.getIntelligenceLanguage() != null
                && !emailRequest.getIntelligenceLanguage().isBlank()) {

            prompt.append("Requested intelligence language: ");
            prompt.append(emailRequest.getIntelligenceLanguage());
            prompt.append("\n\n");

            prompt.append("""
                    INTELLIGENCE LANGUAGE RULES:

                    The AI Intelligence section must be generated in the
                    requested intelligence language.

                    The intelligence section includes:

                    - Intent
                    - Priority
                    - Sentiment
                    - Confidence
                    - Key Points

                    Supported intelligence languages:

                    English
                    Hindi
                    Marathi
                    Spanish
                    French
                    German

                    If the requested intelligence language is "auto":

                    1. Detect the language of the LATEST MESSAGE.
                    2. Use that detected language ONLY for the AI Intelligence information.
                    3. Return the detected language in the "intelligenceLanguage" JSON field.
                    4. Keep the reply language independent.

                    If the requested intelligence language is "english":
                    Generate the AI Intelligence information entirely in English.

                    If the requested intelligence language is "hindi":
                    Generate the AI Intelligence information entirely in Hindi.

                    If the requested intelligence language is "marathi":
                    Generate the AI Intelligence information entirely in Marathi.

                    If the requested intelligence language is "spanish":
                    Generate the AI Intelligence information entirely in Spanish.

                    If the requested intelligence language is "french":
                    Generate the AI Intelligence information entirely in French.

                    If the requested intelligence language is "german":
                    Generate the AI Intelligence information entirely in German.

                    The actual classification meaning must not change
                    when the intelligence language changes.

                    Only the presentation language changes.

                    """);
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
        // Complete thread
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
        // Latest message
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


// -----------------------------------------
// Deadline detection
// -----------------------------------------

        prompt.append("""
        
        DEADLINE DETECTION:

        Carefully examine the ENTIRE email conversation for deadlines,
        due dates, dates, or times by which an action is expected.

        Set "deadlineDetected" to true when the conversation contains
        a clearly stated deadline or a deadline that can be directly
        resolved from an explicitly stated relative date.

        Examples of valid deadlines:

        "by September 10"
        "before September 10, 2026"
        "by Friday"
        "before tomorrow"
        "by the end of this week"
        "before the 8th of this month"
        "submit it at 5 PM tomorrow"

        RELATIVE DATE RULES:

        Use the provided current date to resolve relative dates.

        Examples:

        If today's date is 2026-09-05:

        "tomorrow" = September 6, 2026

        "the 8th of this month" = September 8, 2026

        "before the 8th of this month" = September 8, 2026

        IMPORTANT:
        - Resolve relative dates only when the reference is clear.
        - Use the month and year implied by the conversation and
          today's date when appropriate.
        - Do not invent a deadline.
        - Do not convert vague expressions such as "soon", "shortly",
          "as soon as possible", "when possible", or "later" into
          a specific deadline.
        - If a specific date or time is explicitly provided, preserve
          its meaning accurately.
        - If the conversation contains multiple dates, identify the
          date that actually represents the deadline for the required
          action.
        - A date mentioned only as historical context is NOT necessarily
          a deadline.

        DEADLINE OUTPUT:

        If a deadline is detected:

        "deadlineDetected" must be true.

        "deadline" must contain the resolved deadline in a clear
        human-readable format.

        "deadlineDescription" must briefly explain what action or
        requirement the deadline applies to.

        If no deadline is detected:

        "deadlineDetected" must be false.

        "deadline" must be an empty string.

        "deadlineDescription" must be an empty string.

        IMPORTANT:
        Never invent a deadline that is not supported by the conversation.

        """);

        prompt.append("""
        
        FINAL OUTPUT REQUIREMENT:

        Before generating the final JSON, you MUST check the ENTIRE
        conversation for a deadline.

        In this conversation, a deadline may be present in either
        an earlier message or the latest message.

        If the sender says something such as:

        "before the 8th date of this month"
        "before September 8"
        "by September 8"
        "by tomorrow"
        "before Friday"

        you MUST resolve the date using today's date and return:

        "deadlineDetected": true

        You MUST NOT omit the deadlineDetected field.

        You MUST NOT omit the deadline field.

        You MUST NOT omit the deadlineDescription field.

        If a deadline exists, return the resolved date in the
        "deadline" field.

        For example, if today's date is 2026-09-05 and the email says
        "before the 8th date of this month", return:

        "deadlineDetected": true,
        "deadline": "September 8, 2026",
        "deadlineDescription": "Send the deployment report and all required materials"

        If there is genuinely no deadline anywhere in the conversation,
        return:

        "deadlineDetected": false,
        "deadline": "",
        "deadlineDescription": ""

        The final response MUST contain ALL of these fields:

        actionRequired
        action
        actionStatus
        deadlineDetected
        deadline
        deadlineDescription

        Return ONLY the JSON object. Do not add explanations.

        """);

        prompt.append(
                "Now analyze the complete conversation and return the final JSON response."
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
            // Intelligence language
            // -----------------------------------------

            String intelligenceLanguage =
                    root.path("intelligenceLanguage").asText();

            // -----------------------------------------
            // Confidence
            // -----------------------------------------

            double confidence =
                    root.path("confidence").asDouble();

            // -----------------------------------------
            // Translated intelligence labels
            // -----------------------------------------

            String intentLabel =
                    root.path("intentLabel").asText();

            String priorityLabel =
                    root.path("priorityLabel").asText();

            String sentimentLabel =
                    root.path("sentimentLabel").asText();

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

            String reply = root.path("reply").asText();

            String replyTranslation =
                    root.path("replyTranslation").asText();


            boolean actionRequired = root.path("actionRequired").asBoolean();
            String action = root.path("action").asText();
            String actionStatus = root.path("actionStatus").asText();



            if (actionRequired && !"COMPLETED".equalsIgnoreCase(actionStatus)) {
                actionStatus = "PENDING";
            }

            if (!actionRequired) {
                actionStatus = "NONE";
            }

            boolean deadlineDetected =
                    root.path("deadlineDetected").asBoolean();

            String deadline =
                    root.path("deadline").asText();

            String deadlineDescription =
                    root.path("deadlineDescription").asText();

            if (intent.isBlank()) {

                throw new RuntimeException(
                        "AI response did not contain intent."
                );
            }
            if (replyTranslation.isBlank()) {
                throw new RuntimeException(
                        "AI response did not contain reply translation."
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

            if (intelligenceLanguage.isBlank()) {

                throw new RuntimeException(
                        "AI response did not contain intelligence language."
                );
            }

            if (intentLabel.isBlank()) {

                throw new RuntimeException(
                        "AI response did not contain intent label."
                );
            }

            if (priorityLabel.isBlank()) {

                throw new RuntimeException(
                        "AI response did not contain priority label."
                );
            }

            if (sentimentLabel.isBlank()) {

                throw new RuntimeException(
                        "AI response did not contain sentiment label."
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

            // -----------------------------------------
            // Return final response
            // -----------------------------------------
            return new EmailAnalysisResponse(
                    intent,
                    priority,
                    sentiment,
                    intelligenceLanguage,
                    confidence,
                    intentLabel,
                    priorityLabel,
                    sentimentLabel,
                    keyPoints,
                    reply,
                    replyTranslation,
                    actionRequired,
                    action,
                    actionStatus,
                    deadlineDetected,
                    deadline,
                    deadlineDescription
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
