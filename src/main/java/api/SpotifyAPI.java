package api;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hc.core5.http.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import exceptions.DuplicateTrackException;
import exceptions.MissingTokenException;
import exceptions.TrackNotFoundException;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.SpotifyHttpManager;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRefreshRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import se.michaelthelin.spotify.requests.data.playlists.AddItemsToPlaylistRequest;
import se.michaelthelin.spotify.requests.data.tracks.GetTrackRequest;
import utils.ReactionInfo;
import utils.Submission;
import utils.Utility;

public class SpotifyAPI {
    private static SpotifyAPI instance;
    private SpotifyApi spotifyApi;
    private String authorizationCode;
    private AuthorizationCodeCredentials authorizationCodeCredentials;
    private String accessToken;
    private String refreshToken;

    private static final URI redirectUri = SpotifyHttpManager.makeUri(Utility.readFromDatabase("URI_STRING"));
    private static final Logger logger = LoggerFactory.getLogger(SpotifyAPI.class);
    private static final String playlistId = Utility.readFromDatabase("PLAYLIST_ID");
    private static final String approvedPlaylistId = Utility.readFromDatabase("APPROVED_PLAYLIST_ID");

    // constructor
    private SpotifyAPI() {
        // on boot grab current tokens
        accessToken = Utility.readFromDatabase("AUTH_ACCESS_TOKEN");
        refreshToken = Utility.readFromDatabase("AUTH_REFRESH_TOKEN");

        // build
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

    public Boolean addToPlaylist(String trackLink, String userId, String messageId) {
        if (isAccessExpired()) {
            refreshTokens();
        }

        Pattern pattern = Pattern.compile("(spotify:track:|\\/track\\/)([a-zA-Z0-9]+)(\\?|\\s|$)");
        Matcher matcher = pattern.matcher(trackLink);

        if (matcher.find()) {
            String trackId = matcher.group(2);

            try {
                Track track = getTrack(trackId);

                if (track == null) {
                    throw new TrackNotFoundException("Valid Spotify track link, but invalid track ID: " + trackId);
                }

                String[] trackUri = new String[] { getTrack(trackId).getUri() };

                // Check for duplicates in both submissions and approved playlists
                Optional<Boolean> duplicateInSubmissions = isDuplicate(playlistId, trackId);
                Optional<Boolean> duplicateInApproved = isDuplicate(approvedPlaylistId, trackId);

                if (duplicateInSubmissions.isPresent() && duplicateInApproved.isPresent()) {
                    if (duplicateInApproved.get()) {
                        // Track is already approved
                        throw new DuplicateTrackException(
                                "This track has already been approved and is in the approved playlist!");
                    } else if (duplicateInSubmissions.get()) {
                        // Track is already in submissions queue
                        throw new DuplicateTrackException(
                                "Duplicate entry found. This track is already in queue for review!");
                    } else {
                        // Track is not in either playlist - safe to add
                        AddItemsToPlaylistRequest addItemsToPlaylistRequest = spotifyApi
                                .addItemsToPlaylist(playlistId, trackUri)
                                .position(0)
                                .build();

                        addItemsToPlaylistRequest.execute();
                        logger.info("Track added to playlist.");

                        // Add to submissions table in database
                        Utility.saveTrackSubmission(trackId, userId, messageId);

                        return true;
                    }

                } else { // error thrown when checking for duplicate
                    return false;
                }

            } catch (IOException | SpotifyWebApiException | ParseException e) {
                logger.error("Error: " + e.getMessage());

                return false;
            }

        } else {
            // Handle the case where no match was found
            logger.warn("Provided URL does not match expected format(s): " + trackLink);

            return false;
        }

    }

    public String initiateAuthorization() throws Exception {
        try {
            String authorizeUrl = spotifyApi.authorizationCodeUri()
                    .scope("playlist-modify-public playlist-modify-private playlist-read-private") // scopes
                    .show_dialog(true)
                    .build()
                    .execute()
                    .toString();

            return authorizeUrl;
        } catch (Exception e) {
            throw new Exception("Failed to build authorization URL.");
        }
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

            logger.info("Token lifespan: " + expiresIn + " seconds = " + expiresIn / 60 + " minutes");

        } catch (ParseException | SpotifyWebApiException | IOException e) {
            logger.error("Error: " + e.getMessage());
        }
    }

    public boolean isAccessExpired() {
        try {
            if (accessToken == null || refreshToken == null) {
                throw new MissingTokenException("Access/Refresh token(s) missing.");
            }

            long authTime = Long.parseLong(Utility.readFromDatabase("AUTH_TIME"));
            int expiresIn = Integer.parseInt(Utility.readFromDatabase("EXPIRES_IN"));
            long currentTimeInSeconds = System.currentTimeMillis() / 1000;

            long timeElapsed = (currentTimeInSeconds - authTime);

            if (timeElapsed <= expiresIn) {
                logger.info("Time since last authentication: " + timeElapsed);
            }

            return timeElapsed > expiresIn;
        } catch (MissingTokenException e) { // refresh token will handle both expired and does not exist cases
            logger.error("Error: " + e.getMessage());

            return true;
        }

    }

