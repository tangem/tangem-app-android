package com.tangem.domain.yield.supply.usecase

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.quotes.QuotesRepository
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.domain.yield.supply.models.YieldMarketToken
import com.tangem.domain.yield.supply.models.YieldSupplyMaxFee
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.RoundingMode

@OptIn(ExperimentalCoroutinesApi::class)
internal class YieldSupplyGetMaxFeeUseCaseTest {

    private val yieldSupplyRepository: YieldSupplyRepository = mockk()
    private val quotesRepository: QuotesRepository = mockk()
    private val singleAccountListSupplier: SingleAccountListSupplier = mockk()

    private val useCase = YieldSupplyGetMaxFeeUseCase(
        yieldSupplyRepository = yieldSupplyRepository,
        quotesRepository = quotesRepository,
        singleAccountListSupplier = singleAccountListSupplier,
    )

    private val userWalletId = UserWalletId("abcdef012345")

    @BeforeEach
    fun setUp() {
        clearMocks(yieldSupplyRepository, quotesRepository, singleAccountListSupplier)
    }

    @Test
    fun `GIVEN cached market token WHEN invoke THEN converts and HALF_UP-rounds the fee to token and fiat`() =
        runTest {
            // Arrange — values chosen to pin the formula AND the rounding mode with literal expectations:
            //   fiatMaxFee = maxFeeNative(0.0002) * nativeFiatRate(1000) = 0.2
            //   tokenMaxFee = 0.2 / tokenFiatRate(3) = 0.066666… → 0.066667 at 6 decimals (HALF_UP; HALF_DOWN = 0.066666)
            val token = createToken(rawNetworkId = NETWORK_ID, decimals = 6)
            val nativeCoin = createCoin(rawNetworkId = NETWORK_ID, decimals = 18)
            val cryptoStatus = createTokenStatus(token = token, fiatRate = BigDecimal("3"))

            stubAccountList(token, nativeCoin)
            stubNativeQuote(nativeCoin, fiatRate = BigDecimal("1000"))
            coEvery { yieldSupplyRepository.getCachedMarkets() } returns listOf(
                createMarketToken(token = token, maxFeeNative = BigDecimal("0.0002")),
            )

            // Act
            val result = useCase(userWalletId, cryptoStatus)

            // Assert — literal expectations, not a mirror of the production expression
            assertThat(result).isEqualTo(
                Either.Right(
                    YieldSupplyMaxFee(
                        nativeMaxFee = BigDecimal("0.0002"),
                        tokenMaxFee = BigDecimal("0.066667"),
                        fiatMaxFee = BigDecimal("0.2"),
                    ),
                ),
            )
            coVerify(exactly = 0) { yieldSupplyRepository.getTokenStatus(any()) }
        }

    @Test
    fun `GIVEN no matching cached token WHEN invoke THEN falls back to fetching token status`() = runTest {
        // Arrange
        val token = createToken(rawNetworkId = NETWORK_ID, decimals = 6)
        val nativeCoin = createCoin(rawNetworkId = NETWORK_ID, decimals = 18)
        val cryptoStatus = createTokenStatus(token = token, fiatRate = BigDecimal("1.00"))
        val nativeFiatRate = BigDecimal("2000.00")
        val maxFeeNative = BigDecimal("0.005")

        stubAccountList(token, nativeCoin)
        stubNativeQuote(nativeCoin, nativeFiatRate)
        coEvery { yieldSupplyRepository.getCachedMarkets() } returns emptyList()
        coEvery { yieldSupplyRepository.getTokenStatus(token) } returns createMarketToken(
            token = token,
            maxFeeNative = maxFeeNative,
        )

        val fiatMaxFee = maxFeeNative.multiply(nativeFiatRate)
        val expected = YieldSupplyMaxFee(
            nativeMaxFee = maxFeeNative,
            tokenMaxFee = fiatMaxFee.divide(cryptoStatus.value.fiatRate, token.decimals, RoundingMode.HALF_UP),
            fiatMaxFee = fiatMaxFee.stripTrailingZeros(),
        )

        // Act
        val result = useCase(userWalletId, cryptoStatus)

        // Assert
        assertThat(result).isEqualTo(Either.Right(expected))
        coVerify(exactly = 1) { yieldSupplyRepository.getTokenStatus(token) }
    }

