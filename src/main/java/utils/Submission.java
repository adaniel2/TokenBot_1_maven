package utils;

public class Submission {
    private String trackId;
    private String userId;
    private String messageId;
    private int submissionId;

    // Constructor
    public Submission(String trackId, String userId, String messageId, int submissionId) {
        this.trackId = trackId;
        this.userId = userId;
        this.messageId = messageId;
        this.submissionId = submissionId;
    }

    public String getTrackId() {
        return this.trackId;
    }

    public void setTrackId(String trackId) {
        this.trackId = trackId;
    }

    public String getUserId() {
        return this.userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMessageId() {
        return this.messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public int getSubmissionId() {
        return this.submissionId;
    }

    public void setSubmissionId(int submissionId) {
        this.submissionId = submissionId;
    }
    
}
