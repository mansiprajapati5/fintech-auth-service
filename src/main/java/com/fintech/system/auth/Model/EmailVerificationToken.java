package com.fintech.system.auth.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "email_verification_tokens")
public class EmailVerificationToken {
    @Id
    private String id;
    private String token;
    private String userId;
    private LocalDateTime expiresAt;
    private boolean used;
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}