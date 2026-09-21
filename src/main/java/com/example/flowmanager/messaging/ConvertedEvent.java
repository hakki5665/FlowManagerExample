package com.example.flowmanager.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record ConvertedEvent(
        UUID requestId,
        ConversionStatus status,
        @JsonProperty("outputPath") String convertedFilePath,
        String errorMessage
) {}