package com.omnicharge.common.dto;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PagedResponse DTO.
 */
class PagedResponseTest {

    @Test
    void constructor_shouldCreatePagedResponseWithAllFields() {
        // Arrange
        List<String> content = Arrays.asList("item1", "item2", "item3");

        // Act
        PagedResponse<String> response = new PagedResponse<>(content, 0, 10, 25, 3);

        // Assert
        assertThat(response.getContent()).hasSize(3);
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.getTotalElements()).isEqualTo(25);
        assertThat(response.getTotalPages()).isEqualTo(3);
        assertThat(response.isLast()).isFalse();
    }

    @Test
    void constructor_shouldCalculateLastPageCorrectly() {
        // Arrange
        List<String> content = Arrays.asList("item1", "item2");

        // Act
        PagedResponse<String> response = new PagedResponse<>(content, 2, 10, 25, 3);

        // Assert
        assertThat(response.isLast()).isTrue(); // page 2 is the last page (0-indexed)
    }

    @Test
    void constructor_shouldHandleFirstPage() {
        // Arrange
        List<String> content = Arrays.asList("item1", "item2");

        // Act
        PagedResponse<String> response = new PagedResponse<>(content, 0, 10, 25, 3);

        // Assert
        assertThat(response.isLast()).isFalse();
    }

    @Test
    void constructor_shouldHandleSinglePage() {
        // Arrange
        List<String> content = Arrays.asList("item1", "item2");

        // Act
        PagedResponse<String> response = new PagedResponse<>(content, 0, 10, 2, 1);

        // Assert
        assertThat(response.isLast()).isTrue();
    }

    @Test
    void setters_shouldUpdateFields() {
        // Arrange
        PagedResponse<String> response = new PagedResponse<>();
        List<String> content = Arrays.asList("item1");

        // Act
        response.setContent(content);
        response.setPage(1);
        response.setSize(20);
        response.setTotalElements(100);
        response.setTotalPages(5);
        response.setLast(false);

        // Assert
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getSize()).isEqualTo(20);
        assertThat(response.getTotalElements()).isEqualTo(100);
        assertThat(response.getTotalPages()).isEqualTo(5);
        assertThat(response.isLast()).isFalse();
    }
}
