package com.invoiceautomationservice.application.service;

import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.COMPANY_CONTEXT_REQUIRED;
import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.COMPANY_NOT_FOUND;

import com.invoiceautomationservice.application.port.out.CurrentUserProvider;
import com.invoiceautomationservice.application.port.out.UserCompanyRepository;
import com.invoiceautomationservice.infrastructure.config.exception.ApplicationException;
import java.util.List;
import com.invoiceautomationservice.domain.model.CompanyPermission;
import static com.invoiceautomationservice.infrastructure.config.exception.RuntimeErrors.COMPANY_PERMISSION_DENIED;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompanyAccessService {

  private final CurrentUserProvider currentUserProvider;
  private final UserCompanyRepository userCompanyRepository;

  public void associateCurrentUser(String companyId) {
    userCompanyRepository.associate(currentUserProvider.username(), companyId);
  }

  public void requireAccess(String companyId) {
    if (!userCompanyRepository.hasAccess(currentUserProvider.username(), companyId)) {
      // Return the same response as a missing company to avoid leaking another tenant's identifiers.
      throw new ApplicationException(COMPANY_NOT_FOUND, companyId);
    }
  }

  public void requirePermission(String companyId, CompanyPermission permission) {
    var role = userCompanyRepository.findRole(currentUserProvider.username(), companyId)
        .orElseThrow(() -> new ApplicationException(COMPANY_NOT_FOUND, companyId));
    if (!role.grants(permission)) {
      throw new ApplicationException(COMPANY_PERMISSION_DENIED, permission, companyId);
    }
  }

  public com.invoiceautomationservice.domain.model.CompanyRole currentRole(String companyId) {
    return userCompanyRepository.findRole(currentUserProvider.username(), companyId)
        .orElseThrow(() -> new ApplicationException(COMPANY_NOT_FOUND, companyId));
  }

  public List<String> currentCompanyIds() {
    return userCompanyRepository.findCompanyIds(currentUserProvider.username());
  }

  public String resolveCompanyId(String requestedCompanyId) {
    if (requestedCompanyId != null && !requestedCompanyId.isBlank()) {
      requireAccess(requestedCompanyId);
      return requestedCompanyId;
    }
    List<String> companyIds = currentCompanyIds();
    if (companyIds.size() == 1) {
      return companyIds.getFirst();
    }
    throw new ApplicationException(COMPANY_CONTEXT_REQUIRED);
  }
}
