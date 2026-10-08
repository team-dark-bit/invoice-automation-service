package com.invoiceautomationservice.domain.model;

public record UserAccount(
    String id,
    String fullName,
    String username,
    String email,
    boolean enabled,
    String defaultCompanyId
) {}
