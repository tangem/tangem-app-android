package com.tangem.domain.yield.supply.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.format.bigdecimal.anyDecimals
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.domain.yield.supply.models.YieldMarketToken
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetRewardsBalanceUseCase.Companion.TICK_MILLIS
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.ceil

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
    fun `GIVEN zero amount WHEN invoke THEN emit null balances`() = runTest {
        val network = createNetwork()
        val token = createToken(network)
        val status = CryptoCurrencyStatus(
            currency = token,
            value = CryptoCurrencyStatus.Custom(
                amount = BigDecimal.ZERO,
                fiatAmount = null,
                fiatRate = BigDecimal.ONE,
                priceChange = null,
                stakingBalance = null,
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
        assertThat(emissions).hasSize(1)
        assertThat(emissions[0].fiatBalance).isNull()
        assertThat(emissions[0].cryptoBalance).isNull()
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
                stakingBalance = null,
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
                stakingBalance = null,
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
                maxFeeNative = BigDecimal.ZERO,
                maxFeeUSD = BigDecimal.ZERO,
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
                stakingBalance = null,
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
                maxFeeNative = BigDecimal.ZERO,
                maxFeeUSD = BigDecimal.ZERO,
                backendId = "id",
            ),
        )

        val dispatcherProvider = testDispatcherProvider(this)
        val useCase = YieldSupplyGetRewardsBalanceUseCase(repository, dispatcherProvider)
        val appCurrency = AppCurrency.Default

        val deferred = async { useCase(status, appCurrency).take(3).toList() }

        testScheduler.advanceUntilIdle()
        advanceTimeBy(TICK_MILLIS)
        testScheduler.advanceUntilIdle()
        advanceTimeBy(TICK_MILLIS)
        testScheduler.advanceUntilIdle()

        val collected = deferred.await()
        assertThat(collected).hasSize(3)

        val expectedDecimals = calculateMinVisibleDecimalsForTest(initialPerTickDelta(amount, apy))

        val firstExpected = amount.format {
            fiat(
                appCurrency.code,
                appCurrency.symbol,
            ).anyDecimals(decimals = expectedDecimals)
        }
        assertThat(collected[0].fiatBalance).isEqualTo(firstExpected)

        val firstNext = nextBalance(amount, apy)
        val secondExpected = firstNext.format {
            fiat(
                appCurrency.code,
                appCurrency.symbol,
            ).anyDecimals(decimals = expectedDecimals)
        }
        assertThat(collected[1].fiatBalance).isEqualTo(secondExpected)

        val secondNext = nextBalance(firstNext, apy)
        val thirdExpected = secondNext.format {
            fiat(
                appCurrency.code,
                appCurrency.symbol,
            ).anyDecimals(decimals = expectedDecimals)
        }
        assertThat(collected[2].fiatBalance).isEqualTo(thirdExpected)
    }

    @Test
    fun `GIVEN polygon USDT0 Loaded status WHEN invoke THEN emit formatted balances`() = runTest {
        val network = Network(
            id = Network.ID(Network.RawID("POLYGON"), Network.DerivationPath.Card("m/44'/60'/0'/0/0")),
            backendId = "polygon-pos",
            name = "Polygon",
            currencySymbol = "POL",
            derivationPath = Network.DerivationPath.Card("m/44'/60'/0'/0/0"),
            isTestnet = false,
            standardType = Network.StandardType.Unspecified("Polygon"),
            hasFiatFeeRate = true,
            canHandleTokens = true,
            transactionExtrasType = Network.TransactionExtrasType.NONE,
            nameResolvingType = Network.NameResolvingType.NONE,
        )
        val tokenId = CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkId(network.rawId),
            suffix = CryptoCurrency.ID.Suffix.RawID("usdt0", "0xc2132d05d31c914a87c6611c10748aeb04b58e8f"),
        )
        val currency = CryptoCurrency.Token(
            id = tokenId,
            network = network,
            name = "USDT0",
            symbol = "USDT0",
            decimals = 6,
            iconUrl = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/usdt0.png",
            isCustom = false,
            contractAddress = "0xc2132d05d31c914a87c6611c10748aeb04b58e8f",
        )

        val amount = BigDecimal("9.241136")
        val fiatRate = BigDecimal("0.9999761277273864")
        val status = CryptoCurrencyStatus(
            currency = currency,
            value = CryptoCurrencyStatus.Loaded(
                amount = amount,
                fiatAmount = amount.multiply(fiatRate),
                fiatRate = fiatRate,
                priceChange = BigDecimal("-0.000058200000000008245"),
                stakingBalance = null,
                yieldSupplyStatus = null,
                hasCurrentNetworkTransactions = false,
                pendingTransactions = emptySet(),
                networkAddress = NetworkAddress.Single(
                    NetworkAddress.Address(
                        value = "0xb71fa0E20ba8579B3ec51cC79aaa84Bf5982BB49",
                        type = NetworkAddress.Address.Type.Primary,
                    ),
                ),
                sources = CryptoCurrencyStatus.Sources(),
            ),
        )

        val apy = BigDecimal("5.0")
        coEvery { repository.getCachedMarkets() } returns listOf(
            YieldMarketToken(
                tokenAddress = currency.contractAddress,
                chainId = 137,
                apy = apy,
                isActive = true,
                maxFeeNative = BigDecimal.ZERO,
                maxFeeUSD = BigDecimal.ZERO,
                backendId = "polygon-pos",
            ),
        )

        val dispatcherProvider = testDispatcherProvider(this)
        val useCase = YieldSupplyGetRewardsBalanceUseCase(repository, dispatcherProvider)
        val appCurrency = AppCurrency.Default

        val deferred = async { useCase(status, appCurrency).take(2).toList() }

        testScheduler.advanceUntilIdle()
        advanceTimeBy(TICK_MILLIS)
        testScheduler.advanceUntilIdle()

        val emissions = deferred.await()
        assertThat(emissions).hasSize(2)

        val apyFraction = apy.divide(BigDecimal("100"), 18, RoundingMode.HALF_UP)
        val perTickCrypto = amount.multiply(apyFraction)
            .multiply(YieldSupplyGetRewardsBalanceUseCase.TICK_SECONDS_BD)
            .divide(
                YieldSupplyGetRewardsBalanceUseCase.SECONDS_PER_YEAR_BD,
                YieldSupplyGetRewardsBalanceUseCase.SCALE,
                RoundingMode.HALF_UP,
            )
            .abs()
        val minCryptoDecimals = calculateMinVisibleDecimalsForTest(perTickCrypto).coerceAtMost(currency.decimals)

        val fiatAmountStart = amount.multiply(fiatRate)
        val perTickFiat = fiatAmountStart.multiply(apyFraction)
            .multiply(YieldSupplyGetRewardsBalanceUseCase.TICK_SECONDS_BD)
            .divide(
                YieldSupplyGetRewardsBalanceUseCase.SECONDS_PER_YEAR_BD,
                YieldSupplyGetRewardsBalanceUseCase.SCALE,
                RoundingMode.HALF_UP,
            )
            .abs()
        val minFiatDecimals = calculateMinVisibleDecimalsForTest(perTickFiat)

        val expectedCrypto0 = amount.format {
            crypto(currency).anyDecimals(
                maxDecimals = minCryptoDecimals,
                minDecimals = minCryptoDecimals,
            )
        }
        val expectedFiat0 = fiatAmountStart.format {
            fiat(appCurrency.code, appCurrency.symbol).anyDecimals(decimals = minFiatDecimals)
        }

        assertThat(emissions[0].cryptoBalance).isEqualTo(expectedCrypto0)
        assertThat(emissions[0].fiatBalance).isEqualTo(expectedFiat0)
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
        val apyFraction = apy.divide(
            YieldSupplyGetRewardsBalanceUseCase.HUNDRED_BD,
            YieldSupplyGetRewardsBalanceUseCase.SCALE,
            RoundingMode.HALF_UP,
        )
        return amount
            .multiply(apyFraction)
            .multiply(YieldSupplyGetRewardsBalanceUseCase.TICK_SECONDS_BD)
            .divide(
                YieldSupplyGetRewardsBalanceUseCase.SECONDS_PER_YEAR_BD,
                YieldSupplyGetRewardsBalanceUseCase.SCALE,
                RoundingMode.HALF_UP,
            )
            .abs()
    }

    private fun nextBalance(current: BigDecimal, apy: BigDecimal): BigDecimal {
        val apyFraction = apy.divide(
            YieldSupplyGetRewardsBalanceUseCase.HUNDRED_BD,
            YieldSupplyGetRewardsBalanceUseCase.SCALE,
            RoundingMode.HALF_UP,
        )
        val perTickDelta = current
            .multiply(apyFraction)
            .multiply(YieldSupplyGetRewardsBalanceUseCase.TICK_SECONDS_BD)
            .divide(
                YieldSupplyGetRewardsBalanceUseCase.SECONDS_PER_YEAR_BD,
                YieldSupplyGetRewardsBalanceUseCase.SCALE,
                RoundingMode.HALF_UP,
            )
        return current.add(perTickDelta)
    }

    private fun calculateMinVisibleDecimalsForTest(perTickDeltaAbs: BigDecimal): Int {
        if (perTickDeltaAbs <= BigDecimal.ZERO) return YieldSupplyGetRewardsBalanceUseCase.MIN_DECIMALS
        val perTickAsDouble = perTickDeltaAbs.toDouble()
        if (perTickAsDouble.isNaN() || perTickAsDouble.isInfinite()) return YieldSupplyGetRewardsBalanceUseCase.MIN_DECIMALS
        val safe = if (perTickAsDouble <= 0.0) YieldSupplyGetRewardsBalanceUseCase.EPSILON else perTickAsDouble
        val raw = ceil(-kotlin.math.ln(safe) / YieldSupplyGetRewardsBalanceUseCase.LN_10)
        return raw.toInt().coerceIn(
            YieldSupplyGetRewardsBalanceUseCase.MIN_DECIMALS,
            YieldSupplyGetRewardsBalanceUseCase.FIAT_MAX_DECIMALS,
        )
    }

    @Test
    fun `GIVEN token with decimals less than MIN_DECIMALS WHEN invoke THEN does not crash`() = runTest {
        val network = createNetwork()
        val tokenId = CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkId(network.rawId),
            suffix = CryptoCurrency.ID.Suffix.RawID("low-decimals-token", "0xLowDecimals"),
        )
        val tokenWithLowDecimals = CryptoCurrency.Token(
            id = tokenId,
            network = network,
            name = "Low Decimals Token",
            symbol = "LDT",
            decimals = 0,
            iconUrl = null,
            isCustom = false,
            contractAddress = "0xLowDecimals",
        )

        val amount = BigDecimal("100.00")
        val apy = BigDecimal("10.0")

        val status = CryptoCurrencyStatus(
            currency = tokenWithLowDecimals,
            value = CryptoCurrencyStatus.Custom(
                amount = amount,
                fiatAmount = null,
                fiatRate = BigDecimal.ONE,
                priceChange = null,
                stakingBalance = null,
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
                tokenAddress = tokenWithLowDecimals.contractAddress,
                chainId = 1,
                apy = apy,
                isActive = true,
                maxFeeNative = BigDecimal.ZERO,
                maxFeeUSD = BigDecimal.ZERO,
                backendId = "id",
            ),
        )

        val dispatcherProvider = testDispatcherProvider(this)
        val useCase = YieldSupplyGetRewardsBalanceUseCase(repository, dispatcherProvider)
        val appCurrency = AppCurrency.Default

        val deferred = async { useCase(status, appCurrency).take(2).toList() }

        testScheduler.advanceUntilIdle()
        advanceTimeBy(TICK_MILLIS)
        testScheduler.advanceUntilIdle()

        val emissions = deferred.await()
        assertThat(emissions).hasSize(2)
        assertThat(emissions[0].cryptoBalance).isNotNull()
        assertThat(emissions[1].cryptoBalance).isNotNull()
    }
}