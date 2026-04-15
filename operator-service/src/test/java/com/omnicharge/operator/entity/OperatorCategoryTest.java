package com.omnicharge.operator.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OperatorCategoryTest {

    @Test
    void testEnumValues() {
        OperatorCategory[] categories = OperatorCategory.values();
        
        assertThat(categories).hasSize(6);
        assertThat(categories).contains(
                OperatorCategory.PREPAID,
                OperatorCategory.POSTPAID,
                OperatorCategory.DTH,
                OperatorCategory.ELECTRICITY,
                OperatorCategory.GAS,
                OperatorCategory.WATER
        );
    }

    @Test
    void testValueOf() {
        assertThat(OperatorCategory.valueOf("PREPAID")).isEqualTo(OperatorCategory.PREPAID);
        assertThat(OperatorCategory.valueOf("POSTPAID")).isEqualTo(OperatorCategory.POSTPAID);
        assertThat(OperatorCategory.valueOf("DTH")).isEqualTo(OperatorCategory.DTH);
        assertThat(OperatorCategory.valueOf("ELECTRICITY")).isEqualTo(OperatorCategory.ELECTRICITY);
        assertThat(OperatorCategory.valueOf("GAS")).isEqualTo(OperatorCategory.GAS);
        assertThat(OperatorCategory.valueOf("WATER")).isEqualTo(OperatorCategory.WATER);
    }

    @Test
    void testEnumName() {
        assertThat(OperatorCategory.PREPAID.name()).isEqualTo("PREPAID");
        assertThat(OperatorCategory.POSTPAID.name()).isEqualTo("POSTPAID");
        assertThat(OperatorCategory.DTH.name()).isEqualTo("DTH");
        assertThat(OperatorCategory.ELECTRICITY.name()).isEqualTo("ELECTRICITY");
        assertThat(OperatorCategory.GAS.name()).isEqualTo("GAS");
        assertThat(OperatorCategory.WATER.name()).isEqualTo("WATER");
    }

    @Test
    void testEnumOrdinal() {
        assertThat(OperatorCategory.PREPAID.ordinal()).isEqualTo(0);
        assertThat(OperatorCategory.POSTPAID.ordinal()).isEqualTo(1);
        assertThat(OperatorCategory.DTH.ordinal()).isEqualTo(2);
        assertThat(OperatorCategory.ELECTRICITY.ordinal()).isEqualTo(3);
        assertThat(OperatorCategory.GAS.ordinal()).isEqualTo(4);
        assertThat(OperatorCategory.WATER.ordinal()).isEqualTo(5);
    }

    @Test
    void testEnumEquality() {
        OperatorCategory category1 = OperatorCategory.PREPAID;
        OperatorCategory category2 = OperatorCategory.PREPAID;
        OperatorCategory category3 = OperatorCategory.POSTPAID;

        assertThat(category1).isEqualTo(category2);
        assertThat(category1).isNotEqualTo(category3);
    }

    @Test
    void testEnumToString() {
        assertThat(OperatorCategory.PREPAID.toString()).isEqualTo("PREPAID");
        assertThat(OperatorCategory.POSTPAID.toString()).isEqualTo("POSTPAID");
        assertThat(OperatorCategory.DTH.toString()).isEqualTo("DTH");
        assertThat(OperatorCategory.ELECTRICITY.toString()).isEqualTo("ELECTRICITY");
        assertThat(OperatorCategory.GAS.toString()).isEqualTo("GAS");
        assertThat(OperatorCategory.WATER.toString()).isEqualTo("WATER");
    }
}