    @Test
    fun `GIVEN null cached markets WHEN invoke THEN falls back to fetching token status`() = runTest {
        // Arrange
        val token = createToken(rawNetworkId = NETWORK_ID, decimals = 6)
        val nativeCoin = createCoin(rawNetworkId = NETWORK_ID, decimals = 18)
        val cryptoStatus = createTokenStatus(token = token, fiatRate = BigDecimal("1.00"))
        val nativeFiatRate = BigDecimal("2000.00")
        val maxFeeNative = BigDecimal("0.005")

        stubAccountList(token, nativeCoin)
        stubNativeQuote(nativeCoin, nativeFiatRate)
        coEvery { yieldSupplyRepository.getCachedMarkets() } returns null
        coEvery { yieldSupplyRepository.getTokenStatus(token) } returns createMarketToken(
            token = token,
            maxFeeNative = maxFeeNative,
        )

        val fiatMaxFee = maxFeeNative.multiply(nativeFiatRate)
        val expected = YieldSupplyMaxFee(
            nativeMaxFee = maxFeeNative,
            tokenMaxFee = fiatMaxFee.divide(cryptoStatus.value.fiatRate, token.decimals, RoundingMode.HALF_UP),
            fiatMaxFee = fiatMaxFee.stripTrailingZeros(),
        )

        // Act
        val result = useCase(userWalletId, cryptoStatus)

        // Assert
        assertThat(result).isEqualTo(Either.Right(expected))
        coVerify(exactly = 1) { yieldSupplyRepository.getTokenStatus(token) }
    }

    @Test
    fun `GIVEN currency is not a token WHEN invoke THEN returns error`() = runTest {
        // Arrange
        val coinStatus = createCoinStatus(createCoin(rawNetworkId = NETWORK_ID, decimals = 18))

        // Act
        val result = useCase(userWalletId, coinStatus)

        // Assert
        assertLeftWithMessage(result, "CryptoCurrency must be token for max fee calculation")
    }

    @Test
    fun `GIVEN token fiat rate missing WHEN invoke THEN returns error`() = runTest {
        // Arrange
        val token = createToken(rawNetworkId = NETWORK_ID, decimals = 6)
        val cryptoStatus = createTokenStatus(token = token, fiatRate = null)

        // Act
        val result = useCase(userWalletId, cryptoStatus)

        // Assert
        assertLeftWithMessage(result, "Fiat rate is missing")
    }

    @Test
    fun `GIVEN token fiat rate non-positive WHEN invoke THEN returns error`() = runTest {
        // Arrange
        val token = createToken(rawNetworkId = NETWORK_ID, decimals = 6)
        val cryptoStatus = createTokenStatus(token = token, fiatRate = BigDecimal.ZERO)

        // Act
        val result = useCase(userWalletId, cryptoStatus)

        // Assert
        assertLeftWithMessage(result, "Fiat rate for token must be > 0")
    }

    @Test
    fun `GIVEN account status list missing WHEN invoke THEN returns error`() = runTest {
        // Arrange
        val token = createToken(rawNetworkId = NETWORK_ID, decimals = 6)
        val cryptoStatus = createTokenStatus(token = token, fiatRate = BigDecimal("1.00"))
        coEvery { singleAccountListSupplier.getSyncOrNull(userWalletId = userWalletId) } returns null

        // Act
        val result = useCase(userWalletId, cryptoStatus)

        // Assert
        assertLeftStartingWith(result, "Account status list is missing")
    }

    @Test
    fun `GIVEN native coin not found in account list WHEN invoke THEN returns error`() = runTest {
        // Arrange
        val token = createToken(rawNetworkId = NETWORK_ID, decimals = 6)
        val cryptoStatus = createTokenStatus(token = token, fiatRate = BigDecimal("1.00"))
        coEvery {
            singleAccountListSupplier.getSyncOrNull(userWalletId = userWalletId)
        } returns AccountList.empty(userWalletId = userWalletId, cryptoCurrencies = listOf(token))

        // Act
        val result = useCase(userWalletId, cryptoStatus)

        // Assert
        assertLeftStartingWith(result, "Unable to find coin for network ID")
    }

