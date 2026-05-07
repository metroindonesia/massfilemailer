package massfilemailer;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class MailHandler {

    public static void handleSend(String directoryPath, String excelPath, String subject, String messageBody, App appUI) {
        if (directoryPath == null || directoryPath.isEmpty() || excelPath == null || excelPath.isEmpty() || subject == null || subject.isEmpty()) {
            appUI.appendLog("Error: Folder, File Excel, atau Judul Email belum diisi!");
            return;
        }

        try {
            appUI.updateProgress(0, "Membaca Excel...");
            List<Recipient> recipients = loadRecipients(excelPath);
            
            // --- TAHAP 1: VALIDASI FILE ---
            appUI.updateProgress(0.1, "Memvalidasi keberadaan file lampiran...");
            List<String> missingFiles = new ArrayList<>();
            for (Recipient r : recipients) {
                File file = new File(directoryPath, r.getAttachment());
                if (!file.exists()) {
                    missingFiles.add("Nama: " + r.getNama() + " | Email: " + r.getEmail() + " | File Hilang: " + r.getAttachment());
                }
            }

            if (!missingFiles.isEmpty()) {
                appUI.appendLog("VALIDASI GAGAL! Beberapa file tidak ditemukan:");
                for (String error : missingFiles) {
                    appUI.appendLog("- " + error);
                }
                appUI.updateProgress(0, "Pengiriman dibatalkan.");
                appUI.setSendButtonDisable(false); // Aktifkan lagi agar user bisa benerin file
                return;
            }

            // --- TAHAP 2: PENGIRIMAN ---
            appUI.appendLog("Validasi sukses. Memulai pengiriman...");
            Session session = createEmailSession();
            int total = recipients.size();
            int successCount = 0;

            for (int i = 0; i < total; i++) {
                Recipient r = recipients.get(i);
                double progress = (double) (i + 1) / total;
                appUI.updateProgress(progress, "Mengirim ke: " + r.getEmail() + " (" + (i + 1) + "/" + total + ")");

                try {
                    sendEmailWithAttachment(session, directoryPath, r, subject, messageBody);
                    successCount++;
                    
                    // Jeda 2 detik agar tidak dianggap spam/membebani server
                    Thread.sleep(2000); 
                } catch (Exception e) {
                    appUI.appendLog("Gagal kirim ke " + r.getEmail() + ": " + e.getMessage());
                }
            }

            appUI.updateProgress(1.0, "Selesai! " + successCount + "/" + total + " terkirim.");
            appUI.appendLog("Proses Selesai. " + successCount + " email berhasil dikirim.");

        } catch (Exception e) {
            appUI.appendLog("Fatal Error: " + e.getMessage());
            appUI.updateProgress(0, "Error.");
            appUI.setSendButtonDisable(false);
        }
    }

    private static void sendEmailWithAttachment(Session session, String dirPath, Recipient r, String subject, String body) throws Exception {
        String fromEmail = ConfigManager.getProperty("mail.smtp.user", "");
        String senderName = ConfigManager.getProperty("mail.sender.name", "Mass File Mailer");
        
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(fromEmail, senderName));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(r.getEmail()));
        message.setSubject(subject.replace("{nama}", r.getNama()));

        MimeBodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setText(body.replace("{nama}", r.getNama()));

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(messageBodyPart);

        File fileToAttach = new File(dirPath, r.getAttachment());
        File finalFile = fileToAttach;
        File tempFile = null;

        if (r.getPdfPassword() != null && !r.getPdfPassword().isEmpty() && r.getAttachment().toLowerCase().endsWith(".pdf")) {
            tempFile = protectPdf(fileToAttach, r.getPdfPassword());
            finalFile = tempFile;
        }

        MimeBodyPart attachPart = new MimeBodyPart();
        attachPart.attachFile(finalFile);
        
        // Set nama file agar terlihat bersih di email (tanpa angka random dari temp file)
        String prefix = ConfigManager.getProperty("pdf.prefix", "protected_");
        attachPart.setFileName(prefix + r.getAttachment());
        
        multipart.addBodyPart(attachPart);

        message.setContent(multipart);
        Transport.send(message);

        if (tempFile != null && tempFile.exists()) tempFile.delete();
    }

    private static File protectPdf(File original, String password) throws IOException {
        String prefix = ConfigManager.getProperty("pdf.prefix", "protected_");
        // createTempFile butuh prefix minimal 3 karakter
        String safePrefix = prefix.length() < 3 ? prefix + "___" : prefix;
        
        File temp = File.createTempFile(safePrefix, "_" + original.getName());
        try (PDDocument document = PDDocument.load(original)) {
            AccessPermission ap = new AccessPermission();
            StandardProtectionPolicy spp = new StandardProtectionPolicy(password, password, ap);
            spp.setEncryptionKeyLength(128);
            spp.setPermissions(ap);
            document.protect(spp);
            document.save(temp);
        }
        return temp;
    }

    private static Session createEmailSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host", ConfigManager.getProperty("mail.smtp.host", ""));
        props.put("mail.smtp.port", ConfigManager.getProperty("mail.smtp.port", "587"));
        props.put("mail.smtp.auth", ConfigManager.getProperty("mail.smtp.auth", "true"));
        props.put("mail.smtp.starttls.enable", ConfigManager.getProperty("mail.smtp.starttls.enable", "true"));

        return Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(
                        ConfigManager.getProperty("mail.smtp.user", ""),
                        ConfigManager.getProperty("mail.smtp.pass", "")
                );
            }
        });
    }

    private static List<Recipient> loadRecipients(String path) throws Exception {
        List<Recipient> list = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(new File(path));
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            
            // Validasi Header (email, nama, attachment, set_pdf_password)
            String[] expected = {"email", "nama", "attachment", "set_pdf_password"};
            for (int i = 0; i < expected.length; i++) {
                Cell cell = headerRow.getCell(i);
                String val = (cell == null) ? "" : cell.getStringCellValue().trim();
                if (!expected[i].equalsIgnoreCase(val)) {
                    throw new Exception("Header kolom " + (i+1) + " salah! Harusnya: " + expected[i]);
                }
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String email = getCellValue(row.getCell(0));
                String nama = getCellValue(row.getCell(1));
                String attachment = getCellValue(row.getCell(2));
                String password = getCellValue(row.getCell(3));
                if (!email.isEmpty()) list.add(new Recipient(email, nama, attachment, password));
            }
        }
        return list;
    }

    private static String getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue().trim();
            case NUMERIC: return String.format("%.0f", cell.getNumericCellValue());
            default: return "";
        }
    }
}