    public boolean refreshTokens() {
        String clientId = Utility.readFromDatabase("APP_CLIENT_ID");
        String secret = Utility.readFromDatabase("CLIENT_SECRET");

        AuthorizationCodeRefreshRequest refreshRequest = spotifyApi
                .authorizationCodeRefresh(clientId, secret, refreshToken).build();

        try {
            authorizationCodeCredentials = refreshRequest.execute();

            int expiresIn = authorizationCodeCredentials.getExpiresIn();
            accessToken = authorizationCodeCredentials.getAccessToken();
            String newRefreshToken = (authorizationCodeCredentials.getRefreshToken() != null)
                    ? authorizationCodeCredentials.getRefreshToken()
                    : refreshToken;

            Utility.saveToDatabase("AUTH_TIME", Long.toString(System.currentTimeMillis() /
                    1000));
            Utility.saveToDatabase("AUTH_ACCESS_TOKEN", accessToken);
            Utility.saveToDatabase("EXPIRES_IN", Integer.toString(expiresIn));

            spotifyApi.setAccessToken(accessToken);

            logger.info("Access token refreshed.");

            if (newRefreshToken != refreshToken) {
                Utility.saveToDatabase("AUTH_REFRESH_TOKEN", newRefreshToken);
                spotifyApi.setRefreshToken(newRefreshToken);

                logger.info("Refresh token refreshed.");
            }

            return true;
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            logger.error("Error: " + e.getMessage());

            return false;
        }
    }

    public List<ReactionInfo> processSubmissions() throws Exception {
        logger.info("=== Starting processSubmissions() ===");
        
        Set<String> approvedTrackIds = fetchPlaylistTracks(approvedPlaylistId);
        Set<String> submissionTrackIds = fetchPlaylistTracks(playlistId);

        logger.info("Approved playlist ({}) contains {} tracks", approvedPlaylistId, approvedTrackIds.size());
        logger.info("Submissions playlist ({}) contains {} tracks", playlistId, submissionTrackIds.size());
        
        if (!approvedTrackIds.isEmpty()) {
            logger.info("Approved track IDs: {}", approvedTrackIds);
        }
        if (!submissionTrackIds.isEmpty()) {
            logger.info("Submission track IDs: {}", submissionTrackIds);
        }

        // Fetch all submissions from the database
        List<Submission> submissions = Utility.fetchAllSubmissions();

        logger.info("Database contains {} submissions", submissions.size());

        if (submissions.isEmpty()) {
            throw new Exception("Database does not contain any submissions.");
        }

        // Reactions list
        List<ReactionInfo> reactions = new ArrayList<>();

        for (Submission submission : submissions) {
            String trackId = submission.getTrackId();
            String userId = submission.getUserId();
            String messageId = submission.getMessageId();
            int submissionId = submission.getSubmissionId();

            logger.info("Processing submission {}: trackId={}, userId={}, messageId={}", 
                       submissionId, trackId, userId, messageId);

            if (approvedTrackIds.contains(trackId)) {
                // Track was approved
                logger.info("✅ Track {} found in APPROVED playlist - adding checkmark and removing from DB", trackId);
                reactions.add(new ReactionInfo(userId, messageId, "✅"));
                Utility.deleteSubmission(submissionId);
            } else if (!submissionTrackIds.contains(trackId)) {
                // Track was not found in the submissions playlist, implying rejection or removal
                logger.info("❌ Track {} NOT found in submissions playlist - adding checkmark and removing from DB", trackId);
                reactions.add(new ReactionInfo(userId, messageId, "✅"));
                Utility.deleteSubmission(submissionId);
            } else {
                logger.info("⏳ Track {} still in submissions playlist - no action taken", trackId);
            }

            // If a track is still in the submissions playlist, no action is taken as it's pending review

        }

        logger.info("=== processSubmissions() complete: {} reactions to add ===", reactions.size());
        // clean up

        return reactions;
    }

    private Track getTrack(String id) {
        // sync call for metadata using spotify API
        GetTrackRequest getTrackRequest = spotifyApi.getTrack(id)
                .build();

        try {
            Track track = getTrackRequest.execute();

            return track;
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            logger.error("Error: " + e.getMessage());
        }

        return null;
    }

    private Optional<Boolean> isDuplicate(String playlistId, String trackId) {
        try {
            Set<String> playlistTrackIds = fetchPlaylistTracks(playlistId);

            return Optional.of(playlistTrackIds.contains(trackId));
        } catch (Exception e) {
            logger.error("Error checking for duplicate: " + e.getMessage());

            // Return Optional.empty() to indicate an error condition
            return Optional.empty();
        }
    }

    private Set<String> fetchPlaylistTracks(String playlistId) {
        logger.info("Fetching tracks from playlist: {}", playlistId);
        Set<String> trackIds = new HashSet<>();
        int offset = 0;
        final int limit = 100; // Spotify's max limit per request
        Paging<PlaylistTrack> paging;

        try {
            do {
                paging = spotifyApi.getPlaylistsItems(playlistId)
                        .limit(limit)
                        .offset(offset)
                        .build()
                        .execute();

                logger.info("Fetched {} items from playlist {} (offset: {}, total: {})", 
                           paging.getItems().length, playlistId, offset, paging.getTotal());

                for (PlaylistTrack playlistTrack : paging.getItems()) {
                    String trackId = playlistTrack.getTrack().getId();
                    trackIds.add(trackId);
                    logger.debug("Found track in playlist {}: {}", playlistId, trackId);
                }

                offset += limit;
            } while (offset < paging.getTotal());

            logger.info("Successfully fetched {} unique tracks from playlist {}", trackIds.size(), playlistId);
            return trackIds; // return set of track ids
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            logger.error("Error fetching tracks from playlist {}: {}", playlistId, e.getMessage());

            return trackIds; // empty set if error
        }
    }
}
