package commands;

import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import utils.Curator;
import utils.ReactionInfo;
import utils.Utility;

import java.util.List;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import api.SpotifyAPI;

public class TBReviewSubsCommand extends ListenerAdapter {
    // variables & constants
    private final List<Curator> curators;
    private final String chId;
    private final String commandsChId;
    private SpotifyAPI spotifyApi;

    private static final Logger logger = LoggerFactory.getLogger(TBReviewSubsCommand.class);

    // Constructor
    public TBReviewSubsCommand(List<Curator> cu, String ch, String cmd_ch) {
        curators = cu;
        chId = ch;
        commandsChId = cmd_ch;

        spotifyApi = SpotifyAPI.getInstance();
    }

    @Override
    public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
        String message = event.getMessage().getContentRaw();

        // Check if the message is the command and if it's in the correct channel
        if (message.equals("]reviewSubs") && event.getChannel().getId().equals(commandsChId)) {
            // Check if the user has the required permissions to execute this command
            User user = event.getAuthor();

            if (!Utility.isCurator(curators, user)) {
                Utility.sendSecretMessage(user, "You do not have the required permissions to run that command!", 60);
                return;
            }

            // Execute
            try {
                List<ReactionInfo> reactions = spotifyApi.processSubmissions();

                if (reactions.size() > 0) {
                    TextChannel channel = event.getJDA().getTextChannelById(chId); // submissions channel

                    for (ReactionInfo reaction : reactions) {
                        channel.retrieveMessageById(reaction.messageId)
                                .queue(msg -> msg.addReaction(reaction.emoji).queue(),
                                        throwable -> logger
                                                .error("Could not react to message: " + throwable.getMessage()));
                    }

                    // announcement in submissions channel
                    String submittedRoleId = Utility.readFromDatabase("SUBMITTED_ROLE_ID");

                    String announcementMessage = "<@&" + submittedRoleId + ">\n\n" +
                            "Just finished listening to all of the latest submissions and added a few to the playlist!\n\n"
                            +
                            "There should be a ✅ reaction if I listened to it, so let me know if I missed your submission. "
                            +
                            "Going to play with the playlist order now <:KannaHello:771929794317254686>\n\n"
                            +
                            "I'll be reposting any stories on IG that mention the playlist, just make sure to tag me so I see it!\n\n"
                            +
                            "If you wish to not be pinged until you submit another track, just type !dpm in <#" + commandsChId + "> and it should remove your \"Submitted\" role.";

                    // Send the announcement message
                    channel.sendMessage(announcementMessage).queue();
                } else {
                    logger.error("No actionable submissions found.");
                }
            } catch (Exception e) {
                logger.error("Error while processing submissions: " + e.getMessage());
            }
        }
    }

}
