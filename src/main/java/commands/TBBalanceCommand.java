package commands;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.Objects;

import javax.annotation.Nonnull;

/**
 * This is the balance command. It will return the number of token(s) a user
 * currently has
 * and associated level.
 *
 * @author Daniel Almeida
 * @version 3/12/22
 */
public class TBBalanceCommand extends ListenerAdapter {
    // variables & constants
    private final String tokenName; // token name
    private final String commandsChId;

    /**
     * Constructor for TBBalanceCommand initializes variables.
     *
     * @param tName Name of token used in server
     */
    public TBBalanceCommand(String tName, String cmd_ch) {
        tokenName = tName;
        commandsChId = cmd_ch;
    }

    /**
     * Watch guild messages for the "balance" command and reply with user's token
     * balance.
     *
     * @param event guild message event
     */
    @Override
    public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
        // grab message
        String message = event.getMessage().getContentRaw();

        // if it's the command
        if (message.equals("]balance") && event.getChannel().getId().equals(commandsChId)) {
            // count number of tokens
            int nTokens = 0;

            Member member = event.getMember();

            if (member != null) {
                for (int i = 0; i < Objects.requireNonNull(member).getRoles().size(); i++) {
                    if (member.getRoles().get(i).getName().equals(tokenName)) {
                        // increase count
                        nTokens++;
                    }
                }

                // reply
                event.getChannel().sendMessage("<@" + event.getAuthor().getId() + ">,"
                        + " your token balance is: " + nTokens).queue();
            }

        }

    }

}
