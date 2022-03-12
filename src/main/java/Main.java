import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import commands.*;
import events.CommentWatcher;
import net.dv8tion.jda.api.JDABuilder;

import javax.security.auth.login.LoginException;

public class Main {
    public static void main(String[] args) throws LoginException {
        // bot info
        JDABuilder builder = JDABuilder.createDefault(System.getenv("TOKEN"));

        // to-do: implement differently to allow mod to input these values using commands
        // CommentWatcher class should only work once these values are set "correctly"

        // to-do: tokenLevel | Spotify link -> a format to help replacement
        // let users pick which token they want to use by specifying level

        // server specific inputs
        String targetChannelID = "772607955538804759"; // 772607955538804759 is pbtoken channel

        String helpChannelID = "770745831728742464"; // 770745831728742464 is pbtoken info channel

        String curatorID = "183588631569498112"; // 183588631569498112 is me on discord

        // this token ID array increases in level (from left to right)
        String[] tokens = new String[] {"772603050329899071", "772603979716362290",
                "772604642882093096", "772605656636063745"};

        String tokenName = "playlist token"; // token name (must match discord role name)

        // int chLimit = 10; // submission limit

        // waiter
        EventWaiter waiter = new EventWaiter();
        builder.addEventListeners(waiter);

        // comments
        CommentWatcher comments = new CommentWatcher(tokenName, curatorID, targetChannelID, 0, false, waiter);

        // commands
        TBBalanceCommand tbBalanceCommand = new TBBalanceCommand(tokenName);
        TBCommandsCommand tbCommandsCommand = new TBCommandsCommand();
        TBHelpCommand tbHelpCommand = new TBHelpCommand(helpChannelID);
        TBLevelCommand tbLevelCommand = new TBLevelCommand(tokenName, tokens);
        TBSlotsCommand tbSlotsCommand = new TBSlotsCommand(targetChannelID);

        // add event listeners and build
        builder.addEventListeners(comments);
        builder.addEventListeners(tbBalanceCommand);
        builder.addEventListeners(tbCommandsCommand);
        builder.addEventListeners(tbHelpCommand);
        builder.addEventListeners(tbLevelCommand);
        builder.addEventListeners(tbSlotsCommand);

        builder.build();
    }
}
