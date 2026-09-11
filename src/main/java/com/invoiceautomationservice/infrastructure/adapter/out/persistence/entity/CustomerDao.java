package com.invoiceautomationservice.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(name = "Customer")
@Table(name = "customers")
public class CustomerDao {

  @Id
  private String id;

  private String fullName;
  private String companyName;
  private String documentType;
  private String documentNumber;
  private String phoneNumber;
  private Boolean active;

}
