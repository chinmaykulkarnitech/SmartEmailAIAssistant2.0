package com.email_services.smart_email_ai_assistant;

import java.util.List;

public class EmailAnalysisResponse {

    private String intent;
    private String priority;
    private String sentiment;
    private double confidence;
    private List<String> keyPoints;
    private String reply;

    public EmailAnalysisResponse() {
    }

    public EmailAnalysisResponse(
            String intent,
            String priority,
            String sentiment,
            double confidence,
            List<String> keyPoints,
            String reply) {

        this.intent = intent;
        this.priority = priority;
        this.sentiment = sentiment;
        this.confidence = confidence;
        this.keyPoints = keyPoints;
        this.reply = reply;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getSentiment() {
        return sentiment;
    }

    public void setSentiment(String sentiment) {
        this.sentiment = sentiment;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public List<String> getKeyPoints() {
        return keyPoints;
    }

    public void setKeyPoints(List<String> keyPoints) {
        this.keyPoints = keyPoints;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }
}