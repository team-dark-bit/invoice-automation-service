package com.invoiceautomationservice.commons.util.validation;

import com.invoiceautomationservice.infrastructure.config.exception.ApplicationException;
import com.invoiceautomationservice.commons.util.validation.annotation.IsDecimal;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;
import static com.invoiceautomationservice.infrastructure.config.exception.ValidationErrors.DECIMAL_BAD_REQUEST;

public class IsDecimalValidator implements ConstraintValidator<IsDecimal, String> {

  private int numbers;
  private int decimals;

  @Override
  public void initialize(IsDecimal constraintAnnotation) {
    this.numbers = constraintAnnotation.numbers();
    this.decimals = constraintAnnotation.decimals();
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {

    if (StringUtils.isBlank(value)) {
      return true;//en caso no se haya validado anteriormente
    }

    String regex = "^\\d{1," + numbers + "}\\.\\d{1," + decimals + "}$";
    if (!value.matches(regex)) {
      throw new ApplicationException(DECIMAL_BAD_REQUEST);
    }
    return true;
  }
}