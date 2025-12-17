package com.tangem.domain.yield.supply.usecase

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.quotes.QuotesRepository
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.transaction.FeeRepository
import com.tangem.domain.utils.convertToSdkAmount
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

@OptIn(ExperimentalCoroutinesApi::class)
class YieldSupplyGetCurrentFeeUseCaseTest {

    private val feeRepository: FeeRepository = mockk()
    private val quotesRepository: QuotesRepository = mockk()
    private val currenciesRepository: CurrenciesRepository = mockk()

    private lateinit var useCase: YieldSupplyGetCurrentFeeUseCase

    private val userWalletId = UserWalletId("abcdef012345")

    @BeforeEach
    fun setUp() {
        useCase = YieldSupplyGetCurrentFeeUseCase(
            feeRepository = feeRepository,
            quotesRepository = quotesRepository,
            currenciesRepository = currenciesRepository,
        )
    }

    @Test
    fun `GIVEN valid inputs on non-ethereum WHEN invoke THEN returns fee value and not high`() = runTest {
        val rawNetworkId = Blockchain.BSC.id
        val tokenDecimals = 8
        val nativeDecimals = 18
        val token = createToken(rawNetworkId = rawNetworkId, decimals = tokenDecimals)
        val cryptoStatus = createStatus(token = token, fiatRate = BigDecimal("2.00"))
        val nativeCoin = createCoin(rawNetworkId = rawNetworkId, decimals = nativeDecimals)
        val nativeFiatRate = BigDecimal("4.00")

        val maxFeePerGas = BigInteger.valueOf(1_000_000_000L)
        val feeWithoutGas = Fee.Ethereum.EIP1559(
            maxFeePerGas = maxFeePerGas,
            priorityFee = BigInteger.ONE,
            gasLimit = BigInteger.ZERO,
            amount = BigDecimal.ZERO.convertToSdkAmount(cryptoStatus),
        )

        coEvery { feeRepository.getEthereumFeeWithoutGas(userWalletId, token) } returns feeWithoutGas
        coEvery {
            currenciesRepository.getNetworkCoin(
                userWalletId = userWalletId,
                networkId = token.network.id,
                derivationPath = token.network.derivationPath,
            )
        } returns nativeCoin
        coEvery {
            quotesRepository.getMultiQuoteSyncOrNull(setOf(nativeCoin.id.rawCurrencyId!!))
        } returns setOf(
            QuoteStatus(
                rawCurrencyId = nativeCoin.id.rawCurrencyId!!,
                value = QuoteStatus.Data(
                    source = StatusSource.ACTUAL,
                    fiatRate = nativeFiatRate,
                    priceChange = BigDecimal.ZERO,
                ),
            ),
        )

        val gasLimit = BigInteger.valueOf(350_000)
        val nativeGasValue = maxFeePerGas.multiply(gasLimit).toBigDecimal().movePointLeft(nativeDecimals)
        val rateRatio = nativeFiatRate.divide(cryptoStatus.value.fiatRate!!, tokenDecimals, RoundingMode.HALF_UP)
        val expectedTokenValue = rateRatio.multiply(nativeGasValue).stripTrailingZeros()

        val result = useCase(userWalletId, cryptoStatus)

        assertThat(result.isRight()).isTrue()
        val fee = (result as Either.Right).value
        assertThat(fee.value).isEqualTo(expectedTokenValue)
        assertThat(fee.isHighFee).isFalse()
    }

    @Test
    fun `GIVEN ethereum with high gas WHEN invoke THEN returns high fee flag`() = runTest {
        val rawNetworkId = Blockchain.Ethereum.id
        val tokenDecimals = 8
        val nativeDecimals = 18
        val token = createToken(rawNetworkId = rawNetworkId, decimals = tokenDecimals)
        val cryptoStatus = createStatus(token = token, fiatRate = BigDecimal("2.00"))
        val nativeCoin = createCoin(rawNetworkId = rawNetworkId, decimals = nativeDecimals)
        val nativeFiatRate = BigDecimal("4.00")

        val maxFeePerGas = BigInteger.valueOf(400_000_000L) // threshold
        val feeWithoutGas = Fee.Ethereum.EIP1559(
            maxFeePerGas = maxFeePerGas,
            priorityFee = BigInteger.ONE,
            gasLimit = BigInteger.ZERO,
            amount = BigDecimal.ZERO.convertToSdkAmount(cryptoStatus),
        )

        coEvery { feeRepository.getEthereumFeeWithoutGas(userWalletId, token) } returns feeWithoutGas
        coEvery {
            currenciesRepository.getNetworkCoin(
                userWalletId = userWalletId,
                networkId = token.network.id,
                derivationPath = token.network.derivationPath,
            )
        } returns nativeCoin
        coEvery {
            quotesRepository.getMultiQuoteSyncOrNull(setOf(nativeCoin.id.rawCurrencyId!!))
        } returns setOf(
            QuoteStatus(
                rawCurrencyId = nativeCoin.id.rawCurrencyId!!,
                value = QuoteStatus.Data(
                    source = StatusSource.ACTUAL,
                    fiatRate = nativeFiatRate,
                    priceChange = BigDecimal.ZERO,
                ),
            ),
        )

        val gasLimit = BigInteger.valueOf(350_000)
        val nativeGasValue = maxFeePerGas.multiply(gasLimit).toBigDecimal().movePointLeft(nativeDecimals)
        val rateRatio = nativeFiatRate.divide(cryptoStatus.value.fiatRate!!, tokenDecimals, RoundingMode.HALF_UP)
        val expectedTokenValue = rateRatio.multiply(nativeGasValue).stripTrailingZeros()

        val result = useCase(userWalletId, cryptoStatus)

        assertThat(result.isRight()).isTrue()
        val fee = (result as Either.Right).value
        assertThat(fee.value).isEqualTo(expectedTokenValue)
        assertThat(fee.isHighFee).isTrue()
    }

