package utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.RestAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Utility {
    private static final Properties properties = new Properties();
    private static final Logger logger = LoggerFactory.getLogger(Utility.class);

    /**
     * Sends a direct message to the user. Delete after a given amount of time.
     *
     * @param user    the submitting user
     * @param content private message sent by bot to user
     * @param time    time before message deletion
     *
     * @return RestAction - Type: PrivateChannel
     *         Retrieves the PrivateChannel to use to directly message this User.
     */
    public static RestAction<Void> sendSecretMessage(User user, @Nonnull String content, int time) {
        return user.openPrivateChannel() // RestAction<PrivateChannel>
                .flatMap(channel -> channel.sendMessage(content)) // RestAction<Message>
                .delay(time, TimeUnit.SECONDS) // RestAction<Message> with delayed response
                .flatMap(Message::delete); // RestAction<Void> (executed x seconds after sending)
    }

    public static void saveToFile(String key, String value) {
        properties.setProperty(key, value);

        try (FileOutputStream outputStream = new FileOutputStream("config.properties")) {
            properties.store(outputStream, "Configurations");
        } catch (Exception e) {
            logger.error("Error: " + e.getMessage());
        }
    }

    public static String readFromFile(String key) {
        try (FileInputStream inputStream = new FileInputStream("config.properties")) {
            properties.load(inputStream);

            return properties.getProperty(key);
        } catch (Exception e) {
            logger.error("Error: " + e.getMessage());

            return null;
        }
    }

    public static void saveToDatabase(String key, String value) {
        String sql = "INSERT INTO config(key, value) VALUES(?, ?) ON CONFLICT (key) DO UPDATE SET value = ?";

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, key);
            stmt.setString(2, value);
            stmt.setString(3, value);

            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error: " + e.getMessage());
        }
    }

    public static String readFromDatabase(String key) {
        String sql = "SELECT value FROM config WHERE key = ?";

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, key);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString("value");
            }
        } catch (SQLException e) {
            logger.error("Error: " + e.getMessage());
        }

        return null;
    }

    private static Connection getConnection() throws SQLException {
        String dbUrl = toJdbcUrl(System.getenv("DATABASE_URL"));

        return DriverManager.getConnection(dbUrl);
    }

    private static String toJdbcUrl(String databaseUrl) {
        if (databaseUrl == null || !databaseUrl.startsWith("postgresql://")) {
            return null;
        }

        int protocolEnd = databaseUrl.indexOf("://");
        int credentialsEnd = databaseUrl.lastIndexOf("@");

        String credentials = databaseUrl.substring(protocolEnd + 3, credentialsEnd);
        String[] splitCredentials = credentials.split(":");

        String user = splitCredentials[0];
        String password = splitCredentials[1];

        String afterCredentials = databaseUrl.substring(credentialsEnd + 1);
        String host = afterCredentials.substring(0, afterCredentials.indexOf(":"));
        String portAndDatabase = afterCredentials.substring(afterCredentials.indexOf(":") + 1);

        String jdbcUrl = "jdbc:postgresql://" + host + ":" + portAndDatabase + "?user=" + user + "&password="
                + password;

        return jdbcUrl;
    }

    public static CuratorList readCuratorsFromDatabase() {
        String json = readFromDatabase("CURATORS");

        ObjectMapper mapper = new ObjectMapper();

        try {
            return mapper.readValue(json, CuratorList.class);
        } catch (JsonProcessingException e) {
            // Handle the exception
            return null;
        }
    }

}