package com.tangem.datasource.api.promotion

import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.api.promotion.models.PromotionWalletType
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class PromotionWalletTypeTest {

    @ParameterizedTest
    @CsvSource("CARD, card", "MOBILE, mobile")
    fun `GIVEN wallet type WHEN used as query value THEN serialized to api value`(
        type: PromotionWalletType,
        expected: String,
    ) {
        // Act
        val actual = type.toString()

        // Assert
        assertThat(actual).isEqualTo(expected)
    }
}