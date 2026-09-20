package com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository;

import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.IssuerTaxProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaIssuerTaxProfileRepository
    extends JpaRepository<IssuerTaxProfileEntity, String> {
}
