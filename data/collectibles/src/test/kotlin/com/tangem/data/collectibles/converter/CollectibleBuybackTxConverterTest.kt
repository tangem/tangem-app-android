package com.tangem.data.collectibles.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.collectibles.models.CollectibleBuybackTx
import com.tangem.grow.datasource.collectibles.models.response.GenerateBuybackDataResponse
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class CollectibleBuybackTxConverterTest {

    @Test
    fun `GIVEN buyback data WHEN convert THEN the amount is scaled by the token decimals`() {
        // Arrange
        val response = GenerateBuybackDataResponse(
            buybackId = "buyback-id",
            txData = GenerateBuybackDataResponse.TxDataDTO(
                memo = "cc-memo",
                toDecimals = 6,
                toAmount = "42500000",
                tx = "AQID",
            ),
        )

        // Act
        val actual = CollectibleBuybackTxConverter.convert(response)

        // Assert
        assertThat(actual).isEqualTo(
            CollectibleBuybackTx(
                buybackId = "buyback-id",
                memo = "cc-memo",
                amount = BigDecimal("42.500000"),
                transaction = "AQID",
            ),
        )
    }
}