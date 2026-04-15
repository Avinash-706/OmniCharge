package com.omnicharge.operator.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlanCategoryTest {

    @Test
    void testEnumValues() {
        PlanCategory[] categories = PlanCategory.values();
        
        assertThat(categories).hasSize(4);
        assertThat(categories).contains(
                PlanCategory.RECOMMENDED,
                PlanCategory.DATA,
                PlanCategory.UNLIMITED,
                PlanCategory.TALKTIME
        );
    }

    @Test
    void testValueOf() {
        assertThat(PlanCategory.valueOf("RECOMMENDED")).isEqualTo(PlanCategory.RECOMMENDED);
        assertThat(PlanCategory.valueOf("DATA")).isEqualTo(PlanCategory.DATA);
        assertThat(PlanCategory.valueOf("UNLIMITED")).isEqualTo(PlanCategory.UNLIMITED);
        assertThat(PlanCategory.valueOf("TALKTIME")).isEqualTo(PlanCategory.TALKTIME);
    }

    @Test
    void testEnumName() {
        assertThat(PlanCategory.RECOMMENDED.name()).isEqualTo("RECOMMENDED");
        assertThat(PlanCategory.DATA.name()).isEqualTo("DATA");
        assertThat(PlanCategory.UNLIMITED.name()).isEqualTo("UNLIMITED");
        assertThat(PlanCategory.TALKTIME.name()).isEqualTo("TALKTIME");
    }

    @Test
    void testEnumOrdinal() {
        assertThat(PlanCategory.RECOMMENDED.ordinal()).isEqualTo(0);
        assertThat(PlanCategory.DATA.ordinal()).isEqualTo(1);
        assertThat(PlanCategory.UNLIMITED.ordinal()).isEqualTo(2);
        assertThat(PlanCategory.TALKTIME.ordinal()).isEqualTo(3);
    }

    @Test
    void testEnumEquality() {
        PlanCategory category1 = PlanCategory.RECOMMENDED;
        PlanCategory category2 = PlanCategory.RECOMMENDED;
        PlanCategory category3 = PlanCategory.DATA;

        assertThat(category1).isEqualTo(category2);
        assertThat(category1).isNotEqualTo(category3);
    }

    @Test
    void testEnumToString() {
        assertThat(PlanCategory.RECOMMENDED.toString()).isEqualTo("RECOMMENDED");
        assertThat(PlanCategory.DATA.toString()).isEqualTo("DATA");
        assertThat(PlanCategory.UNLIMITED.toString()).isEqualTo("UNLIMITED");
        assertThat(PlanCategory.TALKTIME.toString()).isEqualTo("TALKTIME");
    }
}
