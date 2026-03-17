package com.fintech.system.auth.Model;



import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fintech.system.auth.Enum.AuthProvider;
import com.fintech.system.auth.Enum.Role;
import com.fintech.system.auth.Enum.UserStatus;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "users")
public class User {
    @Id
    private String id;

    @Indexed(unique = true)
    private String email;
    @JsonProperty("fName")
    private String fName;

    @JsonProperty("lName")
    private String lName;

    private String fullName;

    @JsonIgnore
    private String password;

    private UserStatus userStatus;

    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @Builder.Default
    private boolean emailVerified = false;

    @JsonIgnore
    @Builder.Default
    private boolean softDelete = false;

    private AuthProvider oauthProvider;      // "google", "github", null for local
    private String oauthProviderId;    // provider's user ID

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public String getFullName() {
        return fName + " " + lName;
    }
}
