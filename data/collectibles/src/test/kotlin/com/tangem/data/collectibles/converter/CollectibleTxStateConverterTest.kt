package com.tangem.data.collectibles.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.collectibles.models.CollectibleTxState
import com.tangem.domain.collectibles.models.CollectibleTxStatus
import com.tangem.grow.datasource.collectibles.models.response.CollectibleTxResponse
import com.tangem.grow.datasource.collectibles.models.response.CollectibleTxResponse.CollectibleTxStatusDTO
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CollectibleTxStateConverterTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN an auto buyback answer WHEN convert THEN the payout is in whole tokens`(model: AutoBuybackModel) {
        // Arrange
        val response = CollectibleTxResponse(
            txId = "tx-id",
            status = CollectibleTxStatusDTO.AUTO_BUYBACKED,
            autoBuyback = model.autoBuyback,
        )

        // Act
        val actual = CollectibleTxStateConverter.convert(response)

        // Assert
        assertThat(actual).isEqualTo(
            CollectibleTxState(
                status = CollectibleTxStatus.AUTO_BUYBACKED,
                pack = null,
                autoBuybackAmount = model.expected,
            ),
        )
    }

    internal data class AutoBuybackModel(val autoBuyback: CollectibleTxResponse.AutoBuybackDTO?, val expected: BigDecimal?)

    private fun provideTestModels() = listOf(
        AutoBuybackModel(autoBuyback = createAutoBuyback(toAmount = "42500000"), expected = BigDecimal("42.500000")),
        AutoBuybackModel(autoBuyback = createAutoBuyback(toAmount = "1", toDecimals = 0), expected = BigDecimal("1")),
        AutoBuybackModel(autoBuyback = createAutoBuyback(toAmount = "TBD"), expected = null),
        AutoBuybackModel(autoBuyback = null, expected = null),
    )

    private fun createAutoBuyback(toAmount: String, toDecimals: Int = 6) = CollectibleTxResponse.AutoBuybackDTO(
        buybackId = "buyback-id",
        nftAddress = "nft",
        fromAddress = "owner",
        toNetwork = "solana",
        toContractAddress = "usdc-mint",
        toDecimals = toDecimals,
        toAmount = toAmount,
        toAddress = "vault",
        updatedAt = "2026-08-25T08:21:07.518119Z",
        createdAt = "2026-08-25T08:21:07.518119Z",
    )
}