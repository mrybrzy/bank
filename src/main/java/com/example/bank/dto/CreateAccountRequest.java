package com.example.bank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateAccountRequest {

    @NotBlank
    @Size(min = 3, max = 10)
    @Pattern(regexp = "^[a-zA-Z0-9_]+$")
    private String username;
}