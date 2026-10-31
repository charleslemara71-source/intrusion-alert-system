package com.farm.workersystem.config;

import com.farm.workersystem.model.Role;
import com.farm.workersystem.model.Worker;
import com.farm.workersystem.repository.WorkerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final WorkerRepository repo;
    private final PasswordEncoder encoder;

    public DataInitializer(WorkerRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (repo.existsByRole(Role.ADMIN)) return;

        Worker admin = new Worker();
        admin.setFullName("System Admin");
        admin.setIdNumber("ADMIN001");
        admin.setEmail("admin@farm.com");
        admin.setPasswordHash(encoder.encode("Admin@123"));
        admin.setRole(Role.ADMIN);
        admin.setSection("Management");
        repo.save(admin);

        System.out.println(">>> Default admin created: admin@farm.com / Admin@123");
    }
}