    @Test
    fun `GIVEN native quotes unavailable WHEN invoke THEN returns error`() = runTest {
        // Arrange
        val token = createToken(rawNetworkId = NETWORK_ID, decimals = 6)
        val nativeCoin = createCoin(rawNetworkId = NETWORK_ID, decimals = 18)
        val cryptoStatus = createTokenStatus(token = token, fiatRate = BigDecimal("1.00"))
        stubAccountList(token, nativeCoin)
        coEvery {
            quotesRepository.getMultiQuoteSyncOrNull(setOf(nativeCoin.id.rawCurrencyId!!))
        } returns null

        // Act
        val result = useCase(userWalletId, cryptoStatus)

        // Assert
        assertLeftWithMessage(result, "Quotes for native coin are unavailable")
    }

    @Test
    fun `GIVEN empty native quotes list WHEN invoke THEN returns error`() = runTest {
        // Arrange
        val token = createToken(rawNetworkId = NETWORK_ID, decimals = 6)
        val nativeCoin = createCoin(rawNetworkId = NETWORK_ID, decimals = 18)
        val cryptoStatus = createTokenStatus(token = token, fiatRate = BigDecimal("1.00"))
        stubAccountList(token, nativeCoin)
        coEvery {
            quotesRepository.getMultiQuoteSyncOrNull(setOf(nativeCoin.id.rawCurrencyId!!))
        } returns emptySet()

        // Act
        val result = useCase(userWalletId, cryptoStatus)

        // Assert
        assertLeftWithMessage(result, "Empty quotes list for native coin")
    }

    @Test
    fun `GIVEN native quote has no fiat rate WHEN invoke THEN returns error`() = runTest {
        // Arrange
        val token = createToken(rawNetworkId = NETWORK_ID, decimals = 6)
        val nativeCoin = createCoin(rawNetworkId = NETWORK_ID, decimals = 18)
        val cryptoStatus = createTokenStatus(token = token, fiatRate = BigDecimal("1.00"))
        stubAccountList(token, nativeCoin)
        coEvery {
            quotesRepository.getMultiQuoteSyncOrNull(setOf(nativeCoin.id.rawCurrencyId!!))
        } returns setOf(QuoteStatus(rawCurrencyId = nativeCoin.id.rawCurrencyId!!))

        // Act
        val result = useCase(userWalletId, cryptoStatus)

        // Assert
        assertLeftWithMessage(result, "Native fiat rate is missing")
    }

    @Test
    fun `GIVEN native fiat rate non-positive WHEN invoke THEN returns error`() = runTest {
        // Arrange
        val token = createToken(rawNetworkId = NETWORK_ID, decimals = 6)
        val nativeCoin = createCoin(rawNetworkId = NETWORK_ID, decimals = 18)
        val cryptoStatus = createTokenStatus(token = token, fiatRate = BigDecimal("1.00"))
        stubAccountList(token, nativeCoin)
        stubNativeQuote(nativeCoin, fiatRate = BigDecimal.ZERO)

        // Act
        val result = useCase(userWalletId, cryptoStatus)

        // Assert
        assertLeftWithMessage(result, "Native fiat rate must be > 0")
    }

    // region Helpers

    private fun stubAccountList(token: CryptoCurrency.Token, nativeCoin: CryptoCurrency.Coin) {
        coEvery {
            singleAccountListSupplier.getSyncOrNull(userWalletId = userWalletId)
        } returns AccountList.empty(userWalletId = userWalletId, cryptoCurrencies = listOf(nativeCoin, token))
    }

    private fun stubNativeQuote(nativeCoin: CryptoCurrency.Coin, fiatRate: BigDecimal) {
        coEvery {
            quotesRepository.getMultiQuoteSyncOrNull(setOf(nativeCoin.id.rawCurrencyId!!))
        } returns setOf(
            QuoteStatus(
                rawCurrencyId = nativeCoin.id.rawCurrencyId!!,
                value = QuoteStatus.Data(
                    source = StatusSource.ACTUAL,
                    fiatRate = fiatRate,
                    fiatRateUSD = fiatRate,
                    priceChange = BigDecimal.ZERO,
                ),
            ),
        )
    }

