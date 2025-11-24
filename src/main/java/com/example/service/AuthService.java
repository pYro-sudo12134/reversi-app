package com.example.service;

import com.example.entity.UserDTO;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import java.util.List;

@ApplicationScoped
public class AuthService {
    private final Logger logger = LoggerFactory.getLogger(AuthService.class);
    @Inject
    UserDTOService userService;

    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String issuer;

    @ConfigProperty(name = "quarkus.oauth2.token.expiration")
    long expiration;

    public Uni<String> login(String username, String password) {
        logger.info("Attempting login for user: {}", username);
        return userService.findByUsername(username)
                .onItem().transformToUni(users -> {
                    logger.info("Found users: {}", users.size());
                    if (users.isEmpty()) {
                        logger.error("No user found with username: {}", username);
                        return Uni.createFrom().failure(new SecurityException("Invalid credentials"));
                    }
                    UserDTO user = users.get(0);
                    logger.info("Comparing passwords - input: {}, stored: {}", password, user.getPassword());
                    if (!user.getPassword().equals(password)) {
                        logger.error("Password mismatch for user: {}", username);
                        return Uni.createFrom().failure(new SecurityException("Invalid credentials"));
                    }
                    return generateToken(user);
                });
    }


    public Uni<UserDTO> registerUser(String username, String password) {
        UserDTO newUser = new UserDTO();
        newUser.setUsername(username);
        newUser.setPassword(password);
        newUser.setWins(0L);
        newUser.setLosses(0L);
        newUser.setRole("USER");
        return userService.insert(List.of(newUser))
                .onItem().transform(ignore -> newUser);
    }

    public Uni<UserDTO> registerAdmin(String username, String password) {
        UserDTO newUser = new UserDTO();
        newUser.setUsername(username);
        newUser.setPassword(password);
        newUser.setWins(0L);
        newUser.setLosses(0L);
        newUser.setRole("ADMIN");

        return userService.insert(List.of(newUser))
                .onItem().transform(ignore -> newUser);
    }
    private Uni<String> generateToken(UserDTO user) {
        Set<String> roles = new HashSet<>();
        roles.add(user.getRole());

        return Uni.createFrom().item(Jwt.issuer(issuer)
                .upn(user.getUsername())
                .groups(roles)
                .claim(Claims.sub, user.getId().toString())
                .expiresIn(Duration.ofSeconds(expiration))
                .sign());
    }
}