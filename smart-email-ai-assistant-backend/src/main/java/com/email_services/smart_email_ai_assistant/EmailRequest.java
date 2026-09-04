package com.email_services.smart_email_ai_assistant;

public class EmailRequest {

    private String threadContent;
    private String latestMessage;
    private String tone;
    private String language;
    private String intelligenceLanguage;
    private String replyLength;
    private String customInstruction;
    private String apiKey;

    public String getIntelligenceLanguage() {
        return intelligenceLanguage;
    }

    public void setIntelligenceLanguage(String intelligenceLanguage) {
        this.intelligenceLanguage = intelligenceLanguage;
    }

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

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getReplyLength() {
        return replyLength;
    }

    public void setReplyLength(String replyLength) {
        this.replyLength = replyLength;
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