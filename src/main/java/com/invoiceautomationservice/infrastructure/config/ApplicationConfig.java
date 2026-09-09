package com.invoiceautomationservice.infrastructure.config;
import com.invoiceautomationservice.application.port.in.CreateInvoiceDraftUseCase;
import com.invoiceautomationservice.application.port.out.InvoiceDraftRepository;
import com.invoiceautomationservice.application.service.CreateInvoiceDraftService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;
@Configuration
public class ApplicationConfig {
    @Bean Clock clock() { return Clock.systemUTC(); }
    @Bean CreateInvoiceDraftUseCase createInvoiceDraftUseCase(InvoiceDraftRepository repository, Clock clock) {
        return new CreateInvoiceDraftService(repository, clock);
    }
}
