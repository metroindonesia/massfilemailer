package massfilemailer;

import java.io.*;
import java.util.Properties;

public class ConfigManager {
    private static final String CONFIG_FILE = "smtp_config.properties";
    private static Properties properties = new Properties();

    static {
        loadConfig();
    }

    public static void loadConfig() {
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try (InputStream input = new FileInputStream(file)) {
                properties.load(input);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void saveConfig(String host, String port, String user, String pass, String auth, String starttls, String senderName, String pdfPrefix) {
        properties.setProperty("mail.smtp.host", host);
        properties.setProperty("mail.smtp.port", port);
        properties.setProperty("mail.smtp.user", user);
        properties.setProperty("mail.smtp.pass", pass);
        properties.setProperty("mail.smtp.auth", auth);
        properties.setProperty("mail.smtp.starttls.enable", starttls);
        properties.setProperty("mail.sender.name", senderName);
        properties.setProperty("pdf.prefix", pdfPrefix);

        try (OutputStream output = new FileOutputStream(CONFIG_FILE)) {
            properties.store(output, "SMTP Configuration");
        } catch (IOException io) {
            io.printStackTrace();
        }
    }

    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}
