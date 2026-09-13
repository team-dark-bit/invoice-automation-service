package com.invoiceautomationservice.application.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CompanyResponse {

  private String id;
  private String legalName;
  private String tradeName;
  private String taxId;
  private String address;
  private Boolean active;
}
