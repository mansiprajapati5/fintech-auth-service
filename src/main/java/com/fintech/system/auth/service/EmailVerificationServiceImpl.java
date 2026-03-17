package com.fintech.system.auth.service;



import com.fintech.system.auth.Model.EmailVerificationToken;
import com.fintech.system.auth.Model.User;
import com.fintech.system.auth.Repository.EmailVerificationTokenRepository;
import com.fintech.system.auth.Repository.UserRepository;
import com.fintech.system.common.exception.InvalidTokenException;
import com.fintech.system.common.utills.TokenGenerator;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.fintech.system.common.constant.ExceptionConstant.EXPIRED_TOKEN_EXCEPTION;
import static com.fintech.system.common.constant.ExceptionConstant.INVALID_TOKEN_EXCEPTION;


@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService{

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;

    private final JavaMailSender mailSender;
    private final TokenGenerator tokenGenerator;
    private final SpringTemplateEngine templateEngine;
    @Value("${app.base-url}")
    private String baseUrl;



    @Override
    @Async
    public void sendVerificationEmail(User user) throws MessagingException {
        try {
            // 1. generate token
            String tokenValue = UUID.randomUUID().toString();

            // 2. save token to DB
            EmailVerificationToken token = EmailVerificationToken.builder()
                    .token(tokenValue)
                    .userId(user.getId())
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .build();
            tokenRepository.save(token);

            // 3. send email
            sendHtmlVerificationEmail(user, tokenValue);

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    @Override
    @Transactional
    public void verifyEmail(String tokenValue) {
        EmailVerificationToken token = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new InvalidTokenException(INVALID_TOKEN_EXCEPTION));

        if (token.isExpired()) {
            tokenRepository.delete(token);
            throw new InvalidTokenException(EXPIRED_TOKEN_EXCEPTION);
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setEmailVerified(true);
        userRepository.save(user);
        tokenRepository.delete(token);
    }

    private void sendHtmlVerificationEmail(User user, String token) throws MessagingException {

        Context context = new Context();
        context.setVariable("name", user.getFName());
        context.setVariable("verificationUrl", baseUrl + "/api/auth/users/verify-email?token=" + token);

        String htmlContent = templateEngine.process("email-verification", context);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(user.getEmail());
        helper.setSubject("Verify your email address");
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }
}
