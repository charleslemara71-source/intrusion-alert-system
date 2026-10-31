package com.zuvo.service;

import com.zuvo.web.ApiException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String from;
    private final boolean devLogCodes;

    public EmailService(JavaMailSender mailSender,
                        @Value("${spring.mail.username:}") String from,
                        @Value("${zuvo.dev-log-codes:false}") boolean devLogCodes) {
        this.mailSender = mailSender;
        this.from = from;
        this.devLogCodes = devLogCodes;
    }

    public void sendCode(String to, String name, String code) {
        if (devLogCodes) log.info("DEV ONLY - verification code for {} is {}", to, code);
        try {
            if (from == null || from.isBlank() || from.startsWith("PUT_")) {
                throw new IllegalStateException("Mail username is not configured");
            }
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
            h.setFrom(from, "Zuvo");
            h.setTo(to);
            h.setSubject("Your Zuvo verification code: " + code);
            h.setText("Hi " + name + ", your Zuvo code is " + code + ". It expires in 10 minutes.", html(name, code));
            mailSender.send(msg);
        } catch (Exception e) {
            log.warn("Could not send email to {}: {}", to, e.getMessage());
            if (!devLogCodes) throw new ApiException(502, "We couldn't send the email. Check the mail settings and try again.");
        }
    }

    private String html(String name, String code) {
        StringBuilder digits = new StringBuilder();
        for (char c : code.toCharArray()) {
            digits.append("<span style=\"display:inline-block;width:44px;height:54px;line-height:54px;margin:0 4px;")
                  .append("border-radius:12px;background:#EEEDFE;color:#3B2FD9;font-size:28px;font-weight:700;\">")
                  .append(c).append("</span>");
        }
        return "<div style=\"font-family:Arial,sans-serif;max-width:480px;margin:auto;padding:28px;border-radius:16px;"
             + "border:1px solid #e5e7f5;text-align:center;\">"
             + "<div style=\"font-size:26px;font-weight:700;color:#3B2FD9;\">zuv<span style=\"color:#FF7A3D;\">o</span></div>"
             + "<p style=\"color:#444;font-size:16px;\">Hi " + escape(name) + ", enter this code to verify your email:</p>"
             + "<div style=\"margin:22px 0;\">" + digits + "</div>"
             + "<p style=\"color:#777;font-size:13px;\">The code expires in 10 minutes. If you didn't create a Zuvo account, ignore this email.</p>"
             + "</div>";
    }

    private String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
