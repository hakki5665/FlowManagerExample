package com.example.flowmanager.messaging;

import java.util.UUID;

public record ToConvertEvent(
        UUID requestId,
        String filePath
) {}