package com.tangem.features.send.feeselector.model.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeExtraInfo
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeItem
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeNonce
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import com.tangem.features.send.commonFee
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FeeSelectorNonceChangeTransformerTest {

    private val fee = commonFee()

    private fun content(feeNonce: FeeNonce) = FeeSelectorUM.Content(
        isPrimaryButtonEnabled = true,
        fees = TransactionFee.Single(normal = fee),
        feeItems = persistentListOf(FeeItem.Market(fee)),
        selectedFeeItem = FeeItem.Market(fee),
        feeExtraInfo = mockk<FeeExtraInfo>(),
        feeFiatRateUM = null,
        feeNonce = feeNonce,
    )

    private fun nonceState(nonce: BigInteger?) = content(FeeNonce.Nonce(nonce = nonce, onNonceChange = {}))

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Update {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN nonce field WHEN transform THEN nonce updated`(model: UpdateModel) {
            // Arrange
            val state = nonceState(nonce = BigInteger.ONE)

            // Act
            val result = FeeSelectorNonceChangeTransformer(model.value).transform(state) as FeeSelectorUM.Content

            // Assert
            assertThat((result.feeNonce as FeeNonce.Nonce).nonce).isEqualTo(model.expectedNonce)
        }

        private fun provideTestModels() = listOf(
            UpdateModel(value = "42", expectedNonce = BigInteger.valueOf(42)), // valid number
            UpdateModel(value = "", expectedNonce = null), // empty -> cleared
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Unchanged {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN non-applicable input WHEN transform THEN state returned unchanged`(model: UnchangedModel) {
            // Act
            val result = FeeSelectorNonceChangeTransformer(model.value).transform(model.state)

            // Assert
            assertThat(result).isSameInstanceAs(model.state)
        }

        private fun provideTestModels() = listOf(
            UnchangedModel(value = "abc", state = nonceState(nonce = BigInteger.ONE)), // non-numeric
            UnchangedModel(value = "42", state = content(FeeNonce.None)), // no editable nonce
            UnchangedModel(value = "42", state = FeeSelectorUM.Loading), // not a content state
        )
    }

    data class UpdateModel(val value: String, val expectedNonce: BigInteger?)
    data class UnchangedModel(val value: String, val state: FeeSelectorUM)
}