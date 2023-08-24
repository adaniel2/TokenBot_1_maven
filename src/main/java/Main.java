import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import commands.*;
import events.CommentWatcher;
import events.SpotifyAPI;
import events.Utility;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import static spark.Spark.*;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.security.auth.login.LoginException;

public class Main {
    public static void main(String[] args) throws LoginException {
        // bot
        JDABuilder builder = JDABuilder.createDefault(Utility.readFromFile("TOKEN"));
        SpotifyAPI spotifyApi = SpotifyAPI.getInstance();

        CountDownLatch latch = new CountDownLatch(1);

        port(8080);

        get("/spotify-redirect", (req, res) -> {
            String code = req.queryParams("code");

            if (code != null && !code.isEmpty()) {
                Utility.saveToFile("SPOTIFY_AUTH_CODE", code); // Store the one-time auth code

                latch.countDown();

                return "Authorization successful!";
            }

            return "No authorization code found in the request.";
        });

        // server specific inputs
        String targetChannelID = Utility.readFromFile("TARGET_CHANNEL_ID");
        String helpChannelID = Utility.readFromFile("HELP_CHANNEL_ID");
        String curatorID = Utility.readFromFile("CURATOR_ID");
        String tokenName = Utility.readFromFile("TOKEN_NAME");
        String commandsChannelId = Utility.readFromFile("COMMANDS_CHANNEL_ID");

        // this token ID array increases in level (from left to right)
        List<String> tokenList = new ArrayList<>();
        int i = 1;

        while (true) {
            String token = Utility.readFromFile("TOKEN_LEVEL_" + i);

            if (token != null && !token.isEmpty()) {
                tokenList.add(token);
                i++;
            } else {
                break;
            }
        }

        String[] tokens = tokenList.toArray(new String[0]);

        // waiter
        EventWaiter waiter = new EventWaiter();
        builder.addEventListeners(waiter);

        // comments
        CommentWatcher comments = new CommentWatcher(tokenName, curatorID, targetChannelID, 0, false, waiter,
                spotifyApi);

        // commands
        TBBalanceCommand tbBalanceCommand = new TBBalanceCommand(tokenName, commandsChannelId);
        TBCommandsCommand tbCommandsCommand = new TBCommandsCommand(commandsChannelId);
        TBHelpCommand tbHelpCommand = new TBHelpCommand(helpChannelID, commandsChannelId);

        // add event listeners and build
        builder.addEventListeners(comments);
        builder.addEventListeners(tbBalanceCommand);
        builder.addEventListeners(tbCommandsCommand);
        builder.addEventListeners(tbHelpCommand);

        // build bot
        JDA jda = builder.build();

        // init spotify app authentication
        jda.retrieveUserById(curatorID).queue(bonjr -> {
            String initialAuthCode = Utility.readFromFile("SPOTIFY_AUTH_CODE");

            if (initialAuthCode == null) {
                spotifyApi.initiateAuthorization(bonjr); // Send the admin the auth link

                try {
                    if (!latch.await(30, TimeUnit.SECONDS)) {
                        System.out.println("Timeout while waiting for Spotify auth code. Please restart bot.");
                    } else {
                        // Read from file again in case it's changed after initiateAuthorization
                        String newAuthCode = Utility.readFromFile("SPOTIFY_AUTH_CODE");

                        if (newAuthCode != null) {
                            spotifyApi.setAuthorizationCode(newAuthCode);

                            spotifyApi.setupAccessAndRefreshToken(); // Get and set the access and refresh tokens

                            // bot is ready
                            comments.setBotIsReady(true);
                        }
                    }
                } catch (InterruptedException e) {
                    System.out.println("Error: " + e.getMessage());
                }
            } else if (spotifyApi.isTokenExpired()) {
                comments.setBotIsReady(spotifyApi.refreshToken());
            } else {
                // bot is ready
                comments.setBotIsReady(true);
            }

        });

    }

}