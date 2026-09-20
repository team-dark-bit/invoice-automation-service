package com.invoiceautomationservice.infrastructure.adapter.out.persistence;

import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.ISSUER_TAX_PROFILE_NOT_FOUND;

import com.invoiceautomationservice.application.port.out.IssuerTaxProfileRepository;
import com.invoiceautomationservice.domain.model.IssuerTaxProfile;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity.IssuerTaxProfileEntity;
import com.invoiceautomationservice.infrastructure.adapter.out.persistence.repository.JpaIssuerTaxProfileRepository;
import com.invoiceautomationservice.infrastructure.config.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class IssuerTaxProfilePersistenceAdapter implements IssuerTaxProfileRepository {

  private final JpaIssuerTaxProfileRepository repository;

  @Override
  public IssuerTaxProfile save(IssuerTaxProfile profile) {
    return toDomain(repository.save(toEntity(profile)));
  }

  @Override
  public IssuerTaxProfile findByCompanyId(String companyId) {
    return repository.findById(companyId)
        .map(this::toDomain)
        .orElseThrow(() -> new ApplicationException(ISSUER_TAX_PROFILE_NOT_FOUND, companyId));
  }

  @Override
  public boolean existsByCompanyId(String companyId) {
    return repository.existsById(companyId);
  }

  private IssuerTaxProfileEntity toEntity(IssuerTaxProfile profile) {
    IssuerTaxProfileEntity entity = new IssuerTaxProfileEntity();
    entity.setCompanyId(profile.companyId());
    entity.setTaxpayerType(profile.taxpayerType());
    entity.setFiscalAddress(profile.fiscalAddress());
    entity.setUbigeo(profile.ubigeo());
    entity.setDepartment(profile.department());
    entity.setProvince(profile.province());
    entity.setDistrict(profile.district());
    entity.setCountryCode(profile.countryCode());
    entity.setCreatedAt(profile.createdAt());
    entity.setUpdatedAt(profile.updatedAt());
    return entity;
  }

  private IssuerTaxProfile toDomain(IssuerTaxProfileEntity entity) {
    return new IssuerTaxProfile(
        entity.getCompanyId(), entity.getTaxpayerType(), entity.getFiscalAddress(),
        entity.getUbigeo(), entity.getDepartment(), entity.getProvince(), entity.getDistrict(),
        entity.getCountryCode(), entity.getCreatedAt(), entity.getUpdatedAt());
  }
}
