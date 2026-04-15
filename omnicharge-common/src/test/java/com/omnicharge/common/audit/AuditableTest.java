package com.omnicharge.common.audit;

import jakarta.persistence.Column;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Auditable abstract class.
 * Tests field annotations and getter/setter functionality.
 */
class AuditableTest {

    @Test
    void auditableFields_shouldHaveCorrectAnnotations() throws NoSuchFieldException {
        // Test createdDate field
        Field createdDateField = Auditable.class.getDeclaredField("createdDate");
        assertThat(createdDateField.isAnnotationPresent(CreatedDate.class)).isTrue();
        assertThat(createdDateField.isAnnotationPresent(Column.class)).isTrue();
        
        Column createdDateColumn = createdDateField.getAnnotation(Column.class);
        assertThat(createdDateColumn.name()).isEqualTo("created_date");
        assertThat(createdDateColumn.nullable()).isFalse();
        assertThat(createdDateColumn.updatable()).isFalse();

        // Test lastModifiedDate field
        Field lastModifiedDateField = Auditable.class.getDeclaredField("lastModifiedDate");
        assertThat(lastModifiedDateField.isAnnotationPresent(LastModifiedDate.class)).isTrue();
        assertThat(lastModifiedDateField.isAnnotationPresent(Column.class)).isTrue();
        
        Column lastModifiedDateColumn = lastModifiedDateField.getAnnotation(Column.class);
        assertThat(lastModifiedDateColumn.name()).isEqualTo("last_modified_date");

        // Test createdBy field
        Field createdByField = Auditable.class.getDeclaredField("createdBy");
        assertThat(createdByField.isAnnotationPresent(CreatedBy.class)).isTrue();
        assertThat(createdByField.isAnnotationPresent(Column.class)).isTrue();
        
        Column createdByColumn = createdByField.getAnnotation(Column.class);
        assertThat(createdByColumn.name()).isEqualTo("created_by");
        assertThat(createdByColumn.updatable()).isFalse();

        // Test lastModifiedBy field
        Field lastModifiedByField = Auditable.class.getDeclaredField("lastModifiedBy");
        assertThat(lastModifiedByField.isAnnotationPresent(LastModifiedBy.class)).isTrue();
        assertThat(lastModifiedByField.isAnnotationPresent(Column.class)).isTrue();
        
        Column lastModifiedByColumn = lastModifiedByField.getAnnotation(Column.class);
        assertThat(lastModifiedByColumn.name()).isEqualTo("last_modified_by");
    }

    @Test
    void auditableEntity_shouldSetAndGetCreatedDate() {
        // Arrange
        TestAuditableEntity entity = new TestAuditableEntity();
        LocalDateTime now = LocalDateTime.now();

        // Act
        entity.setCreatedDate(now);

        // Assert
        assertThat(entity.getCreatedDate()).isEqualTo(now);
    }

    @Test
    void auditableEntity_shouldSetAndGetLastModifiedDate() {
        // Arrange
        TestAuditableEntity entity = new TestAuditableEntity();
        LocalDateTime now = LocalDateTime.now();

        // Act
        entity.setLastModifiedDate(now);

        // Assert
        assertThat(entity.getLastModifiedDate()).isEqualTo(now);
    }

    @Test
    void auditableEntity_shouldSetAndGetCreatedBy() {
        // Arrange
        TestAuditableEntity entity = new TestAuditableEntity();

        // Act
        entity.setCreatedBy("admin");

        // Assert
        assertThat(entity.getCreatedBy()).isEqualTo("admin");
    }

    @Test
    void auditableEntity_shouldSetAndGetLastModifiedBy() {
        // Arrange
        TestAuditableEntity entity = new TestAuditableEntity();

        // Act
        entity.setLastModifiedBy("user123");

        // Assert
        assertThat(entity.getLastModifiedBy()).isEqualTo("user123");
    }

    @Test
    void auditableEntity_shouldHandleNullValues() {
        // Arrange
        TestAuditableEntity entity = new TestAuditableEntity();

        // Act
        entity.setCreatedDate(null);
        entity.setLastModifiedDate(null);
        entity.setCreatedBy(null);
        entity.setLastModifiedBy(null);

        // Assert
        assertThat(entity.getCreatedDate()).isNull();
        assertThat(entity.getLastModifiedDate()).isNull();
        assertThat(entity.getCreatedBy()).isNull();
        assertThat(entity.getLastModifiedBy()).isNull();
    }

    // Test entity extending Auditable
    static class TestAuditableEntity extends Auditable {
        // Concrete implementation for testing
    }
}
