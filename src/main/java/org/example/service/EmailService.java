package org.example.service;

import jakarta.mail.internet.MimeMessage;
import org.example.model.RegistrationData;
import org.example.model.Workshop;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final FirestoreService firestoreService;

    @Value("${spring.mail.from}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender, FirestoreService firestoreService) {
        this.mailSender = mailSender;
        this.firestoreService = firestoreService;
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
        // Build a workshop ID -> name lookup map from Firestore
        Map<String, String> workshopNames;
        try {
            List<Workshop> allWorkshops = firestoreService.getAllWorkshops();
            workshopNames = allWorkshops.stream()
                    .collect(Collectors.toMap(Workshop::getId, w -> w.getContent() != null ? w.getContent() : w.getId()));
        } catch (Exception e) {
            workshopNames = Map.of();
        }

        // Build workshops list HTML
        List<String> workshops = r.getWorkshops();
        boolean hasWorkshops = workshops != null && !workshops.isEmpty()
                && !(workshops.size() == 1 && "ws0".equalsIgnoreCase(workshops.get(0)));

        // Accompanying persons
        long accompany = r.getAccompanycount();
        String accompanyText = accompany > 0 ? String.valueOf(accompany) : "None";

        // Build the full HTML using StringBuilder to avoid .formatted() scope issues
        StringBuilder html = new StringBuilder();
        html.append("<html><body style=\"font-family: Arial, sans-serif; color: #1f2937; max-width: 620px; margin: 0 auto; padding: 0;\">");
        html.append("<div style=\"background: #1e3a8a; padding: 1.5rem 2rem;\">");
        html.append("<h1 style=\"color: white; margin: 0; font-size: 1.4rem;\">NERCON 2026</h1>");
        html.append("<p style=\"color: #bfdbfe; margin: 0.25rem 0 0; font-size: 0.9rem;\">35th Annual Conference of NERCON</p>");
        html.append("</div>");
        html.append("<div style=\"padding: 2rem;\">");
        html.append("<p style=\"font-size: 1rem; font-weight: 700; color: #1e3a8a; margin-top: 0;\">Greetings from NERCON 2026!</p>");
        html.append("<p>Dear <strong>").append(escHtml(r.getFullname())).append("</strong>,</p>");
        html.append("<p>Thank you for registering for the conference. We are delighted to confirm your participation, and the details of your registration are as follows:</p>");
        html.append("<ol style=\"line-height: 1.9; padding-left: 1.4rem; margin: 1.2rem 0;\">");
        html.append("<li style=\"margin-bottom:0.5rem;\"><strong>Conference:</strong> NERCON 2026 (13\u201314 November 2026)</li>");

        if (hasWorkshops) {
            html.append("<li style=\"margin-bottom:0.5rem;\"><strong>Pre-Conference Workshops (12 November 2026):</strong>");
            html.append("<ul style=\"margin:0.4rem 0 0 1.2rem; padding:0; list-style-type:disc;\">");
            final Map<String, String> wsNames = workshopNames;
            for (String wsId : workshops) {
                String displayName = wsNames.getOrDefault(wsId, wsId);
                html.append("<li style=\"margin-bottom:0.3rem;\">").append(escHtml(displayName)).append("</li>");
            }
            html.append("</ul></li>");
        } else {
            html.append("<li style=\"margin-bottom:0.5rem;\"><strong>Pre-Conference Workshops:</strong> None</li>");
        }

        html.append("<li style=\"margin-bottom:0.5rem;\"><strong>Accompanying Persons:</strong> ").append(escHtml(accompanyText)).append("</li>");
        html.append("</ol>");
        html.append("<p>Your Delegate ID is <strong style=\"color: #1e3a8a; font-size: 1rem; letter-spacing: 0.04em;\">")
            .append(escHtml(r.getDelegateId())).append("</strong>.</p>");
        html.append("<p>We sincerely appreciate your interest and look forward to welcoming you. We are confident that the conference will provide a valuable and enriching experience.</p>");
        html.append("<p style=\"margin-bottom: 0;\">Warm regards,<br><strong>Registration Committee</strong><br>NERCON 2026</p>");
        html.append("</div>");
        html.append("<div style=\"background: #f3f4f6; padding: 1rem 2rem; font-size: 0.78rem; color: #9ca3af; text-align: center;\">This is an automated message. Please do not reply to this email.</div>");
        html.append("</body></html>");

        return html.toString();
    }

    private static String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
