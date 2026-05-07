package massfilemailer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class SmtpConfigDialog {

    public static void show(Stage owner) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        dialog.setTitle("SMTP Server Configuration");

        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f4f7f6;");

        Label titleLabel = new Label("Pengaturan SMTP");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);

        // Fields
        TextField hostField = new TextField(ConfigManager.getProperty("mail.smtp.host", "smtp.gmail.com"));
        TextField portField = new TextField(ConfigManager.getProperty("mail.smtp.port", "587"));
        TextField userField = new TextField(ConfigManager.getProperty("mail.smtp.user", ""));
        PasswordField passField = new PasswordField();
        passField.setText(ConfigManager.getProperty("mail.smtp.pass", ""));
        TextField senderNameField = new TextField(ConfigManager.getProperty("mail.sender.name", "Mass File Mailer"));
        TextField pdfPrefixField = new TextField(ConfigManager.getProperty("pdf.prefix", "protected_"));

        CheckBox tlsCheck = new CheckBox("Enable STARTTLS (Gunakan untuk Port 587)");
        tlsCheck.setSelected(Boolean.parseBoolean(ConfigManager.getProperty("mail.smtp.starttls.enable", "true")));
        
        CheckBox authCheck = new CheckBox("Enable SMTP Auth");
        authCheck.setSelected(Boolean.parseBoolean(ConfigManager.getProperty("mail.smtp.auth", "true")));

        // Add to grid
        grid.add(new Label("SMTP Host:"), 0, 0);
        grid.add(hostField, 1, 0);
        grid.add(new Label("Port:"), 0, 1);
        grid.add(portField, 1, 1);
        grid.add(new Label("Email User:"), 0, 2);
        grid.add(userField, 1, 2);
        grid.add(new Label("Password/App Password:"), 0, 3);
        grid.add(passField, 1, 3);
        grid.add(new Label("Nama Pengirim:"), 0, 4);
        grid.add(senderNameField, 1, 4);
        grid.add(new Label("Prefix File PDF:"), 0, 5);
        grid.add(pdfPrefixField, 1, 5);
        grid.add(tlsCheck, 1, 6);
        grid.add(authCheck, 1, 7);

        // Buttons
        Button saveBtn = new Button("Simpan Konfigurasi");
        saveBtn.getStyleClass().add("action-button");
        saveBtn.setOnAction(e -> {
            ConfigManager.saveConfig(
                hostField.getText(),
                portField.getText(),
                userField.getText(),
                passField.getText(),
                String.valueOf(authCheck.isSelected()),
                String.valueOf(tlsCheck.isSelected()),
                senderNameField.getText(),
                pdfPrefixField.getText()
            );
            dialog.close();
        });

        Button cancelBtn = new Button("Batal");
        cancelBtn.setOnAction(e -> dialog.close());

        HBox btnBox = new HBox(10, cancelBtn, saveBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(titleLabel, grid, btnBox);

        Scene scene = new Scene(root, 400, 450);
        // Reuse the main style
        scene.getStylesheets().add(SmtpConfigDialog.class.getResource("/massfilemailer/style.css").toExternalForm());
        
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
