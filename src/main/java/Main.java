import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import api.SpotifyAPI;
import commands.*;
import events.CommentWatcher;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import utils.Curator;
import utils.Utility;

import static spark.Spark.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.security.auth.login.LoginException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws LoginException {
        // bot
        JDABuilder builder = JDABuilder.createDefault(Utility.readFromDatabase("TOKEN"));
        SpotifyAPI spotifyApi = SpotifyAPI.getInstance();

        CountDownLatch latch = new CountDownLatch(1);

        String portEnv = System.getenv("PORT");
        int portNumber;

        if (portEnv != null) {
            portNumber = Integer.parseInt(portEnv);
        } else {
            portNumber = 8080; // fallback to 8080 for local development
        }

        port(portNumber);

        get("/spotify-redirect", (req, res) -> {
            String code = req.queryParams("code");

            if (code != null && !code.isEmpty()) {
                Utility.saveToDatabase("SPOTIFY_AUTH_CODE", code); // Store one-time auth code

                latch.countDown();

                return "Authorization successful!";
            }

            return "No authorization code found in the request.";
        });

        // server specific inputs
        String targetChannelId = Utility.readFromDatabase("TARGET_CHANNEL_ID");
        String helpChannelId = Utility.readFromDatabase("HELP_CHANNEL_ID");
        String adminId = Utility.readFromDatabase("ADMIN");
        String tokenName = Utility.readFromDatabase("TOKEN_NAME");
        String commandsChannelId = Utility.readFromDatabase("COMMANDS_CHANNEL_ID");
        List<Curator> curators = Utility.readCuratorsFromDatabase().getCurators();

        // this token ID array increases in level (from left to right)
        List<String> tokenList = new ArrayList<>();
        int i = 1;

        while (true) {
            String token = Utility.readFromDatabase("TOKEN_LEVEL_" + i);

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
        CommentWatcher comments = new CommentWatcher(tokenName, adminId, curators, targetChannelId, helpChannelId, 0,
                false, false, waiter);

        // commands
        TBBalanceCommand tbBalanceCommand = new TBBalanceCommand(tokenName, commandsChannelId);
        TBCommandsCommand tbCommandsCommand = new TBCommandsCommand(commandsChannelId);
        TBHelpCommand tbHelpCommand = new TBHelpCommand(helpChannelId, commandsChannelId);
        TBReviewSubsCommand tbReviewSubsCommand = new TBReviewSubsCommand(curators, targetChannelId, commandsChannelId);

        // add event listeners and build
        builder.addEventListeners(comments);
        builder.addEventListeners(tbBalanceCommand);
        builder.addEventListeners(tbCommandsCommand);
        builder.addEventListeners(tbHelpCommand);
        builder.addEventListeners(tbReviewSubsCommand);

        // build bot
        JDA jda = builder.build();

        // init spotify app authentication
        if (adminId != null) {
            jda.retrieveUserById(adminId).queue(bonjr -> {
                String initialAuthCode = Utility.readFromDatabase("SPOTIFY_AUTH_CODE");

                if (initialAuthCode == null) {
                    try {
                        // Send the admin the auth link
                        String authUrl = spotifyApi.initiateAuthorization();

                        if (authUrl != null) {
                            Utility.sendSecretMessage(bonjr, authUrl, 60).queue();
                        }

                        if (!latch.await(60, TimeUnit.SECONDS)) {
                            logger.error("Timeout while waiting for Spotify auth code. Please restart bot.");
                        } else {
                            // Read from file again in case it's changed after initiateAuthorization
                            String newAuthCode = Utility.readFromDatabase("SPOTIFY_AUTH_CODE");

                            if (newAuthCode != null) {
                                spotifyApi.setAuthorizationCode(newAuthCode);

                                spotifyApi.setupAccessAndRefreshToken(); // Get and set the access and refresh tokens

                                // bot is ready
                                comments.setBotIsReady(true);

                                logger.info("Bot is ready.");
                            }
                        }
                    } catch (Exception e) {
                        logger.error("Error: " + e.getMessage());
                    }
                } else if (spotifyApi.isAccessExpired()) {
                    if (spotifyApi.refreshTokens()) {
                        comments.setBotIsReady(true);

                        logger.info("Bot is ready.");
                    }
                } else {
                    // bot is ready
                    comments.setBotIsReady(true);

                    logger.info("Bot is ready.");
                }

            });
        } else {
            logger.error("No admin ID provided. Authentication is not possible!");
        }

    }

}
