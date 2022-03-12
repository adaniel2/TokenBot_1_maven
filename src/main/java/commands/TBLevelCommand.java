package commands;

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * This is the level command used to determine the user's token's level.
 *
 * Note: It will return the level for all tokens the user has.
 *
 * @author      Daniel Almeida
 * @version     3/12/22
 */
public class TBLevelCommand extends ListenerAdapter {
    // variables & constants
    private final String tokenName;
    private final String[] tokens;

    /**
     * Constructor to initialize an array of server specific token IDs
     *
     * @param tn    token name
     * @param t     token ID array
     */
    public TBLevelCommand(String tn, String[] t) {
        tokenName = tn;
        tokens = t;
    }

    /**
     * This function replies with the caller's token's level. It will also notify the caller
     * if he has no tokens available.
     *
     * @param e     guild message event
     */
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent e) {
        // grab message
        String message = e.getMessage().getContentRaw();

        // if it's the command, determine: what token(s) caller has, calculate token level(s) and reply
        if (message.equals("]level")) {
            // string build
            StringBuilder reply = new StringBuilder("<@" + e.getAuthor().getId() + ">, you have a level ");

            // set no roles flag
            boolean noRoles = true;

            // set no token flag
            boolean noTokens = true;

            // track number of tokens found
            int count = 0;

            // cycle through roles
            for (int i = 0; i < Objects.requireNonNull(e.getMember()).getRoles().size(); i++) {
                // has roles
                noRoles = false;

                // check the role is a token
                if (e.getMember().getRoles().get(i).getName().contains(tokenName)) {
                    // has a token
                    noTokens = false;

                    // first token no comma
                    if (count > 0) {
                        reply.append(", ");
                    }

                    // cycle through token id array
                    for (int j = 0; j < tokens.length; j++) {
                        // cross-reference caller's token ID with tokens array to determine token's level
                        if (tokens[j].equals(e.getMember().getRoles().get(i).getId())) {
                            // append each token to reply
                            reply.append(((j+1) * (5)));

                            // increase count
                            count++;
                        }

                    }

                }

            }

            // if caller has no roles (and don't report no tokens)
            if (noRoles) {
                e.getChannel().sendMessage("<@" + e.getAuthor().getId() +
                        ">, according to my calculations... you're not even a member o.O").queue();
            }
            else if (noTokens) { // no tokens
                e.getChannel().sendMessage("<@" + e.getAuthor().getId() + ">, you have no tokens.").queue();
            }
            else {
                // format fixes for reply
                if (count > 1) {
                    reply.replace(reply.lastIndexOf(","), reply.lastIndexOf(",") + 1, " and");
                }
                reply.append(" token.");

                // reply with list of token levels
                e.getChannel().sendMessage(reply.toString()).queue();
            }

        }

    }

}
