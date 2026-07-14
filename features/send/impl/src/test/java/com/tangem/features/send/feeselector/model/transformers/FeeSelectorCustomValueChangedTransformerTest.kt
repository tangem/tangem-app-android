package com.tangem.features.send.feeselector.model.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeItem
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeNonce
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import com.tangem.features.send.loadedStatus
import com.tangem.features.send.subcomponents.fee.model.converters.custom.kaspa.KaspaCustomFeeConverter
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import kotlinx.collections.immutable.toImmutableList
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FeeSelectorCustomValueChangedTransformerTest {

    private val currencyFactory = MockCryptoCurrencyFactory()

    private val feeStatus = loadedStatus(
        currency = currencyFactory.createCoin(Blockchain.Kaspa),
        fiatRate = BigDecimal("0.1"),
    )

    private val kaspaFee = Fee.Kaspa(
        amount = Amount(currencySymbol = "KAS", value = BigDecimal("0.0001"), decimals = 8),
        mass = BigInteger.valueOf(2000),
        feeRate = BigInteger.valueOf(5),
    )

    private val customItem = FeeItem.Custom(
        fee = kaspaFee,
        customValues = KaspaCustomFeeConverter(
            onCustomFeeValueChange = { _, _ -> },
            appCurrency = AppCurrency.Default,
            feeCryptoCurrencyStatus = feeStatus,
        ).convert(kaspaFee),
    )

    private fun transformer(index: Int, value: String) = FeeSelectorCustomValueChangedTransformer(
        index = index,
        value = value,
        intents = mockk(relaxed = true),
        appCurrency = AppCurrency.Default,
        feeCryptoCurrencyStatus = feeStatus,
    )

    private fun content(feeItems: List<FeeItem>, selected: FeeItem) = FeeSelectorUM.Content(
        isPrimaryButtonEnabled = true,
        fees = TransactionFee.Single(normal = kaspaFee),
        feeItems = feeItems.toImmutableList(),
        selectedFeeItem = selected,
        feeExtraInfo = mockk(),
        feeFiatRateUM = null,
        feeNonce = FeeNonce.None,
    )

    @Test
    fun `GIVEN custom fee and non-zero value WHEN transform THEN custom updated selected and button enabled`() {
        // Arrange
        val state = content(feeItems = listOf(customItem), selected = customItem)

        // Act  (index 0 = amount field of the Kaspa custom fee)
        val result = transformer(index = 0, value = "0.0002").transform(state) as FeeSelectorUM.Content

        // Assert
        assertThat(result.isPrimaryButtonEnabled).isTrue()
        assertThat(result.selectedFeeItem).isInstanceOf(FeeItem.Custom::class.java)
        val updatedCustom = result.feeItems.filterIsInstance<FeeItem.Custom>().first()
        assertThat(updatedCustom.customValues.first().value).isEqualTo("0.0002")
        assertThat(result.selectedFeeItem).isEqualTo(updatedCustom)
    }

    @Test
    fun `GIVEN custom fee edited to zero WHEN transform THEN button disabled`() {
        // Arrange
        val state = content(feeItems = listOf(customItem), selected = customItem)

        // Act
        val result = transformer(index = 0, value = "0").transform(state) as FeeSelectorUM.Content

        // Assert
        assertThat(result.isPrimaryButtonEnabled).isFalse()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN non-applicable state WHEN transform THEN returned unchanged`(model: UnchangedModel) {
        // Act
        val result = transformer(index = 0, value = "0.0002").transform(model.state)

        // Assert
        assertThat(result).isSameInstanceAs(model.state)
    }

    private fun provideTestModels() = listOf(
        UnchangedModel(state = FeeSelectorUM.Loading), // not a content state
        UnchangedModel(state = content(feeItems = listOf(FeeItem.Market(kaspaFee)), selected = FeeItem.Market(kaspaFee))),
    )

    data class UnchangedModel(val state: FeeSelectorUM)
}