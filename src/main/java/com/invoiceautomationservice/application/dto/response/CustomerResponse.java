package com.invoiceautomationservice.application.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CustomerResponse {

  private String id;
  private String companyId;
  private String fullName;
  private String companyName;
  private String documentType;
  private String documentNumber;
  private String phoneNumber;
  private String address;
  private String email;
  private Boolean active;

}

