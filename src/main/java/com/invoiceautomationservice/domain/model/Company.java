package com.invoiceautomationservice.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Company {

  private String id;
  private String legalName;
  private String tradeName;
  private String taxId;
  private String address;
  private boolean active;
}
