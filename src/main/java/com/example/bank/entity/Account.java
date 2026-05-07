package com.example.bank.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String accountNumber;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 10, message = "Username must be 3-10 characters")
    @Pattern(
            regexp = "^[a-zA-Z0-9_]+$",
            message = "Username can only contain letters, numbers, and underscores"
    )
    @Column(unique = true, nullable = false)
    private String username;
}
