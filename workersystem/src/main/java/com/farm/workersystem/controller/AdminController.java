package com.farm.workersystem.controller;

import com.farm.workersystem.dto.UpdateWorkerRequest;
import com.farm.workersystem.dto.WorkerResponse;
import com.farm.workersystem.service.WorkerService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/workers")
public class AdminController {

    private final WorkerService service;

    public AdminController(WorkerService service) {
        this.service = service;
    }

    private void requireAdmin(HttpSession session) {
        Object role = session.getAttribute("role");
        if (role == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please log in.");
        if (!"ADMIN".equals(role))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admins only.");
    }

    @GetMapping
    public List<WorkerResponse> all(HttpSession session) {
        requireAdmin(session);
        return service.findAll().stream().map(WorkerResponse::from).toList();
    }

    @PutMapping("/{id}")
    public WorkerResponse update(@PathVariable Long id,
                                 @Valid @RequestBody UpdateWorkerRequest req,
                                 HttpSession session) {
        requireAdmin(session);
        return WorkerResponse.from(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable Long id, HttpSession session) {
        requireAdmin(session);
        if (id.equals(session.getAttribute("workerId")))
            throw new IllegalArgumentException("You cannot delete your own account.");
        service.delete(id);
        return Map.of("message", "Worker deleted.");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleStatus(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", e.getReason()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleInvalid(MethodArgumentNotValidException e) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", "Please check the fields. Name, ID, email and role are required."));
    }
}