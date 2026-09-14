package com.invoiceautomationservice.infrastructure.adapter.in.security;

import com.darkbit.security.application.port.UserPort;
import com.darkbit.security.application.service.CustomUserDetails;
import com.darkbit.security.domain.model.UserModel;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

  private final UserPort userPort;

  public CustomUserDetailsService(UserPort userPort) {
    this.userPort = userPort;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    UserModel user = userPort.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

    return new CustomUserDetails(
            user.getUsername(),
            user.getEmail(),
            user.getPassword(),
            user.isEnabled(),
            user.getRoleNames(),
            user.getAllPermissionNames()
    );
  }
}