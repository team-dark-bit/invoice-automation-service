package com.invoiceautomationservice.application.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CustomerResponse {

  private String id;
  private String fullName;
  private String companyName;
  private String documentType;
  private String documentNumber;
  private String phoneNumber;
  private Boolean active;

}