    @Test
    fun `GIVEN token fiat rate missing WHEN invoke THEN returns error`() = runTest {
        val rawNetworkId = Blockchain.BSC.id
        val token = createToken(rawNetworkId = rawNetworkId, decimals = 8)
        val cryptoStatus = createStatus(token = token, fiatRate = null)

        coEvery { feeRepository.getEthereumFeeWithoutGas(userWalletId, token) } returns Fee.Ethereum.EIP1559(
            maxFeePerGas = BigInteger.ONE,
            priorityFee = BigInteger.ONE,
            gasLimit = BigInteger.ZERO,
            amount = BigDecimal.ZERO.convertToSdkAmount(
                createStatus(token = token, fiatRate = BigDecimal.ONE),
            ),
        )

        val result = useCase(userWalletId, cryptoStatus)

        assertThat(result.isLeft()).isTrue()
        val error = (result as Either.Left).value
        assertThat(error).isInstanceOf(IllegalStateException::class.java)
        assertThat(error.message).isEqualTo("Fiat rate is missing")
    }

    @Test
    fun `GIVEN native quotes unavailable WHEN invoke THEN returns error`() = runTest {
        val rawNetworkId = Blockchain.BSC.id
        val token = createToken(rawNetworkId = rawNetworkId, decimals = 8)
        val cryptoStatus = createStatus(token = token, fiatRate = BigDecimal("2.00"))
        val nativeCoin = createCoin(rawNetworkId = rawNetworkId, decimals = 18)

        coEvery { feeRepository.getEthereumFeeWithoutGas(userWalletId, token) } returns Fee.Ethereum.EIP1559(
            maxFeePerGas = BigInteger.valueOf(1_000_000_000L),
            priorityFee = BigInteger.ONE,
            gasLimit = BigInteger.ZERO,
            amount = BigDecimal.ZERO.convertToSdkAmount(cryptoStatus),
        )
        coEvery {
            currenciesRepository.getNetworkCoin(
                userWalletId = userWalletId,
                networkId = token.network.id,
                derivationPath = token.network.derivationPath,
            )
        } returns nativeCoin
        coEvery { quotesRepository.getMultiQuoteSyncOrNull(setOf(nativeCoin.id.rawCurrencyId!!)) } returns null

        val result = useCase(userWalletId, cryptoStatus)

        assertThat(result.isLeft()).isTrue()
        val error = (result as Either.Left).value
        assertThat(error).isInstanceOf(IllegalStateException::class.java)
        assertThat(error.message).isEqualTo("Quotes for native coin are unavailable")
    }

    @Test
    fun `GIVEN empty quotes list WHEN invoke THEN returns error`() = runTest {
        val rawNetworkId = Blockchain.BSC.id
        val token = createToken(rawNetworkId = rawNetworkId, decimals = 8)
        val cryptoStatus = createStatus(token = token, fiatRate = BigDecimal("2.00"))
        val nativeCoin = createCoin(rawNetworkId = rawNetworkId, decimals = 18)

        coEvery { feeRepository.getEthereumFeeWithoutGas(userWalletId, token) } returns Fee.Ethereum.EIP1559(
            maxFeePerGas = BigInteger.valueOf(1_000_000_000L),
            priorityFee = BigInteger.ONE,
            gasLimit = BigInteger.ZERO,
            amount = BigDecimal.ZERO.convertToSdkAmount(cryptoStatus),
        )
        coEvery {
            currenciesRepository.getNetworkCoin(
                userWalletId = userWalletId,
                networkId = token.network.id,
                derivationPath = token.network.derivationPath,
            )
        } returns nativeCoin
        coEvery { quotesRepository.getMultiQuoteSyncOrNull(setOf(nativeCoin.id.rawCurrencyId!!)) } returns emptySet()

        val result = useCase(userWalletId, cryptoStatus)

        assertThat(result.isLeft()).isTrue()
        val error = (result as Either.Left).value
        assertThat(error).isInstanceOf(IllegalStateException::class.java)
        assertThat(error.message).isEqualTo("Empty quotes list for native coin")
    }

