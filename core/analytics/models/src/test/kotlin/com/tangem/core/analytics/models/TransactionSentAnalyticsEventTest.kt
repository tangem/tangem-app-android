package com.tangem.core.analytics.models

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TransactionSentAnalyticsEventTest {

    @ParameterizedTest
    @MethodSource("provideFeeAssetTypes")
    fun `GIVEN a swap fee asset type WHEN Transaction sent built THEN Fee Asset Type carries its value`(
        model: FeeAssetTypeModel,
    ) {
        // Act
        val event = Basic.TransactionSent(
            sentFrom = AnalyticsParam.TxSentFrom.Swap(
                blockchain = "Solana",
                token = "USDC",
                feeType = AnalyticsParam.FeeType.Normal,
                feeToken = "SOL",
                feeAssetType = model.feeAssetType,
            ),
            memoType = Basic.TransactionSent.MemoType.Null,
            isCustomToken = false,
            isCustomFeeToken = false,
        )

        // Assert
        assertThat(event.id).isEqualTo("[Basic] Transaction sent")
        assertThat(event.params).isEqualTo(
            mapOf(
                "Source" to "Swap",
                "Blockchain" to "Solana",
                "Token" to "USDC",
                "Fee Type" to "Normal",
                "Fee Token" to "SOL",
                "Fee Asset Type" to model.expected,
                "Memo" to "Null",
            ),
        )
    }

    data class FeeAssetTypeModel(val feeAssetType: AnalyticsParam.FeeAssetType, val expected: String)

    private fun provideFeeAssetTypes() = listOf(
        FeeAssetTypeModel(feeAssetType = AnalyticsParam.FeeAssetType.Coin, expected = "Coin"),
        FeeAssetTypeModel(feeAssetType = AnalyticsParam.FeeAssetType.Token, expected = "Token"),
        FeeAssetTypeModel(feeAssetType = AnalyticsParam.FeeAssetType.Sponsored, expected = "Sponsored"),
    )
}