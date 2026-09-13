package com.invoiceautomationservice.infrastructure.inbound.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.invoiceautomationservice.application.dto.request.CreateCompanyRequest;
import com.invoiceautomationservice.application.dto.response.CompanyResponse;
import com.invoiceautomationservice.application.port.in.CompanyUseCase;
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
