package com.fintech.system.auth.service;


import com.fintech.system.auth.Model.User;
import jakarta.mail.MessagingException;

public interface EmailVerificationService {

    public void sendVerificationEmail(User user) throws MessagingException;

    public void verifyEmail(String tokenValue);

}
