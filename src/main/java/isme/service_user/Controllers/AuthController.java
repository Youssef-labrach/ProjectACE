package isme.service_user.Controllers;

import isme.service_user.JWT.JwtService;
import isme.service_user.Models.AuthRequest;
import isme.service_user.Models.User;
import isme.service_user.Repositories.UserRepo;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepo userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    public AuthController(UserRepo userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }
    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@RequestBody User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/signin")
    public ResponseEntity<String> signIn(@RequestBody AuthRequest authRequest) {
        User user = userRepository.findByEmail(authRequest.getEmail());
        if (!passwordEncoder.matches(authRequest.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid password");
        }
        String token = jwtService.generateToken(
                new org.springframework.security.core.userdetails.User(
                        user.getEmail(),
                        user.getPassword(),
                        List.of()
                )
        );
        return ResponseEntity.ok(token);
    }
}