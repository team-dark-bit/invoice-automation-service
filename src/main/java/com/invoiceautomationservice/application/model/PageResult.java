package com.invoiceautomationservice.application.model;

import com.invoiceautomationservice.application.dto.response.PageResponse;
import java.util.List;
import java.util.function.Function;

public record PageResult<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
  public PageResult {
    content = List.copyOf(content);
  }

  public <R> PageResponse<R> map(Function<T, R> mapper) {
    return new PageResponse<>(content.stream().map(mapper).toList(), page, size,
        totalElements, totalPages, page == 0, totalPages == 0 || page + 1 >= totalPages);
  }
}
