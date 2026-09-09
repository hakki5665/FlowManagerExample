package com.example.flowmanager.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record ToConvertEvent(
        @JsonProperty("requestId") UUID requestId,
        @JsonProperty("filePath") String filePath
) {}