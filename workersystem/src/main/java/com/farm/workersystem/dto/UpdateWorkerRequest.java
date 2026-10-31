package com.farm.workersystem.dto;

import com.farm.workersystem.model.Role;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record UpdateWorkerRequest(
        @NotBlank String fullName,
        @NotBlank String idNumber,
        @NotBlank @Email String email,
        @NotNull Role role,
        String section,
        @PositiveOrZero BigDecimal salary,
        String newPassword) {}