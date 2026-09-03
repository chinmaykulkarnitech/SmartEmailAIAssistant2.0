//package com.email_services.smart_email_ai_assistant;
//
//public class EmailRequest {
//
//    private String emailContent;
//    private String tone;
//    private String customInstruction;
//    private String apiKey;
//
//    public EmailRequest() {
//    }
//
//    public String getEmailContent() {
//        return emailContent;
//    }
//
//    public void setEmailContent(String emailContent) {
//        this.emailContent = emailContent;
//    }
//
//    public String getTone() {
//        return tone;
//    }
//
//    public void setTone(String tone) {
//        this.tone = tone;
//    }
//
//    public String getCustomInstruction() {
//        return customInstruction;
//    }
//
//    public void setCustomInstruction(String customInstruction) {
//        this.customInstruction = customInstruction;
//    }
//
//    public String getApiKey() {
//        return apiKey;
//    }
//
//    public void setApiKey(String apiKey) {
//        this.apiKey = apiKey;
//    }
//}

package com.email_services.smart_email_ai_assistant;

public class EmailRequest {

    private String threadContent;
    private String latestMessage;
    private String tone;
    private String customInstruction;
    private String apiKey;

    public EmailRequest() {
    }

    public String getThreadContent() {
        return threadContent;
    }

    public void setThreadContent(String threadContent) {
        this.threadContent = threadContent;
    }

    public String getLatestMessage() {
        return latestMessage;
    }

    public void setLatestMessage(String latestMessage) {
        this.latestMessage = latestMessage;
    }

    public String getTone() {
        return tone;
    }

    public void setTone(String tone) {
        this.tone = tone;
    }

    public String getCustomInstruction() {
        return customInstruction;
    }

    public void setCustomInstruction(String customInstruction) {
        this.customInstruction = customInstruction;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}