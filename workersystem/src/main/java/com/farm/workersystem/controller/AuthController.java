package com.farm.workersystem.controller;

import com.farm.workersystem.dto.LoginRequest;
import com.farm.workersystem.dto.RegisterRequest;
import com.farm.workersystem.dto.WorkerResponse;
import com.farm.workersystem.model.Worker;
import com.farm.workersystem.service.WorkerService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final WorkerService service;

    public AuthController(WorkerService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest req) {
        service.register(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Registration successful!"));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginRequest req,
                                                     HttpSession session) {
        try {
            Worker w = service.login(req.email().trim(), req.password());
            session.setAttribute("workerId", w.getId());
            session.setAttribute("role", w.getRole().name());
            return ResponseEntity.ok(Map.of(
                    "message", "Login successful!",
                    "role", w.getRole().name(),
                    "name", w.getFullName()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public Map<String, String> logout(HttpSession session) {
        session.invalidate();
        return Map.of("message", "Logged out.");
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpSession session) {
        Object id = session.getAttribute("workerId");
        if (id == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Not logged in."));
        return ResponseEntity.ok(WorkerResponse.from(service.findById((Long) id)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleInvalid(MethodArgumentNotValidException e) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", "Please fill in all fields correctly (password: 6+ characters)."));
    }
}