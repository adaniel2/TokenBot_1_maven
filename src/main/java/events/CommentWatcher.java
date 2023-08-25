package events;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is a CommentWatcher class. It is responsible for watching for comments
 * sent into a submission channel and responding with an appropriate action as
 * defined
 * by the purposes of the bot.
 *
 * @author Daniel Almeida
 * @version 3/12/22
 */
public class CommentWatcher extends ListenerAdapter {
    // variables & constants
    private final String tokenName; // token name
    private final String chId; // comment channel id (submission channel)
    private final int INFO_COUNT; // if there's any info messages in the channel, define that number here
    private final boolean godMode; // allows posting without token (can be enabled for maintenance purposes)
    private final String curatorID; // curator
    // private final EventWaiter waiter; // EventWaiter
    private SpotifyAPI spotifyApi; // api
    private boolean botIsReady;

    /**
     * Constructor for CommentWatcher initializes variables.
     *
     * @param tn token name
     * @param cu curator's ID
     * @param ch submission channel ID
     * @param IC no. of permanent info/instruction messages in channel
     * @param gm decide whether no-token posts are deleted or not
     * @param w  event waiter
     */
    public CommentWatcher(String tn, String cu, String ch, int IC, boolean gm, EventWaiter w, SpotifyAPI api) {
        tokenName = tn;
        curatorID = cu;
        chId = ch;
        // waiter = w;
        INFO_COUNT = IC;
        godMode = gm;
        spotifyApi = api;
        botIsReady = false;
    }

    public void setBotIsReady(boolean status) {
        this.botIsReady = status;
    }

    /**
     * This function is called every time a guild message is posted.
     *
     * When a comment is posted in the channel corresponding to chId, the comment is
     * deleted if IC comments already exist in said channel or if the message is not
     * of proper form.
     *
     * If the submission is successful, then the user's token is removed.
     *
     * Nothing happens when a message is posted outside of submission channel.
     *
     * Note: Enabling god-mode prevents deletion of posts made without a token
     * (expected to be a mod).
     *
     * @param event event triggering function call
     */
    @Override
    public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
        // comment removal flag (using boolean object to help handle mod case)
        Boolean commentRemoved = null;

        if (!event.getChannel().getId().equals(chId)) { // channel check
            return;
        } else if (!event.getAuthor().isBot() /* && hasToken(event) */) { // bot check (and token check just in case)
            // grab event's message and user
            Message messageSent = event.getMessage();
            User user = event.getAuthor();

            // exit if bot is not ready
            if (!botIsReady) {
                messageSent.delete().queue();
                commentRemoved = true;

                System.out.println("Bot is not ready.");

                return;
            }

            // set comment removal flag
            commentRemoved = false;

            String urlRegEx = "(https?://[\\w.-]+)";
            Matcher urlMatcher = Pattern.compile(urlRegEx).matcher(messageSent.getContentRaw());

            if (urlMatcher.find()) { // is any link
                // delete link if not proper format
                if (!isTrackLink(event)) { // format check
                    messageSent.delete().queue();
                    commentRemoved = true;

                    Utility.sendSecretMessage(user,
                            "Spotify track links only! If the link continues to give this error, " +
                                    "check the #playlist-token-info channel for more information.",
                            10).queue();
                }

                // comment not removed, carry on with submission
                if (!commentRemoved) {
                    spotifyApi.addToPlaylist(messageSent.getContentRaw());

                    event.getChannel().sendMessage("We got your submission <@" + event.getAuthor().getId()
                            + ">, thanks!").queue();

                    // removeToken(event);
                }

            }

        }

