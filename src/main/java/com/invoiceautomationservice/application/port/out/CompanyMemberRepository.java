package com.invoiceautomationservice.application.port.out;

import com.invoiceautomationservice.domain.model.CompanyMember;
import com.invoiceautomationservice.domain.model.CompanyRole;
import java.time.Instant;
import java.util.List;

public interface CompanyMemberRepository {
  boolean userExists(String username, String email);
  CompanyMember create(String companyId, String fullName, String username, String email,
      String passwordHash, CompanyRole role, Instant joinedAt);
  CompanyMember associateExisting(
      String companyId, String username, CompanyRole role, Instant joinedAt);
  List<CompanyMember> findAll(String companyId);
  CompanyMember findByUserId(String companyId, String userId);
  CompanyMember update(String companyId, String userId, CompanyRole role, Boolean enabled);
  void remove(String companyId, String userId);
  long countOwners(String companyId);
}
