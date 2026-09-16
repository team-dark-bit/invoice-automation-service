package com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity;

import java.io.Serializable;

public record UserCompanyId(String userId, String companyId) implements Serializable {
}
