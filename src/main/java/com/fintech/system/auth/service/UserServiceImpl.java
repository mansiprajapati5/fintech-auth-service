package com.fintech.system.auth.service;



import com.fintech.system.auth.Enum.AuthProvider;
import com.fintech.system.auth.Enum.Role;
import com.fintech.system.auth.Model.User;
import com.fintech.system.auth.Repository.UserRepository;
import com.fintech.system.common.JWT.JwtService;
import com.fintech.system.common.exception.AlreadyExistsException;
import com.fintech.system.common.exception.NotFoundException;
import com.fintech.system.auth.Decorator.AuthResponse;
import com.fintech.system.auth.Decorator.LoginRequest;
import com.fintech.system.auth.Decorator.RegisterRequest;
import com.fintech.system.auth.Decorator.UserUpdateRequest;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.stereotype.Service;


import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService{

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final JwtService jwtService;

    @Override
    public AuthResponse createUser(RegisterRequest request) throws MessagingException {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AlreadyExistsException(request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fName(request.getFName())
                .lName(request.getLName())
                .fullName(request.getFName() + " " + request.getLName())
                .roles(Set.of(Role.ROLE_USER))
                .emailVerified(false)
                .softDelete(false)
                .oauthProvider(AuthProvider.LOCAL)
                .build();

        User saved = userRepository.save(user);

        // async - won't block
        emailVerificationService.sendVerificationEmail(saved);

        // manually build response with JWT
        return AuthResponse.builder()
                .accessToken(jwtService.generateToken(saved))
                .refreshToken(jwtService.generateRefreshToken(saved))
                .tokenType("Bearer")
                .userId(saved.getId())
                .email(saved.getEmail())
                .fName(saved.getFName())
                .lName(saved.getLName())
                .roles(saved.getRoles().stream()
                        .map(Role::name)
                        .collect(Collectors.toSet()))
                .build();
    }
    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new NotFoundException(request.getEmail()));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        if (!user.isEmailVerified()) {
            throw new RuntimeException("Please verify your email first");
        }

        if (user.isSoftDelete()) {
            throw new RuntimeException("Account is deactivated");
        }


        return AuthResponse.builder()
                .accessToken(jwtService.generateToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fName(user.getFName())
                .lName(user.getLName())
                .roles(user.getRoles().stream()
                        .map(Role::name)
                        .collect(Collectors.toSet()))
                .build();
    }

    @Override
    public User getUserById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(id));
        return user;
    }

    public Page<User> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return userRepository.findBySoftDeleteFalse(pageable)
                .map(user -> modelMapper.map(user, User.class));
    }

    @Override
    public AuthResponse updateUser(String id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(id));

        modelMapper.map(request, user);
        User updatedUser = userRepository.save(user);
        return modelMapper.map(updatedUser, AuthResponse.class);
    }

    @Override
    public void deleteUser(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(id));
        user.setSoftDelete(true);
        userRepository.save(user);
    }

    // Called by OAuth2 flow (Google/GitHub)
    public AuthResponse processOAuthUser(OAuth2UserRequest oAuth2UserRequest, String email,
                                         String name, AuthProvider provider) {
        return userRepository.findByEmail(email)
                .map(existingUser -> {
                    existingUser.setFullName(name);
                    return modelMapper.map(userRepository.save(existingUser), AuthResponse.class);
                })
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(email)
                            .fullName(name)
                            .roles(Set.of(Role.ROLE_USER))
                            .emailVerified(true)  // OAuth emails are pre-verified
                            .softDelete(false)
                            .oauthProvider(provider)
                            .password("")  // no password for OAuth users
                            .build();
                    return modelMapper.map(userRepository.save(newUser), AuthResponse.class);
                });
    }
}