package massfilemailer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class App extends Application {

    private Stage primaryStage;
    private ProgressBar progressBar;
    private Label statusLabel;
    private TextArea logArea;
    private Button sendBtn; // Simpan sebagai variabel class

    @Override
    public void start(Stage stage) throws Exception {
        this.primaryStage = stage;
        showWelcomeScreen();
    }

    private void showWelcomeScreen() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));
        
        Label titleLabel = new Label("Mass File Mailer");
        titleLabel.getStyleClass().add("title-label");
        Label descLabel = new Label("Siap untuk mengirim file secara massal.");
        descLabel.getStyleClass().add("desc-label");

        Button startButton = new Button("Mulai Sekarang");
        startButton.getStyleClass().add("action-button");
        startButton.setOnAction(e -> showMainDashboard());

        root.getChildren().addAll(titleLabel, descLabel, startButton);
        Scene scene = new Scene(root, 450, 350);
        applyStyle(scene);
        primaryStage.setTitle("Mass File Mailer - v1.0");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void showMainDashboard() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));

        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);
        Label headerLabel = new Label("Konfigurasi Pengiriman");
        headerLabel.getStyleClass().add("title-label");
        headerLabel.setStyle("-fx-font-size: 18px;");
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button settingsBtn = new Button("⚙ Settings SMTP");
        settingsBtn.setOnAction(e -> SmtpConfigDialog.show(primaryStage));
        headerBox.getChildren().addAll(headerLabel, spacer, settingsBtn);

        VBox dirSection = createSection("1. Folder Sumber File (Data)", "Pilih folder...");
        TextField dirField = (TextField) ((HBox) dirSection.getChildren().get(1)).getChildren().get(0);
        Button browseDir = (Button) ((HBox) dirSection.getChildren().get(1)).getChildren().get(1);
        browseDir.setOnAction(e -> {
            File f = new DirectoryChooser().showDialog(primaryStage);
            if (f != null) dirField.setText(f.getAbsolutePath());
        });

        VBox excelSection = createSection("2. Daftar Penerima (Excel .xlsx)", "Pilih file excel...");
        TextField excelField = (TextField) ((HBox) excelSection.getChildren().get(1)).getChildren().get(0);
        Button browseExcel = (Button) ((HBox) excelSection.getChildren().get(1)).getChildren().get(1);
        browseExcel.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
            File f = fc.showOpenDialog(primaryStage);
            if (f != null) excelField.setText(f.getAbsolutePath());
        });

        Label subjectLbl = new Label("3. Judul Email (gunakan {nama} untuk personalisasi):");
        subjectLbl.setStyle("-fx-font-weight: bold;");
        TextField subjectField = new TextField();
        subjectField.setText("File untuk {nama}");
        subjectField.setPromptText("Masukkan judul email...");

        Label msgLbl = new Label("4. Isi Pesan Email (gunakan {nama} untuk personalisasi):");
        msgLbl.setStyle("-fx-font-weight: bold;");
        TextArea msgArea = new TextArea();
        msgArea.setText("Dear {nama}\n\nplease see attachment");
        msgArea.setPrefRowCount(5);

        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        statusLabel = new Label("Siap.");
        
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPromptText("Log error/status akan muncul di sini...");
        logArea.setPrefRowCount(5);
        logArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 11px;");

        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        Button backBtn = new Button("Kembali");
        backBtn.setOnAction(e -> showWelcomeScreen());
        
        sendBtn = new Button("Proses & Kirim");
        sendBtn.getStyleClass().add("action-button");
        sendBtn.setPrefWidth(150);
        sendBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Konfirmasi Pengiriman");
            alert.setHeaderText("Anda akan mengirim email massal.");
            alert.setContentText("Pastikan semua data sudah benar. Lanjutkan?");

            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    sendBtn.setDisable(true);
                    logArea.clear();
                    new Thread(() -> {
                        MailHandler.handleSend(dirField.getText(), excelField.getText(), subjectField.getText(), msgArea.getText(), this);
                    }).start();
                }
            });
        });

        actionBox.getChildren().addAll(backBtn, sendBtn);
        layout.getChildren().addAll(headerBox, dirSection, excelSection, subjectLbl, subjectField, msgLbl, msgArea, 
                                   new Label("Status Pengiriman:"), progressBar, statusLabel, logArea, actionBox);

        Scene scene = new Scene(layout, 650, 750);
        applyStyle(scene);
        primaryStage.setScene(scene);
    }

    public void updateProgress(double progress, String status) {
        Platform.runLater(() -> {
            progressBar.setProgress(progress);
            statusLabel.setText(status);
        });
    }

    public void appendLog(String text) {
        Platform.runLater(() -> logArea.appendText(text + "\n"));
    }

    public void setSendButtonDisable(boolean disable) {
        Platform.runLater(() -> sendBtn.setDisable(disable));
    }

    private VBox createSection(String labelText, String promptText) {
        VBox section = new VBox(5);
        Label label = new Label(labelText);
        label.setStyle("-fx-font-weight: bold;");
        HBox inputBox = new HBox(10);
        TextField textField = new TextField();
        textField.setEditable(false);
        HBox.setHgrow(textField, Priority.ALWAYS);
        Button browseBtn = new Button("Telusuri...");
        inputBox.getChildren().addAll(textField, browseBtn);
        section.getChildren().addAll(label, inputBox);
        return section;
    }

    private void applyStyle(Scene scene) {
        scene.getStylesheets().add(getClass().getResource("/massfilemailer/style.css").toExternalForm());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
