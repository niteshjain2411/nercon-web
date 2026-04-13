package org.example.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import jakarta.mail.internet.MimeMessage;
import org.example.model.RegistrationData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Generates a CODE-128 barcode PNG for the given delegateId.
     */
    public byte[] generateBarcode(String delegateId) throws Exception {
        Code128Writer writer = new Code128Writer();
        BitMatrix matrix = writer.encode(delegateId, BarcodeFormat.CODE_128, 700, 175);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return out.toByteArray();
    }

    /**
     * Generates a barcode for registration.getDelegateId() and sends a
     * registration-approval email to registration.getEmail() with the
     * barcode attached as "food-coupon-barcode.png".
     */
    public void sendApprovalEmail(RegistrationData registration) throws Exception {
        String delegateId = registration.getDelegateId();
        byte[] barcodeBytes = generateBarcode(delegateId);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromAddress);
        helper.setTo(registration.getEmail());
        helper.setSubject("NERCON 2026 — Registration Approved! | Delegate ID: " + delegateId);
        helper.setText(buildEmailBody(registration), true);
        helper.addAttachment("food-coupon-barcode.png", new ByteArrayResource(barcodeBytes));

        mailSender.send(message);
    }

    private String buildEmailBody(RegistrationData r) {
        return """
                <html>
                <body style="font-family: Arial, sans-serif; color: #1f2937; max-width: 620px; margin: 0 auto; padding: 0;">
                  <div style="background: #1e3a8a; padding: 1.5rem 2rem;">
                    <h1 style="color: white; margin: 0; font-size: 1.4rem;">NERCON 2026</h1>
                    <p style="color: #bfdbfe; margin: 0.25rem 0 0; font-size: 0.9rem;">35th Annual Conference of NERCON</p>
                  </div>
                  <div style="padding: 2rem;">
                    <h2 style="color: #059669; margin-top: 0;">Registration Approved ✓</h2>
                    <p style="margin-bottom: 1.4rem;">Dear <strong>%s</strong>,</p>
                    <p>We are delighted to inform you that your registration for <strong>NERCON 2026</strong> has been
                       successfully reviewed and <strong>approved</strong>.</p>

                    <table style="border-collapse: collapse; width: 100%%; margin: 1.5rem 0; background: #f0f9ff;
                                  border-radius: 8px; overflow: hidden;">
                      <tr>
                        <td style="padding: 0.7rem 1rem; color: #6b7280; font-weight: 600; width: 40%%;">Delegate ID</td>
                        <td style="padding: 0.7rem 1rem; font-weight: 700; color: #1e3a8a; font-size: 1rem; letter-spacing: 0.04em;">%s</td>
                      </tr>
                      <tr style="background: #e0f2fe;">
                        <td style="padding: 0.7rem 1rem; color: #6b7280; font-weight: 600;">Name</td>
                        <td style="padding: 0.7rem 1rem;">%s</td>
                      </tr>
                      <tr>
                        <td style="padding: 0.7rem 1rem; color: #6b7280; font-weight: 600;">Email</td>
                        <td style="padding: 0.7rem 1rem;">%s</td>
                      </tr>
                      <tr style="background: #e0f2fe;">
                        <td style="padding: 0.7rem 1rem; color: #6b7280; font-weight: 600;">Total Amount</td>
                        <td style="padding: 0.7rem 1rem;">₹ %s</td>
                      </tr>
                    </table>

                    <div style="background: #fef9c3; border-left: 4px solid #d97706; padding: 1rem 1.2rem;
                                border-radius: 0 6px 6px 0; margin: 1.5rem 0;">
                      <strong style="color: #92400e;">🍽 Food Coupon Barcode Attached</strong>
                      <p style="margin: 0.5rem 0 0; color: #78350f; font-size: 0.9rem;">
                        Your food coupon barcode is attached to this email as
                        <em>food-coupon-barcode.png</em>. Please carry a printout or a digital copy
                        of this barcode to the conference venue. It will be scanned at the meal
                        counters and the banquet dinner to grant you access to catering services.
                      </p>
                    </div>

                    <p style="font-size: 0.9rem;">We look forward to welcoming you at NERCON 2026. Should you have any
                    questions, please reach out to us at
                    <a href="mailto:nercon2026@gmail.com" style="color: #1e3a8a;">nercon2026@gmail.com</a>.</p>

                    <p>Warm regards,<br>
                    <strong>NERCON 2026 Organising Committee</strong></p>
                  </div>
                  <div style="background: #f3f4f6; padding: 1rem 2rem; font-size: 0.78rem; color: #9ca3af; text-align: center;">
                    This is an automated message. Please do not reply to this email.
                  </div>
                </body>
                </html>
                """.formatted(
                        r.getFullname(),
                        r.getDelegateId(),
                        r.getFullname(),
                        r.getEmail(),
                        r.getTotalAmount() != null ? r.getTotalAmount() : "—"
                );
    }
}
