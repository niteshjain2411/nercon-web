package org.example.service;

import jakarta.mail.internet.MimeMessage;
import org.example.model.RegistrationData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends a registration-approval email to registration.getEmail().
     */
    @Async
    public void sendApprovalEmail(RegistrationData registration) {
        String delegateId = registration.getDelegateId();
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(registration.getEmail());
            helper.setSubject("NERCON 2026 — Registration Approved! | Delegate ID: " + delegateId);
            helper.setText(buildEmailBody(registration), true);

            mailSender.send(message);
            System.out.println("Approval email sent successfully to " + registration.getEmail() + " for " + delegateId);
        } catch (Exception e) {
            System.err.println("Approval email FAILED for " + delegateId + " (" + registration.getEmail() + "): " + e.getMessage());
        }
    }

    private String buildEmailBody(RegistrationData r) {
        // Build workshops list HTML
        java.util.List<String> workshops = r.getWorkshops();
        boolean hasWorkshops = workshops != null && !workshops.isEmpty()
                && !(workshops.size() == 1 && "ws0".equalsIgnoreCase(workshops.get(0)));
        StringBuilder workshopsHtml = new StringBuilder();
        if (hasWorkshops) {
            for (String w : workshops) {
                workshopsHtml.append("        <li style=\"margin-bottom:0.3rem;\">").append(escHtml(w)).append("</li>\n");
            }
        }

        // Accompanying persons
        long accompany = r.getAccompanycount();
        String accompanyText = accompany > 0 ? String.valueOf(accompany) : "None";

        String workshopsSection = hasWorkshops
                ? """
                  <li style="margin-bottom:0.5rem;">
                    <strong>Pre-Conference Workshops (12 November 2026):</strong>
                    <ul style="margin:0.4rem 0 0 1.2rem; padding:0; list-style-type:disc;">
                """ + workshopsHtml + """
                    </ul>
                  </li>
                """
                : """
                  <li style="margin-bottom:0.5rem;">
                    <strong>Pre-Conference Workshops:</strong> None
                  </li>
                """;

        return """
                <html>
                <body style="font-family: Arial, sans-serif; color: #1f2937; max-width: 620px; margin: 0 auto; padding: 0;">
                  <div style="background: #1e3a8a; padding: 1.5rem 2rem;">
                    <h1 style="color: white; margin: 0; font-size: 1.4rem;">NERCON 2026</h1>
                    <p style="color: #bfdbfe; margin: 0.25rem 0 0; font-size: 0.9rem;">35th Annual Conference of NERCON</p>
                  </div>
                  <div style="padding: 2rem;">
                    <p style="font-size: 1rem; font-weight: 700; color: #1e3a8a; margin-top: 0;">Greetings from NERCON 2026!</p>
                    <p>Dear <strong>%s</strong>,</p>
                    <p>Thank you for registering for the conference. We are delighted to confirm your participation,
                       and the details of your registration are as follows:</p>

                    <ol style="line-height: 1.9; padding-left: 1.4rem; margin: 1.2rem 0;">
                      <li style="margin-bottom:0.5rem;"><strong>Conference:</strong> NERCON 2026 (13–14 November 2026)</li>
                """ + workshopsSection + """
                      <li style="margin-bottom:0.5rem;"><strong>Accompanying Persons:</strong> %s</li>
                    </ol>

                    <p>Your Delegate ID is <strong style="color: #1e3a8a; font-size: 1rem; letter-spacing: 0.04em;">%s</strong>.</p>

                    <p>We sincerely appreciate your interest and look forward to welcoming you. We are confident that
                       the conference will provide a valuable and enriching experience.</p>

                    <p style="margin-bottom: 0;">Warm regards,<br>
                    <strong>Registration Committee</strong><br>
                    NERCON 2026</p>
                  </div>
                  <div style="background: #f3f4f6; padding: 1rem 2rem; font-size: 0.78rem; color: #9ca3af; text-align: center;">
                    This is an automated message. Please do not reply to this email.
                  </div>
                </body>
                </html>
                """.formatted(r.getFullname(), accompanyText, r.getDelegateId());
    }

    private static String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
