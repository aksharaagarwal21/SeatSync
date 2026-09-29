package com.seatsync.service.verification;

import com.seatsync.config.OtpProperties;
import com.seatsync.entity.OtpPurpose;
import com.seatsync.entity.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class EmailOtpNotifier implements OtpNotifier {

    private final JavaMailSender mailSender;
    private final OtpProperties properties;

    public EmailOtpNotifier(JavaMailSender mailSender, OtpProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public void send(User user, OtpPurpose purpose, String code, String summary, Duration validFor) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(properties.from());
            helper.setTo(user.getEmail());
            helper.setSubject(code + " is your SeatSync code");
            helper.setText(plainText(code, summary, validFor), html(user, code, summary, validFor));
            mailSender.send(message);
        } catch (MessagingException ex) {
            throw new MailSendException("Could not build verification email", ex);
        }
    }

    private static String plainText(String code, String summary, Duration validFor) {
        return """
                Your SeatSync verification code is %s

                You are approving: %s

                The code expires in %d minutes. If you didn't request it, ignore this email and \
                consider changing your password.
                """.formatted(code, summary, validFor.toMinutes());
    }

    private static String html(User user, String code, String summary, Duration validFor) {
        String spacedCode = String.join(" ", code.split(""));
        return """
                <div style="background:#fafafa;padding:32px 16px;font-family:Inter,Segoe UI,Arial,sans-serif;color:#18181b">
                  <div style="max-width:440px;margin:0 auto;background:#fff;border:1px solid #e4e4e7;border-radius:14px;padding:28px">
                    <p style="margin:0 0 20px;font-weight:600;font-size:15px">Seat<span style="color:#15803d">Sync</span></p>
                    <p style="margin:0 0 6px;font-size:14px">Hi %s,</p>
                    <p style="margin:0 0 20px;font-size:14px;color:#52525b">Use this code to approve: <strong style="color:#18181b">%s</strong></p>
                    <p style="margin:0 0 20px;font-size:32px;font-weight:700;letter-spacing:6px;text-align:center;background:#f4f4f5;border-radius:10px;padding:14px 0">%s</p>
                    <p style="margin:0;font-size:13px;color:#71717a">It expires in %d minutes and can be used once. If you didn't request it, ignore this email and consider changing your password.</p>
                  </div>
                </div>
                """.formatted(HtmlUtils.htmlEscape(user.getName()), HtmlUtils.htmlEscape(summary), spacedCode, validFor.toMinutes());
    }
}
