package com.tangem.features.send.feeselector.model.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.features.send.loadedStatus
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FeeSelectorCustomFieldConverterTest {

    private val currencyFactory = MockCryptoCurrencyFactory()

    // Bitcoin network so the Bitcoin converter passes its isUseBitcoinFeeConverter() check; other converters
    // don't read the network, so a single status drives every dispatch branch.
    private val feeStatus = loadedStatus(
        currency = currencyFactory.createCoin(Blockchain.Bitcoin),
        fiatRate = BigDecimal("50000"),
    )

    private val commonFee: Fee = Fee.Common(Amount(Blockchain.Ethereum))
    private val bitcoinFee: Fee = Fee.Bitcoin(
        amount = Amount(currencySymbol = "BTC", value = BigDecimal("0.0001"), decimals = 8),
        satoshiPerByte = BigDecimal("10"),
        txSize = BigDecimal("250"),
    )
    private val ethereumFee: Fee = Fee.Ethereum.EIP1559(
        amount = Amount(Blockchain.Ethereum),
        gasLimit = BigInteger.valueOf(21_000),
        maxFeePerGas = BigInteger.valueOf(30_000_000_000),
        priorityFee = BigInteger.valueOf(2_000_000_000),
    )
    private val kaspaFee: Fee = Fee.Kaspa(
        amount = Amount(currencySymbol = "KAS", value = BigDecimal("0.0001"), decimals = 8),
        mass = BigInteger.valueOf(2000),
        feeRate = BigInteger.valueOf(5),
    )

    private fun converter(normalFee: Fee = commonFee) = FeeSelectorCustomFieldConverter(
        feeSelectorIntents = mockk(relaxed = true),
        appCurrency = AppCurrency.Default,
        feeCryptoCurrencyStatus = feeStatus,
        normalFee = normalFee,
    )

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN fee type WHEN convert THEN routed to matching custom fee converter`(model: DispatchModel) {
        // Act
        val actual = converter().convert(model.fee)

        // Assert  (each converter emits a distinct number of fields - a fingerprint of correct routing)
        assertThat(actual).hasSize(model.expectedFieldCount)
    }

    private fun provideTestModels() = listOf(
        DispatchModel(fee = bitcoinFee, expectedFieldCount = 2), // amount + satoshi/byte
        DispatchModel(fee = ethereumFee, expectedFieldCount = 4), // amount + maxFee + priority + gasLimit
        DispatchModel(fee = kaspaFee, expectedFieldCount = 1), // amount
        DispatchModel(fee = commonFee, expectedFieldCount = 0), // unsupported -> empty
    )

    @Test
    fun `GIVEN empty custom values WHEN convertBack THEN returns normal fee unchanged`() {
        // Act
        val actual = converter(normalFee = commonFee).convertBack(persistentListOf())

        // Assert
        assertThat(actual).isSameInstanceAs(commonFee)
    }

    data class DispatchModel(val fee: Fee, val expectedFieldCount: Int)
}