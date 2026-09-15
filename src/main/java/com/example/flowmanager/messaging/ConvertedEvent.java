package com.example.flowmanager.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record ConvertedEvent(
        UUID requestId,
        String status,
        @JsonProperty("outputPath") String convertedFilePath,
        String errorMessage
) {}