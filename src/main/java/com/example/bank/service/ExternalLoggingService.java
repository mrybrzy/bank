package com.example.bank.service;

import com.example.bank.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class ExternalLoggingService {
    private final RestClient restClient;

    public void logDebit() {
        try {
            restClient.get()
                    .uri("https://tools-httpstatus.pickup-services.com/200")
                    .retrieve()
                    .toBodilessEntity();

        } catch (Exception e) {
            System.out.println("External logging failed: " + e.getMessage());
            throw new ApplicationException(
                    "External logging failed",
                    HttpStatus.BAD_GATEWAY
            );
        }
    }
}