        // god mode case; posting without a token
        if (!event.getAuthor().isBot() && event.getChannel().getId().equals(chId) && commentRemoved == null) {
            // alert console
            System.out.println("An admin-level action was performed by: " + event.getAuthor().getName());

            // message deletion condition
            if (!godMode) {
                event.getMessage().delete().queue();
            }

        }

    }

    /**
     * Determine if the event's message is in the format of a Spotify track link.
     *
     * @param e event containing message
     * @return true if the message is a Spotify track link
     */
    private boolean isTrackLink(GuildMessageReceivedEvent e) {
        // grab the message being checked
        Message m = e.getMessage();

        // return if format is proper
        String regEx = "^(?:spotify:|(?:https?://(?:open|play)\\.spotify\\.com/))(?:embed)?/?(album|track)";
        Matcher match = Pattern.compile(regEx).matcher(m.getContentRaw());

        if (match.find()) {
            return match.group(1).equals("track");
        }

        return false;
    }

    /**
     * Given an event, return the number of messages before message
     * corresponding to the event.
     *
     * @param e event
     * @return number of messages before event's message
     */
    private int commentCount(GuildMessageReceivedEvent e) {
        // grab channel
        MessageChannel ch = e.getChannel();

        // grab message
        Message msg = e.getMessage();

        // return comment count (limit is 100) accounting for info messages
        return ch.getHistoryBefore(msg.getId(), 100).complete().getRetrievedHistory().size() - INFO_COUNT;
    }

    /**
     * Check if user has a token.
     *
     * @param e event caused by user
     * @return whether the user has a token
     */
    private boolean hasToken(GuildMessageReceivedEvent e) {
        boolean tokenFlag = false;

        // find role
        for (int i = 0; i < Objects.requireNonNull(e.getMember()).getRoles().size() && !tokenFlag; i++) {
            if (e.getMember().getRoles().get(i).getName().contains(tokenName)) {
                // has a token
                tokenFlag = true;
            }
        }

        return tokenFlag;
    }

    /**
     * Remove one of the user's token.
     *
     * Note: This removes the first token the method finds.
     *
     * @param e event containing user
     */
    private void removeToken(GuildMessageReceivedEvent e) {
        // flag to prevent multiple token deletion
        boolean removed = false;

        // cycle through roles to find token
        for (int i = 0; i < Objects.requireNonNull(e.getMember()).getRoles().size(); i++) {
            if (e.getMember().getRoles().get(i).getName().contains(tokenName) && !removed) {
                // remove role from user
                e.getGuild().removeRoleFromMember(e.getMember(), e.getMember().getRoles().get(i)).queue();

                // set flag
                removed = true;
            }
        }
    }

    /**
     * A reaction by curator to a link in the submission channel will send the
     * author a direct
     * message detailing the result of their submission (which is determined by a
     * prompt sent to curator).
     *
     * Checkmark: Submission accepted.
     * Cross: Submission denied.
     *
     * Note: Currently, any reaction by curator will trigger the decision process.
     * Note: If multiple submissions are sent to curator, replying 'y' or 'n' will
     * apply to all submissions pending
     * This is an 'issue' that I could work on improving later...
     *
     * @param event reaction event
     */
    // @Override
    // public void onGuildMessageReactionAdd(@Nonnull GuildMessageReactionAddEvent
    // event) {
    // // channel & curator check
    // if (event.getChannel().getId().equals(chId) &&
    // event.getUser().getId().equals(curatorID)) {
    // // grab required variables from event (can't make .complete() call inside
    // lambda)
    // RestAction<Message> m =
    // event.getChannel().retrieveMessageById(event.getMessageId());
    // Message mess = m.complete();
    // User commentAuthor = mess.getAuthor();

    // // send decision prompt to curator (given 60 seconds to reply)
    // event.getUser().openPrivateChannel()
    // .flatMap(channel -> {
    // channel.sendMessage("Submission posted by: " + commentAuthor.getName())
    // .delay(60, TimeUnit.SECONDS)
    // .flatMap(Message::delete).queue();

    // // waiter
    // waiter.waitForEvent(MessageReceivedEvent.class, e ->
    // e.getAuthor().getId().equals(curatorID)
    // && e.getChannel().equals(channel) && isYesNo(e), e -> {
    // // bot actions for 'y' and 'n'
    // switch(e.getMessage().getContentRaw()) {
    // case "y":
    // // send author accepted message
    // sendSecretMessage(commentAuthor, "<@" + commentAuthor.getId() + ">, " +
    // "your track submission (" + mess.getContentRaw() + ") was accepted!",
    // 86400).queue();

    // // m.complete().delete().queue(); // delete link
    // break;
    // case "n":
    // // send author denied message
    // sendSecretMessage(commentAuthor, "<@" + commentAuthor.getId() + ">, " +
    // "your track submission (" + mess.getContentRaw() + ") was denied.",
    // 86400).queue();

    // // m.complete().delete().queue(); // delete link
    // break;
    // default: // this should never happen; throw exception
    // throw new RuntimeException("Unreachable switch case occurrence.");
    // }

    // }, 30, TimeUnit.SECONDS, () -> {
    // // timeout due to wrong or no input
    // event.getReaction().removeReaction(event.getUser()).queue();

    // String badText = ("Correct input not detected. Please react to the submission
    // again.\n\n" +
    // "During the next decision prompt, make sure you type either" +
    // " 'y' or 'n' (case sensitive).\n(Note: The comment's link was not " +
    // "removed and your reaction was cleared.)");

    // sendSecretMessage(event.getUser(), badText, 60);
    // });

    // return channel.sendMessage("Enter your decision below (y/n):");

    // }).delay(60, TimeUnit.SECONDS)
    // .flatMap(Message::delete).queue();

    // }

    // }

    // /**
    // * This helper function is responsible for checking if the message is "y" or
    // "n".
    // *
    // * @param e event when bot receives a decision from curator
    // * @return true if message is "y" or "n"
    // */
    // private boolean isYesNo(MessageReceivedEvent e) {
    // return e.getMessage().getContentRaw().equals("y") ||
    // e.getMessage().getContentRaw().equals("n");
    // }

}