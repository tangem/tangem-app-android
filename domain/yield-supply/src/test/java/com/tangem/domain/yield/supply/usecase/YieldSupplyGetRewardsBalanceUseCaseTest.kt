package com.tangem.domain.yield.supply.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.anyDecimals
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.domain.yield.supply.models.YieldMarketToken
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.ceil
import kotlin.math.ln

class YieldSupplyGetRewardsBalanceUseCaseTest {

    private val repository: YieldSupplyRepository = mockk(relaxed = true)

    @Test
    fun `GIVEN null amount WHEN invoke THEN emit nothing`() = runTest {
        val token = createToken(createNetwork())
        val status = CryptoCurrencyStatus(
            currency = token,
            value = CryptoCurrencyStatus.Loading,
        )

        val dispatcherProvider = testDispatcherProvider(this)
        val useCase = YieldSupplyGetRewardsBalanceUseCase(repository, dispatcherProvider)
        val appCurrency = AppCurrency.Default

        val emissions = useCase(status, appCurrency).toList()
        assertThat(emissions).isEmpty()
    }

    @Test
    fun `GIVEN coin currency WHEN invoke THEN emit nothing`() = runTest {
        val network = createNetwork()
        val coin = createNativeCoin(network)
        val status = CryptoCurrencyStatus(
            currency = coin,
            value = CryptoCurrencyStatus.Custom(
                amount = BigDecimal.ONE,
                fiatAmount = null,
                fiatRate = BigDecimal.ONE,
                priceChange = null,
                yieldBalance = null,
                yieldSupplyStatus = null,
                hasCurrentNetworkTransactions = false,
                pendingTransactions = emptySet(),
                networkAddress = NetworkAddress.Single(
                    NetworkAddress.Address(value = "0xabc", type = NetworkAddress.Address.Type.Primary),
                ),
                sources = CryptoCurrencyStatus.Sources(),
            ),
        )

        val dispatcherProvider = testDispatcherProvider(this)
        val useCase = YieldSupplyGetRewardsBalanceUseCase(repository, dispatcherProvider)
        val appCurrency = AppCurrency.Default

        val emissions = useCase(status, appCurrency).toList()
        assertThat(emissions).isEmpty()
    }

    @Test
    fun `GIVEN zero apy WHEN invoke THEN emit nothing`() = runTest {
        val network = createNetwork()
        val token = createToken(network)
        val amount = BigDecimal("123.45")
        val status = CryptoCurrencyStatus(
            currency = token,
            value = CryptoCurrencyStatus.Custom(
                amount = amount,
                fiatAmount = null,
                fiatRate = BigDecimal.ONE,
                priceChange = null,
                yieldBalance = null,
                yieldSupplyStatus = null,
                hasCurrentNetworkTransactions = false,
                pendingTransactions = emptySet(),
                networkAddress = NetworkAddress.Single(
                    NetworkAddress.Address(value = "0xabc", type = NetworkAddress.Address.Type.Primary),
                ),
                sources = CryptoCurrencyStatus.Sources(),
            ),
        )

        coEvery { repository.getCachedMarkets() } returns listOf(
            YieldMarketToken(
                tokenAddress = token.contractAddress,
                chainId = 1,
                apy = BigDecimal.ZERO,
                isActive = true,
                maxFeeNative = "0",
                maxFeeUSD = "0",
                backendId = "id",
            ),
        )

        val dispatcherProvider = testDispatcherProvider(this)
        val useCase = YieldSupplyGetRewardsBalanceUseCase(repository, dispatcherProvider)
        val appCurrency = AppCurrency.Default

        val emissions = useCase(status, appCurrency).toList()
        assertThat(emissions).isEmpty()
    }

    @Test
    fun `GIVEN positive apy WHEN invoke THEN emit growing formatted balances`() = runTest {
        val network = createNetwork()
        val token = createToken(network)
        val amount = BigDecimal("100.0")
        val apy = BigDecimal("12.0") // 12%

        val status = CryptoCurrencyStatus(
            currency = token,
            value = CryptoCurrencyStatus.Custom(
                amount = amount,
                fiatAmount = null,
                fiatRate = BigDecimal.ONE,
                priceChange = null,
                yieldBalance = null,
                yieldSupplyStatus = null,
                hasCurrentNetworkTransactions = false,
                pendingTransactions = emptySet(),
                networkAddress = NetworkAddress.Single(
                    NetworkAddress.Address(value = "0xabc", type = NetworkAddress.Address.Type.Primary),
                ),
                sources = CryptoCurrencyStatus.Sources(),
            ),
        )

        coEvery { repository.getCachedMarkets() } returns listOf(
            YieldMarketToken(
                tokenAddress = token.contractAddress,
                chainId = 1,
                apy = apy,
                isActive = true,
                maxFeeNative = "0",
                maxFeeUSD = "0",
                backendId = "id",
            ),
        )

        val dispatcherProvider = testDispatcherProvider(this)
        val useCase = YieldSupplyGetRewardsBalanceUseCase(repository, dispatcherProvider)
        val appCurrency = AppCurrency.Default

        val deferred = async { useCase(status, appCurrency).take(3).toList() }

        testScheduler.advanceUntilIdle()
        advanceTimeBy(300)
        testScheduler.advanceUntilIdle()
        advanceTimeBy(300)
        testScheduler.advanceUntilIdle()

        val collected = deferred.await()
        assertThat(collected).hasSize(3)

        val expectedDecimals = calculateMinVisibleDecimalsForTest(initialPerTickDelta(amount, apy))

        val firstExpected = amount.format { fiat(
            appCurrency.code,
            appCurrency.symbol,
        ).anyDecimals(decimals = expectedDecimals) }
        assertThat(collected[0]).isEqualTo(firstExpected)

        val firstNext = nextBalance(amount, apy)
        val secondExpected = firstNext.format { fiat(
            appCurrency.code,
            appCurrency.symbol,
        ).anyDecimals(decimals = expectedDecimals) }
        assertThat(collected[1]).isEqualTo(secondExpected)

        val secondNext = nextBalance(firstNext, apy)
        val thirdExpected = secondNext.format { fiat(
            appCurrency.code,
            appCurrency.symbol,
        ).anyDecimals(decimals = expectedDecimals) }
        assertThat(collected[2]).isEqualTo(thirdExpected)
    }

