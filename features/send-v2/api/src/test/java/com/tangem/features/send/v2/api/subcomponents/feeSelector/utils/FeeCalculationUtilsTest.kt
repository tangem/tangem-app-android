package com.tangem.features.send.v2.api.subcomponents.feeSelector.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class FeeCalculationUtilsTest {

    @Test
    fun `GIVEN amount subtract available and fee coverage needed WHEN checkAndCalculateSubtractedAmount THEN returns subtracted amount`() {
        // GIVEN
        val isAmountSubtractAvailable = true
        val cryptoCurrencyStatus = createCryptoCurrencyStatus(BigDecimal("6"))
        val amountValue = BigDecimal("5")
        val feeValue = BigDecimal("2")
        val reduceAmountBy = BigDecimal("1")

        // WHEN
        val result = FeeCalculationUtils.checkAndCalculateSubtractedAmount(
            isAmountSubtractAvailable = isAmountSubtractAvailable,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            amountValue = amountValue,
            feeValue = feeValue,
            reduceAmountBy = reduceAmountBy,
        )

        // THEN
        assertThat(result).isEqualTo(BigDecimal("3"))
    }

    @Test
    fun `GIVEN amount subtract not available WHEN checkAndCalculateSubtractedAmount THEN returns original amount`() {
        // GIVEN
        val isAmountSubtractAvailable = false
        val cryptoCurrencyStatus = createCryptoCurrencyStatus(BigDecimal("10"))
        val amountValue = BigDecimal("5")
        val feeValue = BigDecimal("2")
        val reduceAmountBy = BigDecimal("1")

        // WHEN
        val result = FeeCalculationUtils.checkAndCalculateSubtractedAmount(
            isAmountSubtractAvailable = isAmountSubtractAvailable,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            amountValue = amountValue,
            feeValue = feeValue,
            reduceAmountBy = reduceAmountBy,
        )

        // THEN
        assertThat(result).isEqualTo(amountValue)
    }

    @Test
    fun `GIVEN sufficient balance for amount and fee WHEN checkAndCalculateSubtractedAmount THEN returns original amount`() {
        // GIVEN
        val isAmountSubtractAvailable = true
        val cryptoCurrencyStatus = createCryptoCurrencyStatus(BigDecimal("10"))
        val amountValue = BigDecimal("5")
        val feeValue = BigDecimal("2")
        val reduceAmountBy = BigDecimal("1")

        // WHEN
        val result = FeeCalculationUtils.checkAndCalculateSubtractedAmount(
            isAmountSubtractAvailable = isAmountSubtractAvailable,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            amountValue = amountValue,
            feeValue = feeValue,
            reduceAmountBy = reduceAmountBy,
        )

        // THEN
        assertThat(result).isEqualTo(amountValue)
    }

    @Test
    fun `GIVEN no balance WHEN checkAndCalculateSubtractedAmount THEN returns original amount`() {
        // GIVEN
        val isAmountSubtractAvailable = true
        val cryptoCurrencyStatus = createCryptoCurrencyStatus(null)
        val amountValue = BigDecimal("5")
        val feeValue = BigDecimal("2")
        val reduceAmountBy = BigDecimal("1")

        // WHEN
        val result = FeeCalculationUtils.checkAndCalculateSubtractedAmount(
            isAmountSubtractAvailable = isAmountSubtractAvailable,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            amountValue = amountValue,
            feeValue = feeValue,
            reduceAmountBy = reduceAmountBy,
        )

        // THEN
        assertThat(result).isEqualTo(amountValue)
    }

    @Test
    fun `GIVEN fee exceeds balance WHEN checkExceedBalance THEN returns true`() {
        // GIVEN
        val feeBalance = BigDecimal("5")
        val feeAmount = BigDecimal("10")

        // WHEN
        val result = FeeCalculationUtils.checkExceedBalance(feeBalance, feeAmount)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `GIVEN fee within balance WHEN checkExceedBalance THEN returns false`() {
        // GIVEN
        val feeBalance = BigDecimal("10")
        val feeAmount = BigDecimal("5")

        // WHEN
        val result = FeeCalculationUtils.checkExceedBalance(feeBalance, feeAmount)

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `GIVEN null fee amount WHEN checkExceedBalance THEN returns true`() {
        // GIVEN
        val feeBalance = BigDecimal("10")
        val feeAmount: BigDecimal? = null

        // WHEN
        val result = FeeCalculationUtils.checkExceedBalance(feeBalance, feeAmount)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `GIVEN null fee balance WHEN checkExceedBalance THEN returns true`() {
        // GIVEN
        val feeBalance: BigDecimal? = null
        val feeAmount = BigDecimal("5")

        // WHEN
        val result = FeeCalculationUtils.checkExceedBalance(feeBalance, feeAmount)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `GIVEN zero fee amount WHEN checkExceedBalance THEN returns true`() {
        // GIVEN
        val feeBalance = BigDecimal("10")
        val feeAmount = BigDecimal.ZERO

        // WHEN
        val result = FeeCalculationUtils.checkExceedBalance(feeBalance, feeAmount)

        // THEN
        assertThat(result).isTrue()
    }

    private fun createCryptoCurrencyStatus(amount: BigDecimal?): CryptoCurrencyStatus {
        val value = if (amount != null) {
            CryptoCurrencyStatus.Loaded(
                amount = amount,
                fiatAmount = BigDecimal.ZERO,
                fiatRate = BigDecimal.ZERO,
                priceChange = BigDecimal.ZERO,
                yieldBalance = null,
                hasCurrentNetworkTransactions = false,
                pendingTransactions = emptySet(),
                networkAddress = mockk(relaxed = true),
                sources = CryptoCurrencyStatus.Sources(),
            )
        } else {
            CryptoCurrencyStatus.NoAmount(
                priceChange = BigDecimal.ZERO,
                fiatRate = BigDecimal.ZERO,
            )
        }
        return CryptoCurrencyStatus(
            currency = mockk(relaxed = true),
            value = value,
        )
    }
}