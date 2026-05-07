package massfilemailer;

public class Recipient {
    private String email;
    private String nama;
    private String attachment;
    private String pdfPassword;

    public Recipient(String email, String nama, String attachment, String pdfPassword) {
        this.email = email;
        this.nama = nama;
        this.attachment = attachment;
        this.pdfPassword = pdfPassword;
    }

    // Getters
    public String getEmail() { return email; }
    public String getNama() { return nama; }
    public String getAttachment() { return attachment; }
    public String getPdfPassword() { return pdfPassword; }

    @Override
    public String toString() {
        return "Recipient{" +
                "email='" + email + '\'' +
                ", nama='" + nama + '\'' +
                ", attachment='" + attachment + '\'' +
                ", pdfPassword='" + pdfPassword + '\'' +
                '}';
    }
}
