package commands;

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

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
     * @param e guild message event
     */
    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent e) {
        // grab message
        String message = e.getMessage().getContentRaw();

        // if it's the command
        if (message.equals("]balance") && e.getChannel().getId().equals(commandsChId)) {
            // count number of tokens
            int nTokens = 0;

            for (int i = 0; i < Objects.requireNonNull(e.getMember()).getRoles().size(); i++) {
                if (e.getMember().getRoles().get(i).getName().contains(tokenName)) {
                    // increase count
                    nTokens++;
                }
            }

            // reply
            e.getChannel().sendMessage("<@" + e.getAuthor().getId() + ">,"
                    + " your token balance is: " + nTokens).queue();
        }

    }

}