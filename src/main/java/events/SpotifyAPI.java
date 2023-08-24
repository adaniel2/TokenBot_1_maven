package events;

import java.io.IOException;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hc.core5.http.ParseException;

import net.dv8tion.jda.api.entities.User;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.SpotifyHttpManager;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRefreshRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import se.michaelthelin.spotify.requests.data.playlists.AddItemsToPlaylistRequest;
import se.michaelthelin.spotify.requests.data.tracks.GetTrackRequest;

public class SpotifyAPI {
    private static SpotifyAPI instance;
    private SpotifyApi spotifyApi;
    private static final URI redirectUri = SpotifyHttpManager.makeUri(Utility.readFromDatabase("URI_STRING"));
    private String authorizationCode;
    private AuthorizationCodeCredentials authorizationCodeCredentials;
    private String accessToken;
    private String refreshToken;

    // constructor
    private SpotifyAPI() {
        // on boot grab current tokens
        accessToken = Utility.readFromDatabase("AUTH_ACCESS_TOKEN");
        refreshToken = Utility.readFromDatabase("AUTH_REFRESH_TOKEN");

        // authentication
        this.spotifyApi = new SpotifyApi.Builder()
                .setClientId(Utility.readFromDatabase("APP_CLIENT_ID"))
                .setClientSecret(Utility.readFromDatabase("CLIENT_SECRET"))
                .setRedirectUri(redirectUri)
                .build();

        // set to current tokens (could be null)
        spotifyApi.setAccessToken(accessToken);
        spotifyApi.setRefreshToken(refreshToken);
    }

    public void setAuthorizationCode(String code) {
        this.authorizationCode = code;
    }

    public static synchronized SpotifyAPI getInstance() {
        if (instance == null) {
            instance = new SpotifyAPI();
        }

        return instance;
    }

    public Boolean addToPlaylist(String trackLink) {
        if (isTokenExpired()) {
            refreshToken();
        }

        Pattern pattern = Pattern.compile("/(track|album)/([a-zA-Z0-9]+)\\?");
        Matcher matcher = pattern.matcher(trackLink);

        if (matcher.find()) {
            String trackId = matcher.group(2);

            try {
                String[] trackUri = new String[] { getTrack(trackId).getUri() };
                String playlistId = Utility.readFromDatabase("PLAYLIST_ID");

                AddItemsToPlaylistRequest addItemsToPlaylistRequest = spotifyApi
                        .addItemsToPlaylist(playlistId, trackUri)
                        .position(0)
                        .build();

                addItemsToPlaylistRequest.execute();

                return true;
            } catch (IOException | SpotifyWebApiException | ParseException | NullPointerException e) {
                System.out.println("Error: " + e.getMessage());

                return false;
            }

        } else {
            // Handle the case where no match was found
            System.out.println("No match found in the provided URL: " + trackLink);

            return false;
        }

    }

    public void initiateAuthorization(User bonjr) {
        String authorizeUrl = spotifyApi.authorizationCodeUri()
                .scope("playlist-modify-public playlist-modify-private playlist-read-private") // scopes
                .show_dialog(true)
                .build()
                .execute()
                .toString();

        Utility.sendSecretMessage(bonjr, authorizeUrl, 30).queue();
    }

    public void setupAccessAndRefreshToken() {
        AuthorizationCodeRequest authorizationCodeRequest = spotifyApi.authorizationCode(authorizationCode)
                .build();

        try {
            authorizationCodeCredentials = authorizationCodeRequest.execute();

            int expiresIn = authorizationCodeCredentials.getExpiresIn();
            accessToken = authorizationCodeCredentials.getAccessToken();
            refreshToken = authorizationCodeCredentials.getRefreshToken();

            Utility.saveToDatabase("AUTH_TIME", Long.toString(System.currentTimeMillis() / 1000));
            Utility.saveToDatabase("AUTH_ACCESS_TOKEN", accessToken);
            Utility.saveToDatabase("AUTH_REFRESH_TOKEN", refreshToken);
            Utility.saveToDatabase("EXPIRES_IN", Integer.toString(expiresIn));

            spotifyApi.setAccessToken(accessToken);
            spotifyApi.setRefreshToken(refreshToken);

            System.out.println("Token lifespan: " + expiresIn + " seconds = " + expiresIn / 60 + " minutes");

        } catch (ParseException | SpotifyWebApiException | IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public boolean isTokenExpired() {
        try {
            if (accessToken == null || refreshToken == null) {
                throw new MissingTokenException("Access/Refresh token(s) missing.");
            }

            long authTime = Long.parseLong(Utility.readFromDatabase("AUTH_TIME"));
            int expiresIn = Integer.parseInt(Utility.readFromDatabase("EXPIRES_IN"));
            long currentTimeInSeconds = System.currentTimeMillis() / 1000;

            long timeElapsed = (currentTimeInSeconds - authTime);

            System.out.println("Time elapsed: " + timeElapsed);

            return timeElapsed > expiresIn;
        } catch (MissingTokenException e) { // refresh token will handle both expired and does not exist cases
            System.out.println("Error: " + e.getMessage());

            return true;
        }

    }

    public boolean refreshToken() {
        String clientId = Utility.readFromDatabase("APP_CLIENT_ID");
        String secret = Utility.readFromDatabase("CLIENT_SECRET");

        AuthorizationCodeRefreshRequest refreshRequest = spotifyApi
                .authorizationCodeRefresh(clientId, secret, refreshToken).build();

        try {
            authorizationCodeCredentials = refreshRequest.execute();

            int expiresIn = authorizationCodeCredentials.getExpiresIn();
            accessToken = authorizationCodeCredentials.getAccessToken();
            refreshToken = (authorizationCodeCredentials.getRefreshToken() != null)
                    ? authorizationCodeCredentials.getRefreshToken()
                    : refreshToken;

            Utility.saveToDatabase("AUTH_TIME", Long.toString(System.currentTimeMillis() /
                    1000));
            Utility.saveToDatabase("AUTH_ACCESS_TOKEN", accessToken);
            Utility.saveToDatabase("AUTH_REFRESH_TOKEN", refreshToken);
            Utility.saveToDatabase("EXPIRES_IN", Integer.toString(expiresIn));

            spotifyApi.setAccessToken(accessToken);
            spotifyApi.setRefreshToken(refreshToken);

            return true;
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            System.out.println("Error: " + e.getMessage());

            return false;
        }
    }

    private Track getTrack(String id) {
        // sync call for metadata using spotify API
        GetTrackRequest getTrackRequest = spotifyApi.getTrack(id)
                .build();

        try {
            Track track = getTrackRequest.execute();

            return track;
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            System.out.println("Error: " + e.getMessage());
        }

        return null;
    }

    private class MissingTokenException extends Exception {
        public MissingTokenException(String message) {
            super(message);
        }
    }

}