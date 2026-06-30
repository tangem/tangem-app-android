package com.tangem.features.send.feeselector.model.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeExtraInfo
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeItem
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeNonce
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import com.tangem.features.send.commonFee
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import kotlinx.collections.immutable.toImmutableList
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FeeSelectorRemoveSuggestedTransformerTest {

    private val fee = commonFee()
    private val market = FeeItem.Market(fee)
    private val fast = FeeItem.Fast(fee)
    private val suggested = FeeItem.Suggested(title = TextReference.EMPTY, fee = fee)

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN suggested present WHEN transform THEN suggested removed and selection resolved`(model: SelectionModel) {
        // Arrange
        val state = content(feeItems = listOf(suggested, market, fast), selected = model.selected)

        // Act
        val result = FeeSelectorRemoveSuggestedTransformer.transform(state) as FeeSelectorUM.Content

        // Assert
        assertThat(result.feeItems).containsExactly(market, fast).inOrder()
        assertThat(result.selectedFeeItem).isEqualTo(model.expectedSelected)
    }

    private fun provideTestModels() = listOf(
        SelectionModel(selected = suggested, expectedSelected = market),
        SelectionModel(selected = fast, expectedSelected = fast),
    )

    @Test
    fun `GIVEN non-content state WHEN transform THEN returned unchanged`() {
        // Act
        val result = FeeSelectorRemoveSuggestedTransformer.transform(FeeSelectorUM.Loading)

        // Assert
        assertThat(result).isEqualTo(FeeSelectorUM.Loading)
    }

    private fun content(feeItems: List<FeeItem>, selected: FeeItem) = FeeSelectorUM.Content(
        isPrimaryButtonEnabled = true,
        fees = TransactionFee.Single(normal = fee),
        feeItems = feeItems.toImmutableList(),
        selectedFeeItem = selected,
        feeExtraInfo = mockk<FeeExtraInfo>(),
        feeFiatRateUM = null,
        feeNonce = FeeNonce.None,
    )

    data class SelectionModel(val selected: FeeItem, val expectedSelected: FeeItem)
}