    @Test
    fun `GIVEN native fiat rate non-positive WHEN invoke THEN returns error`() = runTest {
        val rawNetworkId = Blockchain.BSC.id
        val token = createToken(rawNetworkId = rawNetworkId, decimals = 8)
        val cryptoStatus = createStatus(token = token, fiatRate = BigDecimal("2.00"))
        val nativeCoin = createCoin(rawNetworkId = rawNetworkId, decimals = 18)

        coEvery { feeRepository.getEthereumFeeWithoutGas(userWalletId, token) } returns Fee.Ethereum.EIP1559(
            maxFeePerGas = BigInteger.valueOf(1_000_000_000L),
            priorityFee = BigInteger.ONE,
            gasLimit = BigInteger.ZERO,
            amount = BigDecimal.ZERO.convertToSdkAmount(cryptoStatus),
        )
        coEvery {
            currenciesRepository.getNetworkCoin(
                userWalletId = userWalletId,
                networkId = token.network.id,
                derivationPath = token.network.derivationPath,
            )
        } returns nativeCoin
        coEvery {
            quotesRepository.getMultiQuoteSyncOrNull(setOf(nativeCoin.id.rawCurrencyId!!))
        } returns setOf(
            QuoteStatus(
                rawCurrencyId = nativeCoin.id.rawCurrencyId!!,
                value = QuoteStatus.Data(
                    source = StatusSource.ACTUAL,
                    fiatRate = BigDecimal.ZERO, // non-positive
                    priceChange = BigDecimal.ZERO,
                ),
            ),
        )

        val result = useCase(userWalletId, cryptoStatus)

        assertThat(result.isLeft()).isTrue()
        val error = (result as Either.Left).value
        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(error.message).isEqualTo("Native fiat rate must be > 0")
    }

    @Test
    fun `GIVEN token fiat rate non-positive WHEN invoke THEN returns error`() = runTest {
        val rawNetworkId = Blockchain.BSC.id
        val token = createToken(rawNetworkId = rawNetworkId, decimals = 8)
        val cryptoStatus = createStatus(token = token, fiatRate = BigDecimal.ZERO) // non-positive

        coEvery { feeRepository.getEthereumFeeWithoutGas(userWalletId, token) } returns Fee.Ethereum.EIP1559(
            maxFeePerGas = BigInteger.ONE,
            priorityFee = BigInteger.ONE,
            gasLimit = BigInteger.ZERO,
            amount = BigDecimal.ZERO.convertToSdkAmount(
                createStatus(token = token, fiatRate = BigDecimal.ONE),
            ),
        )

        val result = useCase(userWalletId, cryptoStatus)

        assertThat(result.isLeft()).isTrue()
        val error = (result as Either.Left).value
        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(error.message).isEqualTo("Fiat rate for token must be > 0")
    }

    private fun createToken(rawNetworkId: String, decimals: Int): CryptoCurrency.Token {
        val derivationPath = Network.DerivationPath.None
        val network = Network(
            id = Network.ID(value = rawNetworkId, derivationPath = derivationPath),
            backendId = rawNetworkId,
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

        return CryptoCurrency.Token(
            id = CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkId(rawNetworkId),
                suffix = CryptoCurrency.ID.Suffix.RawID(rawNetworkId),
            ),
            network = network,
            name = "TEST_TOKEN",
            symbol = "TTK",
            decimals = decimals,
            iconUrl = null,
            isCustom = false,
            contractAddress = "0xToken",
        )
    }

    private fun createCoin(rawNetworkId: String, decimals: Int): CryptoCurrency.Coin {
        val derivationPath = Network.DerivationPath.None
        val network = Network(
            id = Network.ID(value = rawNetworkId, derivationPath = derivationPath),
            backendId = rawNetworkId,
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

        return CryptoCurrency.Coin(
            id = CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.COIN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkId(rawNetworkId),
                suffix = CryptoCurrency.ID.Suffix.RawID(rawNetworkId),
            ),
            network = network,
            name = "TEST_COIN",
            symbol = "TCN",
            decimals = decimals,
            iconUrl = null,
            isCustom = false,
        )
    }

    private fun createStatus(token: CryptoCurrency.Token, fiatRate: BigDecimal?): CryptoCurrencyStatus {
        return CryptoCurrencyStatus(
            currency = token,
            value = CryptoCurrencyStatus.Custom(
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
            ),
        )
    }
}