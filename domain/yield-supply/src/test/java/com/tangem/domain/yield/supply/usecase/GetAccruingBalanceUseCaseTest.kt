package com.tangem.domain.yield.supply.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.format.bigdecimal.anyDecimals
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.balance.AccruingBalanceContribution
import com.tangem.domain.models.currency.balance.BalanceContribution
import com.tangem.domain.yield.supply.models.YieldSupplyRewardBalance
import com.tangem.domain.yield.supply.usecase.RewardsBalanceTicker.TICK_MILLIS
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import com.tangem.test.mock.yield.MockYieldSupply
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class GetAccruingBalanceUseCaseTest {

    private val coin = MockYieldSupply.coin()
    private val appCurrency = AppCurrency.Default

    @Test
    fun `GIVEN position and stake WHEN invoke THEN total ticks with the position only`() = runTest {
        // Arrange
        val status = loadedStatus(
            amount = BigDecimal("1"),
            fiatRate = BigDecimal("3000"),
            contributions = listOf(stake(BigDecimal("0.5")), position(BigDecimal("2"), rate = BigDecimal("0.0255"))),
        )
        val useCase = GetAccruingBalanceUseCase(createTestingCoroutineDispatcherProvider())

        // Act
        val deferred = async { useCase(status, appCurrency).take(3).toList() }
        runCurrent()
        repeat(2) {
            advanceTimeBy(TICK_MILLIS)
            runCurrent()
        }

        // Assert
        assertThat(deferred.await()).containsExactly(
            balance(crypto = "3.5", fiatRate = BigDecimal("3000")),
            balance(crypto = "3.500000001293759513", fiatRate = BigDecimal("3000")),
            balance(crypto = "3.500000002587519027", fiatRate = BigDecimal("3000")),
        ).inOrder()
    }

    @Test
    fun `GIVEN no fiat rate WHEN invoke THEN only the crypto balance ticks`() = runTest {
        // Arrange
        val status = CryptoCurrencyStatus(
            currency = coin,
            value = (loadedStatus(amount = BigDecimal("1"), contributions = emptyList()).value as CryptoCurrencyStatus.Loaded)
                .let { loaded ->
                    CryptoCurrencyStatus.NoQuote(
                        amount = loaded.amount,
                        yieldSupplyStatus = null,
                        hasCurrentNetworkTransactions = false,
                        pendingTransactions = emptySet(),
                        networkAddress = loaded.networkAddress,
                        sources = loaded.sources,
                        contributions = listOf(position(BigDecimal("2"), rate = BigDecimal("0.0255"))),
                    )
                },
        )
        val useCase = GetAccruingBalanceUseCase(createTestingCoroutineDispatcherProvider())

        // Act
        val first = async { useCase(status, appCurrency).take(1).toList() }
        runCurrent()

        // Assert
        assertThat(first.await()).containsExactly(balance(crypto = "3", fiatRate = null))
    }

    @Test
    fun `GIVEN tiny position next to large balance WHEN invoke THEN ticks with 10 fiat and 14 crypto decimals`() = runTest {
        // Arrange
        val status = loadedStatus(
            amount = BigDecimal("10"),
            fiatRate = BigDecimal("3000"),
            contributions = listOf(position(BigDecimal("0.0001"), rate = BigDecimal("0.0255"))),
        )
        val useCase = GetAccruingBalanceUseCase(createTestingCoroutineDispatcherProvider())

        // Act
        val deferred = async { useCase(status, appCurrency).take(2).toList() }
        runCurrent()
        advanceTimeBy(TICK_MILLIS)
        runCurrent()

        // Assert
        val fiatRate = BigDecimal("3000")
        assertThat(deferred.await()).containsExactly(
            balance(crypto = "10.0001", fiatRate = fiatRate, fiatDecimals = 10, cryptoDecimals = 14),
            balance(crypto = "10.000100000000064688", fiatRate = fiatRate, fiatDecimals = 10, cryptoDecimals = 14),
        ).inOrder()
    }

    @Test
    fun `GIVEN no accruing extra balance WHEN invoke THEN emits nothing`() = runTest {
        // Arrange
        val status = loadedStatus(
            amount = BigDecimal("1"),
            fiatRate = BigDecimal("3000"),
            contributions = listOf(stake(BigDecimal("0.5")), position(BigDecimal("2"), rate = BigDecimal.ZERO)),
        )
        val useCase = GetAccruingBalanceUseCase(createTestingCoroutineDispatcherProvider())

        // Act
        val emissions = useCase(status, appCurrency).toList()

        // Assert
        assertThat(emissions).isEmpty()
    }

    @Test
    fun `GIVEN amount not loaded WHEN invoke THEN emits nothing`() = runTest {
        // Arrange
        val status = CryptoCurrencyStatus(currency = coin, value = CryptoCurrencyStatus.Loading)
        val useCase = GetAccruingBalanceUseCase(createTestingCoroutineDispatcherProvider())

        // Act
        val emissions = useCase(status, appCurrency).toList()

        // Assert
        assertThat(emissions).isEmpty()
    }

    private fun loadedStatus(
        amount: BigDecimal,
        contributions: List<BalanceContribution>,
        fiatRate: BigDecimal = BigDecimal.ONE,
    ): CryptoCurrencyStatus {
        val status = MockYieldSupply.loadedStatus(
            currency = coin,
            amount = amount,
            fiatAmount = amount * fiatRate,
            fiatRate = fiatRate,
        )
        return status.copy(value = (status.value as CryptoCurrencyStatus.Loaded).copy(contributions = contributions))
    }

    private fun balance(
        crypto: String,
        fiatRate: BigDecimal?,
        fiatDecimals: Int = 6,
        cryptoDecimals: Int = 9,
    ) = YieldSupplyRewardBalance(
        fiatBalance = fiatRate?.let { rate ->
            BigDecimal(crypto).multiply(rate).format {
                fiat(appCurrency.code, appCurrency.symbol).anyDecimals(decimals = fiatDecimals)
            }
        },
        cryptoBalance = BigDecimal(crypto).format {
            crypto(coin).anyDecimals(maxDecimals = cryptoDecimals, minDecimals = cryptoDecimals)
        },
    )

    private fun stake(amount: BigDecimal): BalanceContribution = object : BalanceContribution {
        override val source: StatusSource = StatusSource.ACTUAL
        override fun totalDeltaCryptoAmount(): BigDecimal = amount
    }

    private fun position(amount: BigDecimal, rate: BigDecimal): AccruingBalanceContribution =
        object : AccruingBalanceContribution {
            override val source: StatusSource = StatusSource.ACTUAL
            override val accrualRate: BigDecimal = rate
            override fun totalDeltaCryptoAmount(): BigDecimal = amount
        }
}