package events;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.RestAction;

public class Utility {
    private static final Properties properties = new Properties();
    
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
    public static RestAction<Void> sendSecretMessage(User user, String content, int time) {
        return user.openPrivateChannel() // RestAction<PrivateChannel>
                .flatMap(channel -> channel.sendMessage(content)) // RestAction<Message>
                .delay(time, TimeUnit.SECONDS) // RestAction<Message> with delayed response
                .flatMap(Message::delete); // RestAction<Void> (executed 300 seconds after sending)
    }

    public static void saveToFile(String key, String value) {
        properties.setProperty(key, value);

        try (FileOutputStream outputStream = new FileOutputStream("config.properties")) {
            properties.store(outputStream, "Configurations");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static String readFromFile(String key) {
        try (FileInputStream inputStream = new FileInputStream("config.properties")) {
            properties.load(inputStream);

            return properties.getProperty(key);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());

            return null;
        }
    }

}