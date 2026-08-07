package com.tangem.features.send.feeselector.model.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeItem
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeNonce
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import com.tangem.features.send.api.subcomponents.feeSelector.params.FeeSelectorParams
import com.tangem.features.send.feeselector.model.FeeSelectorLogic
import com.tangem.features.send.loadedStatus
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FeeSelectorLoadedTransformerTest {

    private val currencyFactory = MockCryptoCurrencyFactory()
    private val coin: CryptoCurrency = currencyFactory.ethereum

    private val commonFee: Fee = Fee.Common(Amount(Blockchain.Ethereum))
    private val ethereumFee: Fee = Fee.Ethereum.Legacy(
        amount = Amount(Blockchain.Ethereum),
        gasLimit = BigInteger.valueOf(21_000),
        gasPrice = BigInteger.valueOf(1_000_000_000),
    )

    private fun status(currency: CryptoCurrency = coin): CryptoCurrencyStatus =
        loadedStatus(currency = currency, fiatRate = BigDecimal("2000"))

    private fun basic(normal: Fee): FeeSelectorLogic.LoadedFeeResult =
        FeeSelectorLogic.LoadedFeeResult.Basic(TransactionFee.Choosable(normal = normal, minimum = normal, priority = normal))

    private fun transformer(
        fees: FeeSelectorLogic.LoadedFeeResult,
        feeStateConfiguration: FeeSelectorParams.FeeStateConfiguration = FeeSelectorParams.FeeStateConfiguration.None,
    ) = FeeSelectorLoadedTransformer(
        cryptoCurrencyStatus = status(),
        feeCryptoCurrencyStatus = status(),
        appCurrency = AppCurrency.Default,
        fees = fees,
        feeStateConfiguration = feeStateConfiguration,
        isFeeApproximate = false,
        feeSelectorIntents = mockk(relaxed = true),
        shouldDisableCustomFee = true,
    )

    private fun prevContent(selected: FeeItem, feeNonce: FeeNonce = FeeNonce.None) = FeeSelectorUM.Content(
        isPrimaryButtonEnabled = true,
        fees = TransactionFee.Single(normal = commonFee),
        feeItems = persistentListOf(selected),
        selectedFeeItem = selected,
        feeExtraInfo = mockk(),
        feeFiatRateUM = null,
        feeNonce = feeNonce,
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SelectedFee {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN previous state WHEN transform THEN selected fee item resolved`(model: SelectedModel) {
            // Act
            val result = transformer(basic(commonFee)).transform(model.prevState) as FeeSelectorUM.Content

            // Assert
            assertThat(result.selectedFeeItem).isInstanceOf(model.expected)
        }

        private fun provideTestModels() = listOf(
            // no prior selection -> defaults to market (no suggested in this config)
            SelectedModel(FeeSelectorUM.Loading, FeeItem.Market::class.java),
            // prior loading selection -> market
            SelectedModel(prevContent(FeeItem.Loading), FeeItem.Market::class.java),
            // prior concrete selection -> same class preserved
            SelectedModel(prevContent(FeeItem.Fast(commonFee)), FeeItem.Fast::class.java),
            // prior class no longer present -> falls back to loading
            SelectedModel(prevContent(FeeItem.Suggested(title = mockk(), fee = commonFee)), FeeItem.Loading::class.java),
        )

        @Test
        fun `GIVEN selection falls back to loading WHEN transform THEN primary button disabled`() {
            // Act
            val result = transformer(basic(commonFee))
                .transform(prevContent(FeeItem.Suggested(title = mockk(), fee = commonFee))) as FeeSelectorUM.Content

            // Assert
            assertThat(result.selectedFeeItem).isEqualTo(FeeItem.Loading)
            assertThat(result.isPrimaryButtonEnabled).isFalse()
        }

        @Test
        fun `GIVEN resolved fee item WHEN transform THEN primary button enabled`() {
            // Act
            val result = transformer(basic(commonFee)).transform(FeeSelectorUM.Loading) as FeeSelectorUM.Content

            // Assert
            assertThat(result.isPrimaryButtonEnabled).isTrue()
        }

        @Test
        fun `GIVEN no prior selection and suggested available WHEN transform THEN suggested preselected`() {
            // Arrange  (Suggestion config makes the converter emit a Suggested item)
            val config = FeeSelectorParams.FeeStateConfiguration.Suggestion(title = mockk(), fee = commonFee)

            // Act
            val result = transformer(basic(commonFee), feeStateConfiguration = config)
                .transform(FeeSelectorUM.Loading) as FeeSelectorUM.Content

            // Assert
            assertThat(result.selectedFeeItem).isInstanceOf(FeeItem.Suggested::class.java)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Nonce {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN normal fee type WHEN transform THEN nonce field present only for ethereum`(model: NonceTypeModel) {
            // Act
            val result = transformer(basic(model.normal)).transform(FeeSelectorUM.Loading) as FeeSelectorUM.Content

            // Assert
            assertThat(result.feeNonce).isInstanceOf(model.expected)
        }

        private fun provideTestModels() = listOf(
            NonceTypeModel(ethereumFee, FeeNonce.Nonce::class.java),
            NonceTypeModel(commonFee, FeeNonce.None::class.java),
        )

        @Test
        fun `GIVEN ethereum fee and previous nonce WHEN transform THEN previous nonce preserved`() {
            // Arrange
            val prev = prevContent(
                selected = FeeItem.Market(commonFee),
                feeNonce = FeeNonce.Nonce(nonce = BigInteger.valueOf(7), onNonceChange = {}),
            )

            // Act
            val result = transformer(basic(ethereumFee)).transform(prev) as FeeSelectorUM.Content

            // Assert
            assertThat((result.feeNonce as FeeNonce.Nonce).nonce).isEqualTo(BigInteger.valueOf(7))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ExtraInfo {

        @Test
        fun `GIVEN basic fee result WHEN transform THEN extra info reflects basic non-tron status`() {
            // Act
            val result = transformer(basic(commonFee)).transform(FeeSelectorUM.Loading) as FeeSelectorUM.Content

            // Assert
            val info = result.feeExtraInfo
            assertThat(info.availableFeeCurrencies).isNull() // Extended-only
            assertThat(info.transactionFeeExtended).isNull() // Extended-only
            assertThat(info.isTronToken).isFalse()
            assertThat(info.isFeeConvertibleToFiat).isEqualTo(coin.network.hasFiatFeeRate)
            assertThat(result.feeFiatRateUM).isNotNull()
        }
    }

    data class SelectedModel(val prevState: FeeSelectorUM, val expected: Class<out FeeItem>)
    data class NonceTypeModel(val normal: Fee, val expected: Class<out FeeNonce>)
}