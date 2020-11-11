import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import commands.*;
import events.CommentWatcher;
import net.dv8tion.jda.api.JDABuilder;

import javax.security.auth.login.LoginException;

public class Main {
    public static void main(String[] args) throws LoginException {
        // bot info
        //JDABuilder builder = JDABuilder.createDefault(System.getenv("TOKEN"));
        JDABuilder builder = JDABuilder.createDefault("NzcxODQwNDgxOTE0NjUwNjU2.X5x-dg.3tPvhpWoL9_q2GeAMKgsXjVPNd8");

        // to-do: implement differently to allow mod to input these values using commands
        // CommentWatcher class should only work once these values are set "correctly"

        // to-do: tokenLevel | YT link -> a format to help replacement
        // let users pick which token they want to use by specifying level

        // server specific inputs
        String targetChannelID = "772607955538804759";

        String curatorID = "183588631569498112";

        // this token ID array must increase in level (from left to right)
        String[] tokens = new String[] {"772603050329899071", "772603979716362290",
                "772604642882093096", "772605656636063745"};

        String helpChannelID = "770745831728742464";

        // String tokenName = "PBToken";

        // int chLimit = 10;

        // waiter
        EventWaiter waiter = new EventWaiter();
        builder.addEventListeners(waiter);

        // comments
        CommentWatcher comments = new CommentWatcher(curatorID, targetChannelID, 0, true, waiter);

        // commands
        TBBalanceCommand tbBalanceCommand = new TBBalanceCommand();
        TBCommandsCommand tbCommandsCommand = new TBCommandsCommand();
        TBHelpCommand tbHelpCommand = new TBHelpCommand(helpChannelID);
        TBLevelCommand tbLevelCommand = new TBLevelCommand(tokens);
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
