package commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;

import javax.annotation.Nonnull;

/**
 * This is the commands command. Returns an embed of commands available.
 *
 * @author Daniel Almeida
 * @version 3/12/22
 */
public class TBCommandsCommand extends ListenerAdapter {
    // array of commands
    private final String[][] commands = new String[][] {
            { "]balance", "Token balance" },
            { "]commands", "List of commands" },
            { "]help", "Help info" } };

    private final String commandsChId;

    public TBCommandsCommand(String cmd_ch) {
        commandsChId = cmd_ch;
    }

    /**
     * Return an embed containing all commands and their functions.
     *
     * @param event guild message event
     */
    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        // grab message
        String message = event.getMessage().getContentRaw();

        // if it's the command, return a list of commands and their functions
        if (message.equals("]commands") && event.getChannel().getId().equals(commandsChId)) {
            // embed builder
            EmbedBuilder eb = new EmbedBuilder();

            eb.setTitle("TokenBot Commands");
            eb.setColor(new Color(255, 178, 113));
            eb.setThumbnail("https://static.wikia.nocookie.net/great-characters/images/2/22/" +
                    "Fujiwara.Chika.full.2474576.png/revision/latest/top-crop/width/360/height/450?cb=20191102191124");

            // add fields
            for (String[] command : commands) {
                eb.addField(command[0], "`" + command[1] + "`", true);
            }

            // format clean-up
            for (int i = commands.length; i < (int) (3 * Math.floor((commands.length + 3) / 3)); i++) {
                eb.addBlankField(true);
            }

            // reply
            event.getChannel().sendMessageEmbeds(eb.build()).queue();
        }

    }

}