package com.invoiceautomationservice.application.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class PageResultTest {

  @Test
  void mapsContentAndPaginationMetadata() {
    var response = new PageResult<>(List.of(1, 2), 1, 2, 5, 3)
        .map(String::valueOf);

    assertThat(response.content()).containsExactly("1", "2");
    assertThat(response.page()).isEqualTo(1);
    assertThat(response.totalElements()).isEqualTo(5);
    assertThat(response.first()).isFalse();
    assertThat(response.last()).isFalse();
  }

  @Test
  void rejectsUnsafePageSizes() {
    assertThatThrownBy(() -> new PageQuery(-1, 20))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new PageQuery(0, 101))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
