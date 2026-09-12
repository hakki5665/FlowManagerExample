package com.example.flowmanager.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record ConvertedEvent(
        @JsonProperty("requestId") UUID requestId,
        @JsonProperty("status") String status,
        @JsonProperty("outputPath") String convertedFilePath,
        @JsonProperty("errorMessage") String errorMessage
) {}