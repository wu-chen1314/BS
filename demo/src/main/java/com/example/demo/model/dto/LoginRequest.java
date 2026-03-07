package com.example.demo.model.dto;

import lombok.Data;

@Data
public class LoginRequest {

    private String username;

    // Preferred plain password field.
    private String password;

    // Backward-compatible field for older clients.
    private String passwordHash;
}