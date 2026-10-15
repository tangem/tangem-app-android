package com.tangem.domain.transaction.models.tron

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TronFeeExtensionsTest {

    @ParameterizedTest
    @ProvideTestModels
    fun isCoveredByOwnTronEnergy(model: CoverageModel) {
        // Act
        val actual = TransactionFee.Single(normal = model.fee).isCoveredByOwnTronEnergy()

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    internal data class CoverageModel(val fee: Fee, val expected: Boolean)

    private fun provideTestModels() = listOf(
        CoverageModel(fee = tronFee(remainingEnergy = 70_000, feeEnergy = 65_000), expected = true),
        CoverageModel(fee = tronFee(remainingEnergy = 65_000, feeEnergy = 65_000), expected = true),
        CoverageModel(fee = tronFee(remainingEnergy = 10_000, feeEnergy = 65_000), expected = false),
        CoverageModel(fee = tronFee(remainingEnergy = 0, feeEnergy = 65_000), expected = false),
        CoverageModel(fee = tronFee(remainingEnergy = 0, feeEnergy = 0), expected = false),
        CoverageModel(fee = tronFee(remainingEnergy = 70_000, feeEnergy = 0), expected = false),
        CoverageModel(fee = Fee.Common(Amount(BigDecimal.ZERO, Blockchain.Tron)), expected = false),
    )

    private fun tronFee(remainingEnergy: Long, feeEnergy: Long) = Fee.Tron(
        amount = Amount(BigDecimal("0.35"), Blockchain.Tron),
        remainingEnergy = remainingEnergy,
        feeEnergy = feeEnergy,
    )

    @ParameterizedTest
    @MethodSource("providePaymentModels")
    fun canPayNativeFee(model: PaymentModel) {
        // Arrange
        val status = nativeStatus(isCoin = model.isCoin, balance = model.balance)
        val fee = TransactionFee.Single(normal = Fee.Common(Amount(BigDecimal(model.fee), Blockchain.Tron)))

        // Act
        val actual = status.canPayNativeFee(fee)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    internal data class PaymentModel(val isCoin: Boolean, val balance: String?, val fee: String, val expected: Boolean)

    private fun providePaymentModels() = listOf(
        PaymentModel(isCoin = true, balance = "1", fee = "0.35", expected = true),
        PaymentModel(isCoin = true, balance = "0.35", fee = "0.35", expected = true),
        PaymentModel(isCoin = true, balance = "0.1", fee = "0.35", expected = false),
        PaymentModel(isCoin = true, balance = null, fee = "0.35", expected = false),
        PaymentModel(isCoin = false, balance = "50", fee = "0.35", expected = false),
    )

    private fun nativeStatus(isCoin: Boolean, balance: String?): CryptoCurrencyStatus {
        val statusCurrency = if (isCoin) mockk<CryptoCurrency.Coin>() else mockk<CryptoCurrency.Token>()
        val statusValue = mockk<CryptoCurrencyStatus.Value> { every { amount } returns balance?.let(::BigDecimal) }
        return mockk {
            every { currency } returns statusCurrency
            every { value } returns statusValue
        }
    }
}