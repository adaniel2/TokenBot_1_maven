package events;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import api.SpotifyAPI;
import exceptions.DuplicateTrackException;
import exceptions.TrackNotFoundException;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import utils.Curator;
import utils.Utility;

import javax.annotation.Nonnull;

import org.apache.commons.validator.routines.UrlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 *
 * @author Daniel Almeida
 * @version 3/12/22
 */
public class CommentWatcher extends ListenerAdapter {
    // variables & constants
    private final String playlistTokenName; // playlist token name
    private final String chId; // comment channel ID (submission channel)
    private final String helpChId; // help channel ID
    private final int HELP_COUNT; // if there's any help messages in the channel, define that number here
    private final boolean godMode; // allows posting without token (can be enabled for maintenance purposes)
    private final String adminId; // admin
    private final List<Curator> curators; // curators
    private final EventWaiter waiter; // EventWaiter
    private SpotifyAPI spotifyApi; // api
    private boolean botIsReady; // bot status
    private final boolean tokenRequirementEnabled; // Enables/disables the requirement for a token

    private static final Logger logger = LoggerFactory.getLogger(CommentWatcher.class);
    private static final Pattern SPOTIFY_PATTERN = Pattern.compile(
            "(?:spotify:(album|track|playlist):([^\\s?]+)|(https?://(?:open|play)\\.spotify\\.com/(album|track|playlist)/([^\\s?]+)))");
    private static final UrlValidator urlValidator = new UrlValidator(UrlValidator.ALLOW_ALL_SCHEMES);

    /**
     * Constructor for CommentWatcher initializes variables.
     *
     * @param tn     playlist token name
     * @param adm    admin's ID
     * @param cu     curator list
     * @param ch     submission channel ID
     * @param IC     no. of permanent help/instruction messages in channel
     * @param gm     god mode
     * @param tknReq token requirement
     * @param w      event waiter
     * @param api    spotify api
     */
    public CommentWatcher(String tn, String adm, List<Curator> cu, String ch, String hlp, int HC, boolean gm,
            boolean tknReq,
            EventWaiter w, SpotifyAPI api) {
        playlistTokenName = tn;
        adminId = adm;
        curators = cu;
        chId = ch;
        helpChId = hlp;
        HELP_COUNT = HC;
        godMode = gm;
        tokenRequirementEnabled = tknReq;
        waiter = w;
        spotifyApi = api;
        botIsReady = false;
    }

    public void setBotIsReady(boolean status) {
        this.botIsReady = status;
    }

    /**
     *
     * @param event event triggering function call
     */
    @Override
    public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
        User user = event.getAuthor();

        if (!event.getChannel().getId().equals(chId) || user.isBot()) {
            return;
        }

        Message messageSent = event.getMessage();

        // If the bot is not ready, delete any message and log an error.
        if (!botIsReady) {
            messageSent.delete().queue();

            logger.error("Bot is not ready.");

            return;
        }

        Matcher spotifyMatcher = SPOTIFY_PATTERN.matcher(messageSent.getContentRaw());

