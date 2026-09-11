package com.invoiceautomationservice.commons.util;

import java.util.UUID;

public class IdGenerator {

    private IdGenerator() {
        // Prevent instantiation
    }

    public static String generateId() {
        return UUID.randomUUID().toString();
    }
}