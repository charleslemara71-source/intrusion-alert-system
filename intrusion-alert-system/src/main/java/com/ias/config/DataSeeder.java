package com.ias.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import com.ias.model.AppUser;
import com.ias.model.Role;
import com.ias.repository.AppUserRepository;

@Component
public class DataSeeder implements CommandLineRunner {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Runs once every time the app starts
    @Override
    public void run(String... args) {
        if (userRepository.findByUsername("admin").isEmpty()) {
            AppUser admin = new AppUser();
            admin.setFullName("System Administrator");
            admin.setUsername("admin");
            admin.setEmail("admin@example.com");
            admin.setPasswordHash(passwordEncoder.encode("Admin@123"));  // change this later
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
            System.out.println(">>> Default admin created: admin / Admin@123");
        }
    }
}