    private fun testDispatcherProvider(scope: TestScope): CoroutineDispatcherProvider {
        val dispatcher: CoroutineDispatcher = StandardTestDispatcher(scope.testScheduler)
        return object : CoroutineDispatcherProvider {
            override val main: CoroutineDispatcher = dispatcher
            override val mainImmediate: CoroutineDispatcher = dispatcher
            override val io: CoroutineDispatcher = dispatcher
            override val default: CoroutineDispatcher = dispatcher
            override val single: CoroutineDispatcher = dispatcher
        }
    }

    private fun createNetwork(): Network {
        val derivationPath = Network.DerivationPath.None
        return Network(
            id = Network.ID(Network.RawID("polygon"), derivationPath),
            backendId = "polygon",
            name = "Polygon",
            currencySymbol = "MATIC",
            derivationPath = derivationPath,
            isTestnet = false,
            standardType = Network.StandardType.ERC20,
            hasFiatFeeRate = false,
            canHandleTokens = true,
            transactionExtrasType = Network.TransactionExtrasType.NONE,
            nameResolvingType = Network.NameResolvingType.ENS,
        )
    }

    private fun createNativeCoin(network: Network): CryptoCurrency.Coin {
        val nativeCoinId = CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.COIN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkId(network.rawId),
            suffix = CryptoCurrency.ID.Suffix.RawID("polygon-ecosystem-token"),
        )
        return CryptoCurrency.Coin(
            id = nativeCoinId,
            network = network,
            name = "Polygon",
            symbol = "MATIC",
            decimals = 18,
            iconUrl = null,
            isCustom = false,
        )
    }

    private fun createToken(network: Network): CryptoCurrency.Token {
        val tokenId = CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkId(network.rawId),
            suffix = CryptoCurrency.ID.Suffix.RawID("test-token", "0xContract"),
        )
        return CryptoCurrency.Token(
            id = tokenId,
            network = network,
            name = "Test Token",
            symbol = "TT",
            decimals = 18,
            iconUrl = null,
            isCustom = false,
            contractAddress = "0xContract",
        )
    }

    private fun initialPerTickDelta(amount: BigDecimal, apy: BigDecimal): BigDecimal {
        val apyFraction = apy.divide(HUNDRED_BD, SCALE, RoundingMode.HALF_UP)
        return amount
            .multiply(apyFraction)
            .multiply(TICK_SECONDS_BD)
            .divide(SECONDS_PER_YEAR_BD, SCALE, RoundingMode.HALF_UP)
            .abs()
    }

    private fun nextBalance(current: BigDecimal, apy: BigDecimal): BigDecimal {
        val apyFraction = apy.divide(HUNDRED_BD, SCALE, RoundingMode.HALF_UP)
        val perTickDelta = current
            .multiply(apyFraction)
            .multiply(TICK_SECONDS_BD)
            .divide(SECONDS_PER_YEAR_BD, SCALE, RoundingMode.HALF_UP)
        return current.add(perTickDelta)
    }

    private fun calculateMinVisibleDecimalsForTest(perTickDeltaAbs: BigDecimal): Int {
        if (perTickDeltaAbs <= BigDecimal.ZERO) return MIN_DECIMALS
        val perTickAsDouble = perTickDeltaAbs.toDouble()
        if (perTickAsDouble.isNaN() || perTickAsDouble.isInfinite()) return MIN_DECIMALS
        val safe = if (perTickAsDouble <= 0.0) EPSILON else perTickAsDouble
        val raw = ceil(-ln(safe) / LN_10)
        return raw.toInt().coerceIn(MIN_DECIMALS, MAX_DECIMALS)
    }

    private companion object {
        private const val SCALE = 18
        private val TICK_SECONDS_BD = BigDecimal("0.3")
        private val SECONDS_PER_YEAR_BD = BigDecimal("31536000")
        private val HUNDRED_BD = BigDecimal("100")

        private const val MIN_DECIMALS = 3
        private const val MAX_DECIMALS = 8

        private val LN_10 = ln(10.0)
        private const val EPSILON = 1e-18
    }
}