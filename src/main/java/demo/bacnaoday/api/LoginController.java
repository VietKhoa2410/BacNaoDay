package demo.bacnaoday.api;

import demo.bacnaoday.api.payload.LoginRequest;
import demo.bacnaoday.api.payload.LoginResponse;
import demo.bacnaoday.security.AppUserDetails;
import demo.bacnaoday.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class LoginController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest body) {
        if (body == null
                || !StringUtils.hasText(body.username())
                || !StringUtils.hasText(body.password())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid request"));
        }
        try {
            UsernamePasswordAuthenticationToken token =
                    new UsernamePasswordAuthenticationToken(body.username().trim(), body.password());
            Authentication authentication = authenticationManager.authenticate(token);
            AppUserDetails user = (AppUserDetails) authentication.getPrincipal();
            String accessToken = jwtService.generateToken(user);
            return ResponseEntity.ok(new LoginResponse(user.getUsername(), accessToken));
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, String>> me(Authentication authentication) {
        return ResponseEntity.ok(Map.of("username", authentication.getName()));
    }
}
