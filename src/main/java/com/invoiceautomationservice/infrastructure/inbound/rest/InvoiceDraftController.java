package com.invoiceautomationservice.infrastructure.inbound.rest;
import com.invoiceautomationservice.application.port.in.CreateInvoiceDraftUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/invoice-drafts")
public class InvoiceDraftController {
    private final CreateInvoiceDraftUseCase useCase;
    public InvoiceDraftController(CreateInvoiceDraftUseCase useCase) { this.useCase = useCase; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public InvoiceDraftResponse create() { return InvoiceDraftResponse.from(useCase.create()); }
}
