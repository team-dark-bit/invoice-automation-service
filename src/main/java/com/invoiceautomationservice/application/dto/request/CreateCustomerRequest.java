package com.invoiceautomationservice.application.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCustomerRequest {

  private String fullName;

  private String companyName;

  private String documentType;

  private String documentNumber;

  private String phoneNumber;

  private Boolean active = true;

}

