package com.accioneselbosque.auth.service;

import com.accioneselbosque.auth.model.Investor;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${app.verification.base-url}")
    private String verificationBaseUrl;

    public void sendVerificationEmail(Investor investor, String tokenValue) {
        String verificationUrl = verificationBaseUrl + "/auth/verify?token=" + tokenValue;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setTo(investor.getEmail());
            helper.setSubject("Verifica tu cuenta en Acciones El Bosque");

            String html = loadEmailTemplate()
                    .replace("${fullName}", investor.getFullName())
                    .replace("${verificationUrl}", verificationUrl);

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Verification email sent to {}", investor.getEmail());
        } catch (MessagingException e) {
            log.error("Failed to send verification email to {}", investor.getEmail(), e);
        }
    }

    public void sendOtp(String email, String otpCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setTo(email);
            helper.setSubject("Tu código de verificación - Acciones El Bosque");
            helper.setText("<p>Tu código de verificación es: <strong>" + otpCode + "</strong></p><p>Válido por 5 minutos.</p>", true);
            mailSender.send(message);
            log.info("OTP sent to {}", email);
        } catch (MessagingException e) {
            // IMP-1: Mail failure does not rollback transaction. OTP session is persisted
            // even if email delivery fails; this is intentional for user experience.
            log.error("Failed to send OTP to {}", email, e);
        }
    }

    public void sendPasswordResetEmail(String email, String tokenValue) {
        String resetUrl = verificationBaseUrl + "/auth/reset-password?token=" + tokenValue;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setTo(email);
            helper.setSubject("Restablecimiento de contraseña - Acciones El Bosque");
            helper.setText("<p>Se ha iniciado un restablecimiento de contraseña para tu cuenta.</p>" +
                    "<p><a href=\"" + resetUrl + "\">Restablecer contraseña</a></p>" +
                    "<p>Este enlace es válido por 24 horas. Si no solicitaste este cambio, ignora este correo.</p>",
                    true);
            mailSender.send(message);
            log.info("Password reset email sent to {}", email);
        } catch (MessagingException e) {
            log.error("Failed to send password reset email to {}", email, e);
        }
    }

    private String loadEmailTemplate() {
        try (var stream = getClass().getResourceAsStream("/templates/email/verification-email.html")) {
            if (stream == null) return buildFallbackEmail();
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return buildFallbackEmail();
        }
    }

    private String buildFallbackEmail() {
        return "<p>Hola ${fullName},</p><p><a href=\"${verificationUrl}\">Verificar cuenta</a></p>";
    }
}
