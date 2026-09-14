package com.invoiceautomationservice.commons.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Utility to generate and verify BCrypt password hashes.
 * Use BCryptUtil.hash("raw") to get a hash and BCryptUtil.matches(...) to verify.
 */
public final class BCryptUtil {

  private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder();

  private BCryptUtil() {}

  public static String hash(String rawPassword) {
    if (rawPassword == null) {
      throw new IllegalArgumentException("rawPassword must not be null");
    }
    return ENCODER.encode(rawPassword);
  }

  public static boolean matches(String rawPassword, String encodedPassword) {
    if (rawPassword == null || encodedPassword == null) {
      return false;
    }
    return ENCODER.matches(rawPassword, encodedPassword);
  }

  /**
   * Quick CLI: prints the bcrypt hash for the first arg.
   * Run from IDE or `mvn -q -Dexec.mainClass=com.invoiceautomationservice.commons.util.BCryptUtil -Dexec.args="mypassword" exec:java`
   */
  public static void main(String[] args) {
    if (args == null || args.length == 0) {
      System.err.println("Usage: BCryptUtil <password>");
      System.exit(1);
    }
    System.out.println(hash(args[0]));
  }
}