    private fun assertLeftWithMessage(result: Either<Throwable, YieldSupplyMaxFee>, message: String) {
        assertThat(result.isLeft()).isTrue()
        assertThat((result as Either.Left).value.message).isEqualTo(message)
    }

    private fun assertLeftStartingWith(result: Either<Throwable, YieldSupplyMaxFee>, prefix: String) {
        assertThat(result.isLeft()).isTrue()
        assertThat((result as Either.Left).value.message).startsWith(prefix)
    }

    private fun createMarketToken(token: CryptoCurrency.Token, maxFeeNative: BigDecimal): YieldMarketToken =
        YieldMarketToken(
            tokenAddress = token.contractAddress,
            chainId = 1,
            apy = BigDecimal.ZERO,
            isActive = true,
            maxFeeNative = maxFeeNative,
            maxFeeUSD = BigDecimal.ZERO,
            backendId = token.network.rawId,
        )

    private fun createToken(rawNetworkId: String, decimals: Int): CryptoCurrency.Token {
        return CryptoCurrency.Token(
            id = CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkId(rawNetworkId),
                suffix = CryptoCurrency.ID.Suffix.RawID(rawNetworkId),
            ),
            network = createNetwork(rawNetworkId),
            name = "TEST_TOKEN",
            symbol = "TTK",
            decimals = decimals,
            iconUrl = null,
            isCustom = false,
            contractAddress = "0xToken",
        )
    }

    private fun createCoin(rawNetworkId: String, decimals: Int): CryptoCurrency.Coin {
        return CryptoCurrency.Coin(
            id = CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.COIN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkId(rawNetworkId),
                suffix = CryptoCurrency.ID.Suffix.RawID(rawNetworkId),
            ),
            network = createNetwork(rawNetworkId),
            name = "TEST_COIN",
            symbol = "TCN",
            decimals = decimals,
            iconUrl = null,
            isCustom = false,
        )
    }

    private fun createNetwork(rawNetworkId: String): Network {
        val derivationPath = Network.DerivationPath.None
        return Network(
            id = Network.ID(value = rawNetworkId, derivationPath = derivationPath),
            name = rawNetworkId,
            currencySymbol = rawNetworkId.take(3).uppercase(),
            derivationPath = derivationPath,
            isTestnet = false,
            standardType = Network.StandardType.Unspecified("UNSPECIFIED"),
            hasFiatFeeRate = true,
            canHandleTokens = true,
            transactionExtrasType = Network.TransactionExtrasType.NONE,
            nameResolvingType = Network.NameResolvingType.NONE,
        )
    }

    private fun createTokenStatus(token: CryptoCurrency.Token, fiatRate: BigDecimal?): CryptoCurrencyStatus =
        CryptoCurrencyStatus(currency = token, value = customValue(fiatRate))

    private fun createCoinStatus(coin: CryptoCurrency.Coin): CryptoCurrencyStatus =
        CryptoCurrencyStatus(currency = coin, value = customValue(BigDecimal.ONE))

    private fun customValue(fiatRate: BigDecimal?): CryptoCurrencyStatus.Custom = CryptoCurrencyStatus.Custom(
        amount = BigDecimal.ZERO,
        fiatAmount = BigDecimal.ZERO,
        fiatRate = fiatRate,
        priceChange = BigDecimal.ZERO,
        stakingBalance = null,
        yieldSupplyStatus = null,
        hasCurrentNetworkTransactions = false,
        pendingTransactions = emptySet(),
        networkAddress = NetworkAddress.Single(
            defaultAddress = NetworkAddress.Address(
                value = "0x0000000000000000000000000000000000000000",
                type = NetworkAddress.Address.Type.Primary,
            ),
        ),
        sources = CryptoCurrencyStatus.Sources(),
    )

    // endregion

    private companion object {
        const val NETWORK_ID = "ethereum"
    }
}