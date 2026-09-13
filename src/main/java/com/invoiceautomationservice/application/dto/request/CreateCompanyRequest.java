package com.invoiceautomationservice.application.dto.request;

import com.invoiceautomationservice.commons.util.validation.annotation.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCompanyRequest {

  @NotBlank(name = "legalName")
  private String legalName;

  private String tradeName;

  @NotBlank(name = "taxId")
  private String taxId;

  private String address;

  private Boolean active = true;
}
