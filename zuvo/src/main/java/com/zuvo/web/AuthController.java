package com.zuvo.web;

import com.zuvo.entity.User;
import com.zuvo.repo.UserInterestRepository;
import com.zuvo.repo.UserRepository;
import com.zuvo.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    public record LoginRequest(String email, String password) {}
    public record VerifyRequest(String email, String code) {}
    public record EmailRequest(String email) {}

    private final AuthService auth;
    private final UserRepository users;
    private final UserInterestRepository interests;

    public AuthController(AuthService auth, UserRepository users, UserInterestRepository interests) {
        this.auth = auth;
        this.users = users;
        this.interests = interests;
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody AuthService.RegisterRequest r) {
        User u = auth.register(r);
        return J.of("message", "We sent a 5-digit code to your email.", "email", u.email);
    }

    @PostMapping("/verify")
    public Map<String, Object> verify(@RequestBody VerifyRequest r, HttpServletRequest req) {
        User u = auth.verify(r.email(), r.code());
        startSession(req, u);
        return me(u);
    }

    @PostMapping("/resend")
    public Map<String, Object> resend(@RequestBody EmailRequest r) {
        auth.resend(r.email());
        return J.of("message", "A new code is on its way.");
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest r, HttpServletRequest req) {
        User u = auth.login(r.email(), r.password());
        startSession(req, u);
        return me(u);
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s != null) s.invalidate();
        return J.of("ok", true);
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        Long uid = s == null ? null : (Long) s.getAttribute("uid");
        if (uid == null) return ResponseEntity.status(401).body(J.of("error", "Not logged in."));
        return users.findById(uid).<ResponseEntity<?>>map(u -> ResponseEntity.ok(me(u)))
                .orElseGet(() -> ResponseEntity.status(401).body(J.of("error", "Not logged in.")));
    }

    private void startSession(HttpServletRequest req, User u) {
        HttpSession old = req.getSession(false);
        if (old != null) old.invalidate();             // new session id after login (prevents session fixation)
        req.getSession(true).setAttribute("uid", u.id);
    }

    private Map<String, Object> me(User u) {
        return J.of("id", u.id, "fullName", u.fullName, "email", u.email, "gender", u.gender,
                "dateOfBirth", u.dateOfBirth.toString(), "profilePicUrl", u.profilePicUrl, "bio", u.bio,
                "role", u.role, "needsInterests", interests.findByUserId(u.id).isEmpty());
    }
}
