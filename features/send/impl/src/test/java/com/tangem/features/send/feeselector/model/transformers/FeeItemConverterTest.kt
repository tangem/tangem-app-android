package com.tangem.features.send.feeselector.model.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeItem
import com.tangem.features.send.api.subcomponents.feeSelector.params.FeeSelectorParams
import com.tangem.features.send.commonFee
import com.tangem.features.send.loadedStatus
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FeeItemConverterTest {

    // Bitcoin status so the custom-fee field converter yields fields for a Bitcoin normalFee.
    private val feeStatus = loadedStatus(
        currency = MockCryptoCurrencyFactory().createCoin(Blockchain.Bitcoin),
        fiatRate = BigDecimal("50000"),
    )

    private val bitcoinFee: Fee = Fee.Bitcoin(
        amount = Amount(currencySymbol = "BTC", value = BigDecimal("0.0001"), decimals = 8),
        satoshiPerByte = BigDecimal("10"),
        txSize = BigDecimal("250"),
    )

    private fun converter(
        config: FeeSelectorParams.FeeStateConfiguration = FeeSelectorParams.FeeStateConfiguration.None,
        normalFee: Fee = commonFee(),
        shouldDisableCustomFee: Boolean = true,
    ) = FeeItemConverter(
        feeStateConfiguration = config,
        normalFee = normalFee,
        feeSelectorIntents = mockk(relaxed = true),
        appCurrency = AppCurrency.Default,
        cryptoCurrencyStatus = feeStatus,
        shouldDisableCustomFee = shouldDisableCustomFee,
    )

    private fun choosable() =
        TransactionFee.Choosable(normal = commonFee(), minimum = commonFee(), priority = commonFee())

    private fun single() = TransactionFee.Single(normal = commonFee())

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Items {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN config and transaction fee WHEN convert THEN fee items match configuration`(model: ItemsModel) {
            // Act  (custom fee disabled -> the list is purely config driven)
            val actual = converter(config = model.config)
                .convert(FeeItemConverter.Input(transactionFee = model.transactionFee, customFee = null))

            // Assert
            assertThat(actual.map { it::class.java }).containsExactlyElementsIn(model.expectedTypes).inOrder()
        }

        private fun provideTestModels() = listOf(
            ItemsModel(
                none(),
                choosable(),
                listOf(FeeItem.Slow::class.java, FeeItem.Market::class.java, FeeItem.Fast::class.java)
            ),
            ItemsModel(none(), single(), listOf(FeeItem.Market::class.java)),
            ItemsModel(
                suggestion(),
                choosable(),
                listOf(
                    FeeItem.Suggested::class.java,
                    FeeItem.Slow::class.java,
                    FeeItem.Market::class.java,
                    FeeItem.Fast::class.java
                ),
            ),
            ItemsModel(
                suggestion(),
                single(),
                listOf(FeeItem.Suggested::class.java, FeeItem.Market::class.java)
            ),
            ItemsModel(
                excludeLow(),
                choosable(),
                listOf(FeeItem.Market::class.java, FeeItem.Fast::class.java)
            ),
            ItemsModel(
                excludeLow(),
                single(),
                listOf(FeeItem.Market::class.java)
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class FeeAssignment {

        @Test
        fun `GIVEN choosable fee WHEN convert THEN slow market fast map to minimum normal priority`() {
            // Arrange  (distinct fees to detect any mis-mapping)
            val minimum = ethFee(value = "1")
            val normal = ethFee(value = "2")
            val priority = ethFee(value = "3")
            val fees = TransactionFee.Choosable(normal = normal, minimum = minimum, priority = priority)

            // Act
            val actual = converter(config = none()).convert(FeeItemConverter.Input(fees, customFee = null))

            // Assert
            assertThat((actual[0] as FeeItem.Slow).fee).isEqualTo(minimum)
            assertThat((actual[1] as FeeItem.Market).fee).isEqualTo(normal)
            assertThat((actual[2] as FeeItem.Fast).fee).isEqualTo(priority)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CustomFee {

        @Test
        fun `GIVEN custom enabled and supported fee WHEN convert THEN custom fee appended`() {
            // Act
            val actual = converter(normalFee = bitcoinFee, shouldDisableCustomFee = false)
                .convert(FeeItemConverter.Input(TransactionFee.Single(bitcoinFee), customFee = null))

            // Assert
            assertThat(actual).hasSize(2) // Market + Custom
            assertThat(actual.last()).isInstanceOf(FeeItem.Custom::class.java)
        }

        @Test
        fun `GIVEN custom disabled WHEN convert THEN no custom fee`() {
            // Act
            val actual = converter(normalFee = bitcoinFee, shouldDisableCustomFee = true)
                .convert(FeeItemConverter.Input(TransactionFee.Single(bitcoinFee), customFee = null))

            // Assert
            assertThat(actual).hasSize(1) // Market only
        }

        @Test
        fun `GIVEN unsupported fee with no custom fields WHEN convert THEN no custom fee`() {
            // Act  (Fee.Common has no custom field converter -> constructCustomFee returns null)
            val actual = converter(normalFee = commonFee(), shouldDisableCustomFee = false)
                .convert(FeeItemConverter.Input(TransactionFee.Single(commonFee()), customFee = null))

            // Assert
            assertThat(actual).hasSize(1) // Market only
        }

        @Test
        fun `GIVEN custom fee provided WHEN convert THEN provided custom reused`() {
            // Arrange
            val provided = FeeItem.Custom(fee = bitcoinFee, customValues = persistentListOf())

            // Act
            val actual = converter(normalFee = bitcoinFee, shouldDisableCustomFee = false)
                .convert(FeeItemConverter.Input(TransactionFee.Single(bitcoinFee), customFee = provided))

            // Assert
            assertThat(actual.last()).isEqualTo(provided)
        }
    }

    private fun none() = FeeSelectorParams.FeeStateConfiguration.None
    private fun excludeLow() = FeeSelectorParams.FeeStateConfiguration.ExcludeLow
    private fun suggestion() = FeeSelectorParams.FeeStateConfiguration.Suggestion(title = mockk(), fee = commonFee())

    private fun ethFee(value: String) =
        Fee.Common(Amount(currencySymbol = "ETH", value = BigDecimal(value), decimals = 18))

    data class ItemsModel(
        val config: FeeSelectorParams.FeeStateConfiguration,
        val transactionFee: TransactionFee,
        val expectedTypes: List<Class<out FeeItem>>,
    )
}