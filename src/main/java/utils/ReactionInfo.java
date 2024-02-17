package utils;

public class ReactionInfo {
    public String userId;
    public String messageId;
    public String emoji;

    public ReactionInfo(String userId, String messageId, String emoji) {
        this.userId = userId;
        this.messageId = messageId;
        this.emoji = emoji;
    }
}
