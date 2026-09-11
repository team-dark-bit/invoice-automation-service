package com.invoiceautomationservice.commons.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

  private Integer statusCode;
  private String message;
  private T data;
  private List<String> errors;

  public static <T> ApiResponse<T> success(Integer statusCode, String message, T data) {
    ApiResponse<T> response = new ApiResponse<>();
    response.setStatusCode(statusCode);
    response.setMessage(message);
    response.setData(data);
    return response;
  }

  public static <T> ApiResponse<T> failure(Integer statusCode, String message, List<String> errors) {
    ApiResponse<T> response = new ApiResponse<>();
    response.setStatusCode(statusCode);
    response.setMessage(message);
    response.setErrors(errors);
    return response;
  }

}