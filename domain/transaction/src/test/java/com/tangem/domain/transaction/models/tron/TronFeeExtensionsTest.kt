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
    @MethodSource("provideCoverageModels")
    fun isCoveredByOwnTronEnergy(model: CoverageModel) {
        // Act
        val actual = TransactionFee.Single(normal = model.fee).isCoveredByOwnTronEnergy()

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    internal data class CoverageModel(val fee: Fee, val expected: Boolean)

    private fun provideCoverageModels() = listOf(
        CoverageModel(fee = tronFee(remainingEnergy = 70_000, feeEnergy = 65_000), expected = true),
        CoverageModel(fee = tronFee(remainingEnergy = 65_000, feeEnergy = 65_000), expected = true),
        CoverageModel(fee = tronFee(remainingEnergy = 10_000, feeEnergy = 65_000), expected = false),
        CoverageModel(fee = tronFee(remainingEnergy = 0, feeEnergy = 65_000), expected = false),
        CoverageModel(fee = tronFee(remainingEnergy = 0, feeEnergy = 0), expected = false),
        CoverageModel(fee = tronFee(remainingEnergy = 70_000, feeEnergy = 0), expected = false),
        CoverageModel(fee = commonFee("0"), expected = false),
    )

    @ParameterizedTest
    @ProvideTestModels
    fun isCheaperInFiatThan(model: ComparisonModel) {
        // Arrange
        val nativeFee = TransactionFee.Single(normal = commonFee(model.nativeFee))
        val gaslessFee = TransactionFee.Single(normal = commonFee(model.gaslessFee))

        // Act
        val actual = nativeFee.isCheaperInFiatThan(
            gaslessFee = gaslessFee,
            nativeStatus = status(rate = model.nativeRate),
            gaslessTokenStatus = status(rate = model.gaslessRate),
        )

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    internal data class ComparisonModel(
        val nativeFee: String,
        val gaslessFee: String,
        val expected: Boolean,
        val nativeRate: String? = TRX_RATE,
        val gaslessRate: String? = USDT_RATE,
    )

    private fun provideTestModels() = listOf(
        ComparisonModel(nativeFee = "0.35", gaslessFee = "2.41", expected = true),
        ComparisonModel(nativeFee = "6.025", gaslessFee = "2.41", expected = true),
        ComparisonModel(nativeFee = "6.43", gaslessFee = "2.41", expected = false),
        ComparisonModel(nativeFee = "0.35", gaslessFee = "2.41", nativeRate = null, expected = false),
        ComparisonModel(nativeFee = "0.35", gaslessFee = "2.41", gaslessRate = null, expected = false),
        ComparisonModel(nativeFee = "6.43", gaslessFee = "2.41", nativeRate = "0", expected = false),
        ComparisonModel(nativeFee = "0.35", gaslessFee = "2.41", gaslessRate = "0", expected = false),
    )

    @ParameterizedTest
    @MethodSource("providePaymentModels")
    fun canPayNativeFee(model: PaymentModel) {
        // Arrange
        val status = status(isCoin = model.isCoin, balance = model.balance)

        // Act
        val actual = status.canPayNativeFee(TransactionFee.Single(normal = commonFee(model.fee)))

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

    @ParameterizedTest
    @MethodSource("provideOwnEnergyModels")
    fun isPaidByOwnTronEnergy(model: OwnEnergyModel) {
        // Arrange
        val fee = TransactionFee.Single(normal = tronFee(remainingEnergy = 70_000, feeEnergy = 65_000))

        // Act
        val actual = fee.isPaidByOwnTronEnergy(status(balance = model.nativeBalance, rate = model.nativeRate))

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    internal data class OwnEnergyModel(val nativeBalance: String, val nativeRate: String?, val expected: Boolean)

    private fun provideOwnEnergyModels() = listOf(
        OwnEnergyModel(nativeBalance = "50", nativeRate = null, expected = true),
        OwnEnergyModel(nativeBalance = "0.1", nativeRate = TRX_RATE, expected = false),
    )

    @ParameterizedTest
    @MethodSource("providePreferenceModels")
    fun isPreferredOverTronGasless(model: PreferenceModel) {
        // Arrange
        val nativeFee = TransactionFee.Single(normal = commonFee(model.nativeFee))
        val gaslessFee = TransactionFee.Single(normal = commonFee("2.41"))

        // Act
        val actual = nativeFee.isPreferredOverTronGasless(
            gaslessFee = gaslessFee,
            nativeStatus = status(balance = model.nativeBalance),
            gaslessTokenStatus = status(isCoin = false, rate = USDT_RATE),
        )

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    internal data class PreferenceModel(val nativeFee: String, val nativeBalance: String, val expected: Boolean)

    private fun providePreferenceModels() = listOf(
        PreferenceModel(nativeFee = "1", nativeBalance = "50", expected = true),
        PreferenceModel(nativeFee = "1", nativeBalance = "0.5", expected = false),
        PreferenceModel(nativeFee = "6.43", nativeBalance = "50", expected = false),
    )

    private fun commonFee(value: String) = Fee.Common(Amount(BigDecimal(value), Blockchain.Tron))

    private fun tronFee(remainingEnergy: Long, feeEnergy: Long) = Fee.Tron(
        amount = Amount(BigDecimal("0.35"), Blockchain.Tron),
        remainingEnergy = remainingEnergy,
        feeEnergy = feeEnergy,
    )

    private fun status(
        isCoin: Boolean = true,
        balance: String? = "50",
        rate: String? = TRX_RATE,
    ): CryptoCurrencyStatus {
        val statusCurrency = if (isCoin) mockk<CryptoCurrency.Coin>() else mockk<CryptoCurrency.Token>()
        val statusValue = mockk<CryptoCurrencyStatus.Value> {
            every { amount } returns balance?.let(::BigDecimal)
            every { fiatRate } returns rate?.let(::BigDecimal)
        }
        return mockk {
            every { currency } returns statusCurrency
            every { value } returns statusValue
        }
    }

    private companion object {
        const val TRX_RATE = "0.4"
        const val USDT_RATE = "1"
    }
}