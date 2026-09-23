package com.invoiceautomationservice.infrastructure.adapter.in.web;

import com.darkbit.security.application.dto.request.LoginRequest;
import com.darkbit.security.application.dto.response.AuthResponse;
import com.darkbit.security.application.service.AuthService;
import com.invoiceautomationservice.commons.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest req) {
    try {
      AuthResponse resp = authService.login(req);
      return ResponseEntity.ok(ApiResponse.success(200, "Login successful", resp));
    } catch (Exception ex) {
      return ResponseEntity.status(401).body(ApiResponse.failure(401, "Invalid credentials", List.of("Invalid credentials")));
    }
  }

  @GetMapping("/me")
  public ResponseEntity<ApiResponse<AuthResponse>> me(@AuthenticationPrincipal UserDetails ud) {
    if (ud == null) {
      return ResponseEntity.status(401).body(ApiResponse.failure(401, "Unauthorized", List.of("Unauthorized")));
    }
    // Build minimal AuthResponse
    AuthResponse resp = new AuthResponse();
    resp.setUsername(ud.getUsername());
    return ResponseEntity.ok(ApiResponse.success(200, "User info", resp));
  }
}
