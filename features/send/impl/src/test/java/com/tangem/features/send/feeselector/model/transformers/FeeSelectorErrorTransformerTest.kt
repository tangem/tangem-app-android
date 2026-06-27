package com.tangem.features.send.feeselector.model.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeExtraInfo
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeItem
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeNonce
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import com.tangem.features.send.commonFee
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FeeSelectorErrorTransformerTest {

    private val fee = commonFee()

    private fun content() = FeeSelectorUM.Content(
        isPrimaryButtonEnabled = true,
        fees = TransactionFee.Single(normal = fee),
        feeItems = persistentListOf(FeeItem.Market(fee)),
        selectedFeeItem = FeeItem.Market(fee),
        feeExtraInfo = FeeExtraInfo(
            isFeeApproximate = false,
            isFeeConvertibleToFiat = false,
            isTronToken = false,
            feeCryptoCurrencyStatus = mockk(),
        ),
        feeFiatRateUM = null,
        feeNonce = FeeNonce.None,
    )

    @Test
    fun `GIVEN content state and not-enough-funds error WHEN transform THEN stays content with flag and disabled button`() {
        // Act
        val result = FeeSelectorErrorTransformer(GetFeeError.GaslessError.NotEnoughFunds)
            .transform(content()) as FeeSelectorUM.Content

        // Assert
        assertThat(result.isPrimaryButtonEnabled).isFalse()
        assertThat(result.feeExtraInfo.isNotEnoughFunds).isTrue()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN other state or error WHEN transform THEN transitions to error`(model: ErrorModel) {
        // Act
        val result = FeeSelectorErrorTransformer(model.error).transform(model.state)

        // Assert
        assertThat(result).isEqualTo(FeeSelectorUM.Error(error = model.error))
    }

    private fun provideTestModels() = listOf(
        // content but a different error -> the special branch needs NotEnoughFunds specifically
        ErrorModel(state = content(), error = GetFeeError.UnknownError),
        // not-enough-funds but not a content state -> the special branch needs a Content state
        ErrorModel(state = FeeSelectorUM.Loading, error = GetFeeError.GaslessError.NotEnoughFunds),
    )

    data class ErrorModel(val state: FeeSelectorUM, val error: GetFeeError)
}