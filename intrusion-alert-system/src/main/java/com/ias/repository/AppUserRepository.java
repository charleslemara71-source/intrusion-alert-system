package com.ias.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.ias.model.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);
}