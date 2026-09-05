package com.email_services.smart_email_ai_assistant;

import java.util.List;

public class EmailAnalysisResponse {

    // Stable internal AI classifications
    private String intent;
    private String priority;
    private String sentiment;
    private String intelligenceLanguage;
    private double confidence;


    // Intelligence information in requested language
    private String intentLabel;
    private String priorityLabel;
    private String sentimentLabel;
    private List<String> keyPoints;

    public String getActionStatus() {
        return actionStatus;
    }

    public void setActionStatus(String actionStatus) {
        this.actionStatus = actionStatus;
    }

    // Generated email reply
    private String reply;
    private String replyTranslation;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    private boolean actionRequired;
    private String action;
    private String actionStatus;
    private boolean deadlineDetected;
    private String deadline;
    private String deadlineDescription;

    public EmailAnalysisResponse() {
    }

    public EmailAnalysisResponse(
            String intent,
            String priority,
            String sentiment,
            String intelligenceLanguage,
            double confidence,
            String intentLabel,
            String priorityLabel,
            String sentimentLabel,
            List<String> keyPoints,
            String reply,
            String replyTranslation,
            boolean actionRequired,
            String action,
            String actionStatus,
            boolean deadlineDetected,
            String deadline,
            String deadlineDescription
    ) {
        this.intent = intent;
        this.priority = priority;
        this.sentiment = sentiment;
        this.intelligenceLanguage = intelligenceLanguage;
        this.confidence = confidence;
        this.intentLabel = intentLabel;
        this.priorityLabel = priorityLabel;
        this.sentimentLabel = sentimentLabel;
        this.keyPoints = keyPoints;
        this.reply = reply;
        this.replyTranslation = replyTranslation;
        this.actionRequired = actionRequired;
        this.action = action;
        this.actionStatus = actionStatus;
        this.deadlineDetected = deadlineDetected;
        this.deadline = deadline;
        this.deadlineDescription = deadlineDescription;
    }

    public String getReplyTranslation() {
        return replyTranslation;
    }

    public void setReplyTranslation(String replyTranslation) {
        this.replyTranslation = replyTranslation;
    }

    public String getIntelligenceLanguage() {
        return intelligenceLanguage;
    }

    public void setIntelligenceLanguage(String intelligenceLanguage) {
        this.intelligenceLanguage = intelligenceLanguage;
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

    public String getIntentLabel() {
        return intentLabel;
    }

    public void setIntentLabel(String intentLabel) {
        this.intentLabel = intentLabel;
    }

    public String getPriorityLabel() {
        return priorityLabel;
    }

    public void setPriorityLabel(String priorityLabel) {
        this.priorityLabel = priorityLabel;
    }

    public String getSentimentLabel() {
        return sentimentLabel;
    }

    public void setSentimentLabel(String sentimentLabel) {
        this.sentimentLabel = sentimentLabel;
    }

    public List<String> getKeyPoints() {
        return keyPoints;
    }

    public void setKeyPoints(List<String> keyPoints) {
        this.keyPoints = keyPoints;
    }

    public boolean isActionRequired() {
        return actionRequired;
    }

    public void setActionRequired(boolean actionRequired) {
        this.actionRequired = actionRequired;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }
}