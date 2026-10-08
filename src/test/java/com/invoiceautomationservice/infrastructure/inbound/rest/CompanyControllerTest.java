package com.invoiceautomationservice.infrastructure.inbound.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.invoiceautomationservice.application.dto.request.CreateCompanyRequest;
import com.invoiceautomationservice.application.dto.response.CompanyResponse;
import com.invoiceautomationservice.application.dto.response.OnboardingStatusResponse;
import com.invoiceautomationservice.application.port.in.CompanyUseCase;
import com.invoiceautomationservice.application.port.in.IssuerOnboardingUseCase;
import com.invoiceautomationservice.application.service.DocumentSeriesService;
import com.invoiceautomationservice.application.service.OnboardingStatusService;
import com.invoiceautomationservice.infrastructure.adapter.in.web.CompanyController;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CompanyController.class)
class CompanyControllerTest {

  @Autowired
  MockMvc mockMvc;

  @MockitoBean
  CompanyUseCase useCase;

  @MockitoBean
  IssuerOnboardingUseCase issuerOnboardingUseCase;

  @MockitoBean
  DocumentSeriesService documentSeriesService;

  @MockitoBean
  OnboardingStatusService onboardingStatusService;

  @Test
  void createsCompany() throws Exception {
    when(useCase.create(any(CreateCompanyRequest.class))).thenReturn(company());

    mockMvc.perform(post("/api/v1/companies")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "legalName": "Dark Bit SAC",
                              "tradeName": "Dark Bit",
                              "taxId": "20123456789",
                              "address": "Lima"
                            }
                            """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.statusCode").value(201))
            .andExpect(jsonPath("$.data.id").value("company-1"))
            .andExpect(jsonPath("$.data.legalName").value("Dark Bit SAC"))
            .andExpect(jsonPath("$.data.taxId").value("20123456789"))
            .andExpect(jsonPath("$.data.active").value(true));
  }

  @Test
  void rejectsCompanyWithoutRequiredFields() throws Exception {
    mockMvc.perform(post("/api/v1/companies")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.statusCode").value(400))
            .andExpect(jsonPath("$.message").value("A problem occurred"));
  }

  @Test
  void findsCompanyById() throws Exception {
    when(useCase.findById("company-1")).thenReturn(company());

    mockMvc.perform(get("/api/v1/companies/company-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value("company-1"));
  }

  @Test
  void listsCompanies() throws Exception {
    when(useCase.findAll()).thenReturn(List.of(company()));

    mockMvc.perform(get("/api/v1/companies"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].id").value("company-1"));
  }

  @Test
  void updatesCompanySettings() throws Exception {
    CompanyResponse updated = company();
    updated.setTradeName("Updated");
    when(useCase.update(org.mockito.ArgumentMatchers.eq("company-1"), any()))
        .thenReturn(updated);

    mockMvc.perform(patch("/api/v1/companies/company-1")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"tradeName":"Updated"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.tradeName").value("Updated"));
  }

  @Test
  void returnsConsolidatedOnboardingStatus() throws Exception {
    when(onboardingStatusService.get("company-1")).thenReturn(new OnboardingStatusResponse(
        "company-1", true, true, false, true, true, List.of()));

    mockMvc.perform(get("/api/v1/companies/company-1/onboarding-status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.completed").value(true))
        .andExpect(jsonPath("$.data.salesReceiptSeriesConfigured").value(true));
  }

  private CompanyResponse company() {
    CompanyResponse response = new CompanyResponse();
    response.setId("company-1");
    response.setLegalName("Dark Bit SAC");
    response.setTradeName("Dark Bit");
    response.setTaxId("20123456789");
    response.setAddress("Lima");
    response.setActive(true);
    return response;
  }
}
