package com.farm.workersystem.service;

import com.farm.workersystem.dto.RegisterRequest;
import com.farm.workersystem.dto.UpdateWorkerRequest;
import com.farm.workersystem.model.Role;
import com.farm.workersystem.model.Worker;
import com.farm.workersystem.repository.WorkerRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WorkerService {

    private final WorkerRepository repo;
    private final PasswordEncoder encoder;

    public WorkerService(WorkerRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    public Worker register(RegisterRequest req) {
        if (req.role() == Role.ADMIN)
            throw new IllegalArgumentException("Admin accounts cannot be created through registration.");
        if (repo.existsByEmail(req.email()))
            throw new IllegalArgumentException("That email is already registered.");
        if (repo.existsByIdNumber(req.idNumber()))
            throw new IllegalArgumentException("That ID number is already registered.");

        Worker w = new Worker();
        w.setFullName(req.name());
        w.setIdNumber(req.idNumber());
        w.setEmail(req.email());
        w.setPasswordHash(encoder.encode(req.password()));
        w.setRole(req.role());
        w.setSection(req.section());
        return repo.save(w);
    }

    public Worker login(String email, String password) {
        Worker w = repo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));
        if (!encoder.matches(password, w.getPasswordHash()))
            throw new IllegalArgumentException("Invalid email or password.");
        return w;
    }

    public List<Worker> findAll() {
        return repo.findAll();
    }

    public Worker findById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Worker not found."));
    }

    public Worker update(Long id, UpdateWorkerRequest req) {
        Worker w = findById(id);

        if (!w.getEmail().equalsIgnoreCase(req.email()) && repo.existsByEmail(req.email()))
            throw new IllegalArgumentException("That email is already used by another worker.");
        if (!w.getIdNumber().equals(req.idNumber()) && repo.existsByIdNumber(req.idNumber()))
            throw new IllegalArgumentException("That ID number is already used by another worker.");

        w.setFullName(req.fullName());
        w.setIdNumber(req.idNumber());
        w.setEmail(req.email());
        w.setRole(req.role());
        w.setSection(req.section());
        w.setSalary(req.salary());

        if (req.newPassword() != null && !req.newPassword().isBlank()) {
            if (req.newPassword().length() < 6)
                throw new IllegalArgumentException("New password must be at least 6 characters.");
            w.setPasswordHash(encoder.encode(req.newPassword()));
        }
        return repo.save(w);
    }

    public void delete(Long id) {
        findById(id);
        repo.deleteById(id);
    }
}