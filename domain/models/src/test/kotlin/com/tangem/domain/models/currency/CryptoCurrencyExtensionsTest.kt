package com.tangem.domain.models.currency

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CryptoCurrencyExtensionsTest {

    @Test
    fun `GIVEN currency is Coin WHEN hasNotSuppliedAmount THEN returns false`() {
        val status = createCryptoCurrencyStatus(
            currency = mockk<CryptoCurrency.Coin>(),
        )

        val result = status.hasNotSuppliedAmount()

        assertThat(result).isFalse()
    }

    @Test
    fun `GIVEN yieldSupplyStatus is null WHEN hasNotSuppliedAmount THEN returns false`() {
        val status = createCryptoCurrencyStatus(
            currency = mockk<CryptoCurrency.Token>(),
            yieldSupplyStatus = null,
        )

        val result = status.hasNotSuppliedAmount()

        assertThat(result).isFalse()
    }

    @Test
    fun `GIVEN yieldSupplyStatus isActive is false WHEN hasNotSuppliedAmount THEN returns false`() {
        val status = createCryptoCurrencyStatus(
            currency = mockk<CryptoCurrency.Token>(),
            yieldSupplyStatus = YieldSupplyStatus(
                isActive = false,
                isInitialized = true,
                isAllowedToSpend = true,
                effectiveProtocolBalance = BigDecimal.TEN,
            ),
        )

        val result = status.hasNotSuppliedAmount()

        assertThat(result).isFalse()
    }

    @Test
    fun `GIVEN effectiveProtocolBalance is null WHEN hasNotSuppliedAmount THEN returns false`() {
        val status = createCryptoCurrencyStatus(
            currency = mockk<CryptoCurrency.Token>(),
            yieldSupplyStatus = YieldSupplyStatus(
                isActive = true,
                isInitialized = true,
                isAllowedToSpend = true,
                effectiveProtocolBalance = null,
            ),
            amount = BigDecimal.TEN,
        )

        val result = status.hasNotSuppliedAmount()

        assertThat(result).isFalse()
    }

    @Test
    fun `GIVEN amount is null WHEN hasNotSuppliedAmount THEN returns false`() {
        val status = createCryptoCurrencyStatus(
            currency = mockk<CryptoCurrency.Token>(),
            yieldSupplyStatus = YieldSupplyStatus(
                isActive = true,
                isInitialized = true,
                isAllowedToSpend = true,
                effectiveProtocolBalance = BigDecimal.TEN,
            ),
            amount = null,
        )

        val result = status.hasNotSuppliedAmount()

        assertThat(result).isFalse()
    }

    @Test
    fun `GIVEN notSupplied is zero WHEN hasNotSuppliedAmount THEN returns false`() {
        val status = createCryptoCurrencyStatus(
            currency = mockk<CryptoCurrency.Token>(),
            yieldSupplyStatus = YieldSupplyStatus(
                isActive = true,
                isInitialized = true,
                isAllowedToSpend = true,
                effectiveProtocolBalance = BigDecimal.TEN,
            ),
            amount = BigDecimal.TEN,
        )

        val result = status.hasNotSuppliedAmount()

        assertThat(result).isFalse()
    }

    @Test
    fun `GIVEN notSupplied is negative WHEN hasNotSuppliedAmount THEN returns false`() {
        val status = createCryptoCurrencyStatus(
            currency = mockk<CryptoCurrency.Token>(),
            yieldSupplyStatus = YieldSupplyStatus(
                isActive = true,
                isInitialized = true,
                isAllowedToSpend = true,
                effectiveProtocolBalance = BigDecimal.TEN,
            ),
            amount = BigDecimal.ONE,
        )

        val result = status.hasNotSuppliedAmount()

        assertThat(result).isFalse()
    }

    @Test
    fun `GIVEN notSupplied is positive WHEN hasNotSuppliedAmount THEN returns true`() {
        val status = createCryptoCurrencyStatus(
            currency = mockk<CryptoCurrency.Token>(),
            yieldSupplyStatus = YieldSupplyStatus(
                isActive = true,
                isInitialized = true,
                isAllowedToSpend = true,
                effectiveProtocolBalance = BigDecimal.ONE,
            ),
            amount = BigDecimal.TEN,
        )

        val result = status.hasNotSuppliedAmount()

        assertThat(result).isTrue()
    }

    @Test
    fun `GIVEN currency is Coin WHEN shouldShowNotSuppliedNotification THEN returns false`() {
        val status = createCryptoCurrencyStatus(
            currency = mockk<CryptoCurrency.Coin>(),
        )

        val result = status.shouldShowNotSuppliedNotification(dustAmount = BigDecimal.ONE)

        assertThat(result).isFalse()
    }

    @Test
    fun `GIVEN yieldSupplyStatus is null WHEN shouldShowNotSuppliedNotification THEN returns false`() {
        val status = createCryptoCurrencyStatus(
            currency = mockk<CryptoCurrency.Token>(),
            yieldSupplyStatus = null,
        )

        val result = status.shouldShowNotSuppliedNotification(dustAmount = BigDecimal.ONE)

        assertThat(result).isFalse()
    }

    @Test
    fun `GIVEN yieldSupplyStatus isActive is false WHEN shouldShowNotSuppliedNotification THEN returns false`() {
        val status = createCryptoCurrencyStatus(
            currency = mockk<CryptoCurrency.Token>(),
            yieldSupplyStatus = YieldSupplyStatus(
                isActive = false,
                isInitialized = true,
                isAllowedToSpend = true,
                effectiveProtocolBalance = BigDecimal.TEN,
            ),
        )

        val result = status.shouldShowNotSuppliedNotification(dustAmount = BigDecimal.ONE)

        assertThat(result).isFalse()
    }

    @Test
    fun `GIVEN effectiveProtocolBalance is null WHEN shouldShowNotSuppliedNotification THEN returns false`() {
        val status = createCryptoCurrencyStatus(
            currency = mockk<CryptoCurrency.Token>(),
            yieldSupplyStatus = YieldSupplyStatus(
                isActive = true,
                isInitialized = true,
                isAllowedToSpend = true,
                effectiveProtocolBalance = null,
            ),
            amount = BigDecimal.TEN,
            fiatRate = BigDecimal.ONE,
        )

        val result = status.shouldShowNotSuppliedNotification(dustAmount = BigDecimal.ONE)

        assertThat(result).isFalse()
    }

    @Test
    fun `GIVEN notSupplied in fiat is less than dustAmount WHEN shouldShowNotSuppliedNotification THEN returns false`() {
        val status = createCryptoCurrencyStatus(
            currency = mockk<CryptoCurrency.Token>(),
            yieldSupplyStatus = YieldSupplyStatus(
                isActive = true,
                isInitialized = true,
                isAllowedToSpend = true,
                effectiveProtocolBalance = BigDecimal("9"),
            ),
            amount = BigDecimal.TEN,
            fiatRate = BigDecimal.ONE,
        )

        val result = status.shouldShowNotSuppliedNotification(dustAmount = BigDecimal.TEN)

        assertThat(result).isFalse()
    }

    @Test
    fun `GIVEN notSupplied in fiat equals dustAmount WHEN shouldShowNotSuppliedNotification THEN returns true`() {
        val status = createCryptoCurrencyStatus(
            currency = mockk<CryptoCurrency.Token>(),
            yieldSupplyStatus = YieldSupplyStatus(
                isActive = true,
                isInitialized = true,
                isAllowedToSpend = true,
                effectiveProtocolBalance = BigDecimal("5"),
            ),
            amount = BigDecimal.TEN,
            fiatRate = BigDecimal("2"),
        )

        val result = status.shouldShowNotSuppliedNotification(dustAmount = BigDecimal.TEN)

        assertThat(result).isTrue()
    }

    @Test
    fun `GIVEN notSupplied in fiat is greater than dustAmount WHEN shouldShowNotSuppliedNotification THEN returns true`() {
        val status = createCryptoCurrencyStatus(
            currency = mockk<CryptoCurrency.Token>(),
            yieldSupplyStatus = YieldSupplyStatus(
                isActive = true,
                isInitialized = true,
                isAllowedToSpend = true,
                effectiveProtocolBalance = BigDecimal.ONE,
            ),
            amount = BigDecimal.TEN,
            fiatRate = BigDecimal("2"),
        )

        val result = status.shouldShowNotSuppliedNotification(dustAmount = BigDecimal.TEN)

        assertThat(result).isTrue()
    }

    @Test
    fun `GIVEN USDT with EUR fiat rate and notSupplied below dust WHEN shouldShowNotSuppliedNotification THEN returns false`() {
        val usdtToEurRate = BigDecimal("0.93")
        val notSuppliedUsdt = BigDecimal("0.05")
        val protocolBalance = BigDecimal("100")
        val totalAmount = protocolBalance.add(notSuppliedUsdt)
        val dustAmountEur = BigDecimal("0.1")

        val status = createCryptoCurrencyStatus(
            currency = mockk<CryptoCurrency.Token>(),
            yieldSupplyStatus = YieldSupplyStatus(
                isActive = true,
                isInitialized = true,
                isAllowedToSpend = true,
                effectiveProtocolBalance = protocolBalance,
            ),
            amount = totalAmount,
            fiatRate = usdtToEurRate,
        )

        val result = status.shouldShowNotSuppliedNotification(dustAmount = dustAmountEur)

        assertThat(result).isFalse()
    }

    @Test
    fun `GIVEN USDT with EUR fiat rate and notSupplied above dust WHEN shouldShowNotSuppliedNotification THEN returns true`() {
        val usdtToEurRate = BigDecimal("0.93")
        val notSuppliedUsdt = BigDecimal("0.15")
        val protocolBalance = BigDecimal("100")
        val totalAmount = protocolBalance.add(notSuppliedUsdt)
        val dustAmountEur = BigDecimal("0.1")

        val status = createCryptoCurrencyStatus(
            currency = mockk<CryptoCurrency.Token>(),
            yieldSupplyStatus = YieldSupplyStatus(
                isActive = true,
                isInitialized = true,
                isAllowedToSpend = true,
                effectiveProtocolBalance = protocolBalance,
            ),
            amount = totalAmount,
            fiatRate = usdtToEurRate,
        )

        val result = status.shouldShowNotSuppliedNotification(dustAmount = dustAmountEur)

        assertThat(result).isTrue()
    }

    @Test
    fun `GIVEN currency is Coin WHEN notSuppliedCryptoAmountOrNull THEN returns null`() {
        val status = createCryptoCurrencyStatus(
            currency = mockk<CryptoCurrency.Coin>(),
        )

        val result = status.notSuppliedCryptoAmountOrNull()

        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN yieldSupplyStatus is null WHEN notSuppliedCryptoAmountOrNull THEN returns null`() {
        val status = createCryptoCurrencyStatus(
            currency = mockk<CryptoCurrency.Token>(),
            yieldSupplyStatus = null,
        )

        val result = status.notSuppliedCryptoAmountOrNull()

        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN yieldSupplyStatus isActive is false WHEN notSuppliedCryptoAmountOrNull THEN returns null`() {
        val status = createCryptoCurrencyStatus(
            currency = mockk<CryptoCurrency.Token>(),
            yieldSupplyStatus = YieldSupplyStatus(
                isActive = false,
                isInitialized = true,
                isAllowedToSpend = true,
                effectiveProtocolBalance = BigDecimal.TEN,
            ),
        )

        val result = status.notSuppliedCryptoAmountOrNull()

        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN effectiveProtocolBalance is null WHEN notSuppliedCryptoAmountOrNull THEN returns null`() {
        val status = createCryptoCurrencyStatus(
            currency = mockk<CryptoCurrency.Token>(),
            yieldSupplyStatus = YieldSupplyStatus(
                isActive = true,
                isInitialized = true,
                isAllowedToSpend = true,
                effectiveProtocolBalance = null,
            ),
            amount = BigDecimal.TEN,
        )

        val result = status.notSuppliedCryptoAmountOrNull()

        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN amount is null WHEN notSuppliedCryptoAmountOrNull THEN returns null`() {
        val status = createCryptoCurrencyStatus(
            currency = mockk<CryptoCurrency.Token>(),
            yieldSupplyStatus = YieldSupplyStatus(
                isActive = true,
                isInitialized = true,
                isAllowedToSpend = true,
                effectiveProtocolBalance = BigDecimal.TEN,
            ),
            amount = null,
        )

        val result = status.notSuppliedCryptoAmountOrNull()

        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN valid data WHEN notSuppliedCryptoAmountOrNull THEN returns amount minus protocolBalance`() {
        val status = createCryptoCurrencyStatus(
            currency = mockk<CryptoCurrency.Token>(),
            yieldSupplyStatus = YieldSupplyStatus(
                isActive = true,
                isInitialized = true,
                isAllowedToSpend = true,
                effectiveProtocolBalance = BigDecimal("3"),
            ),
            amount = BigDecimal.TEN,
        )

        val result = status.notSuppliedCryptoAmountOrNull()

        assertThat(result).isEqualTo(BigDecimal("7"))
    }

    private fun createCryptoCurrencyStatus(
        currency: CryptoCurrency,
        yieldSupplyStatus: YieldSupplyStatus? = null,
        amount: BigDecimal? = null,
        fiatRate: BigDecimal? = null,
    ): CryptoCurrencyStatus {
        val value = mockk<CryptoCurrencyStatus.Value> {
            every { this@mockk.yieldSupplyStatus } returns yieldSupplyStatus
            every { this@mockk.amount } returns amount
            every { this@mockk.fiatRate } returns fiatRate
        }
        return CryptoCurrencyStatus(currency = currency, value = value)
    }
}