        if (spotifyMatcher.find()) { // If it's a valid Spotify link
            if (isValidSubmission(event, spotifyMatcher, user)) { // If it's a valid submission
                boolean submissionAdded = false;

                if (isCurator(user) && godMode) {
                    // Admin/Curator is acting with God Mode ON
                    try {
                        submissionAdded = spotifyApi.addToPlaylist(messageSent.getContentRaw());

                        if (submissionAdded) {
                            event.getChannel()
                                    .sendMessage(
                                            "Submission added by admin without using a token. <@" + user.getId() + ">")
                                    .queue();
                        }
                    } catch (DuplicateTrackException e) {
                        String msg = e.getMessage();

                        if (msg != null) {
                            event.getChannel().sendMessage(msg).queue();
                        }

                        logger.error(e.getMessage());
                    } catch (TrackNotFoundException e) {
                        event.getChannel()
                                .sendMessage(
                                        "Unable to add submission without using a token because track does not exist: "
                                                + spotifyMatcher.group(0))
                                .queue();

                        logger.error(e.getMessage());
                    }
                } else if (hasToken(event, playlistTokenName)) {
                    // User has the token (or token requirement is off)
                    try {
                        submissionAdded = spotifyApi.addToPlaylist(messageSent.getContentRaw());

                        if (submissionAdded) {
                            event.getChannel()
                                    .sendMessage("We got your submission <@" + user.getId() + ">, thanks!")
                                    .queue();

                            flagSubmitted(event); // give user submitted token

                            // Only remove the token if token requirements are enabled
                            if (tokenRequirementEnabled) {
                                removeToken(event, playlistTokenName);
                            }
                        }
                    } catch (DuplicateTrackException e) {
                        String msg = e.getMessage();

                        if (msg != null) {
                            event.getChannel().sendMessage(msg).queue();
                        }

                        logger.error(e.getMessage());
                    } catch (TrackNotFoundException e) {
                        event.getChannel()
                                .sendMessage("Hey, I was unable to find the track you submitted: "
                                        + spotifyMatcher.group(0) + "\n\n" +
                                        "Please double check the link is correct!")
                                .queue();

                        logger.error(e.getMessage());
                    }
                } else {
                    // Regular user without the required token
                    messageSent.delete().queue();

                    logger.warn("Suspicious activity detected: " + user.getName());
                }
            } else {
                // Invalid Spotify submission
                messageSent.delete().queue();

                logger.warn("Invalid Spotify submission deleted.");
            }
        } else if (urlValidator.isValid(messageSent.getContentRaw())) {
            // Handle non-Spotify URLs
            messageSent.delete().queue();

            Utility.sendSecretMessage(user,
                    "Hello o/, I saw your submission, but I only accept Spotify links!\n\n" +
                            "Check out the <#" + helpChId + "> channel for more details!\n\n" +
                            "Note: This message will be deleted after 60 seconds.",
                    60).queue();

            logger.warn("Invalid link deleted.");
        }

    }

    /**
     * Determine if the event's message is in the format of a Spotify track link.
     *
     * @param e event containing message
     * @return true if the message is a Spotify track link
     */
    private boolean isValidSubmission(GuildMessageReceivedEvent e, Matcher match, User user) {
        match.reset();

        if (match.find()) { // spotify album, track or playlist link found
            String contentType;
            String uriOrLink;

            if (match.group(1) != null) { // URI
                contentType = match.group(1);
                uriOrLink = "URI";
            } else if (match.group(4) != null) { // Link
                contentType = match.group(4);
                uriOrLink = "link";
            } else {
                contentType = "unknown";
                uriOrLink = "unknown";
            }

            switch (contentType) {
                case "album":
                    logger.info("Spotify album " + uriOrLink + "provided.");

                    break;
                case "track":
                    logger.info("Spotify track " + uriOrLink + "provided.");

                    return true;
                case "playlist":
                    logger.info("Spotify playlist " + uriOrLink + "provided.");

                    break;
                default:
                    logger.warn("Unknown Spotify " + uriOrLink + "type.");
            }

            Utility.sendSecretMessage(user,
                    "Provided " + uriOrLink + ": " + contentType + "\n\n" +
                            "This is not a track link! Please pick a single track to submit." +
                            " Check the #hidden-gems-help channel for more information.\n\n" +
                            "Note: This message will disappear after 60 seconds.",
                    60).queue();

            return false;
        }

        logger.warn("Not a recognized Spotify link.");

        return false;
    }

    private void flagSubmitted(GuildMessageReceivedEvent e) {
        String submittedRoleId = Utility.readFromDatabase("SUBMITTED_ROLE_ID");
        Member member = e.getMember();

        try {
            if (member != null && submittedRoleId != null) {
                net.dv8tion.jda.api.entities.Role submitted;

                submitted = e.getGuild().getRoleById(submittedRoleId);

                if (submitted != null && !member.getRoles().contains(submitted)) {
                    e.getGuild().addRoleToMember(member, submitted).queue();
                }
            } else {
                throw new NullPointerException("User or Role missing.");
            }
        } catch (NullPointerException | UnsupportedOperationException | ClassCastException
                | IllegalArgumentException err) {
            logger.error("Error: " + err.getMessage());
        }

    }

    private boolean isCurator(User user) {
        return curators.stream().anyMatch(curator -> curator.getId().equals(user.getId()));
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

        // return comment count (limit is 100) accounting for help messages
        return ch.getHistoryBefore(msg.getId(), 100).complete().getRetrievedHistory().size() - HELP_COUNT;
    }

    /**
     * Check if user has a token.
     *
     * @param e event caused by user
     * @return whether the user has a token
     */
    private boolean hasToken(GuildMessageReceivedEvent e, String tokenName) {
        if (!tokenRequirementEnabled) {
            return true; // Bypasses the token check if the requirement is disabled.
        }

        boolean tokenFlag = false;

        try {
            Member member = e.getMember();

            if (member != null) {
                // find role
                for (int i = 0; i < Objects.requireNonNull(member).getRoles().size() && !tokenFlag; i++) {
                    if (member.getRoles().get(i).getName().contains(tokenName)) {
                        // has a token
                        tokenFlag = true;
                    }
                }

                return tokenFlag;
            }
        } catch (NullPointerException err) {
            logger.error(err.getMessage());
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
    private void removeToken(GuildMessageReceivedEvent e, String tokenName) {
        // removed flag (although might be useless since i'm no longer looping)
        boolean removed = false;

        Member member = e.getMember();

        if (member != null) {
            Optional<Role> optionalRole = member.getRoles().stream()
                    .filter(role -> role.getName().contains(tokenName))
                    .findFirst();

            if (optionalRole.isPresent() && !removed) {
                Role role = optionalRole.get();

                if (role != null) { // redundant but silences warning
                    e.getGuild().removeRoleFromMember(member, role).queue();

                    removed = true;
                }
            }

        }

    }

}