package com.tangem.domain.yield.supply

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.quotes.QuotesRepository
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.transaction.FeeRepository
import com.tangem.domain.yield.supply.usecase.YieldSupplyMinAmountUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

class YieldSupplyMinAmountUseCaseTest {

    private val feeRepository: FeeRepository = mockk(relaxed = true)
    private val quotesRepository: QuotesRepository = mockk(relaxed = true)
    private val currenciesRepository: CurrenciesRepository = mockk(relaxed = true)

    private val useCase = YieldSupplyMinAmountUseCase(
        feeRepository = feeRepository,
        quotesRepository = quotesRepository,
        currenciesRepository = currenciesRepository,
    )

    @Test
    fun `GIVEN valid inputs WHEN invoke THEN return expected min amount`() = runTest {
        val network = createNetwork()
        val nativeCoin = createNativeCoin(network)
        val token = createToken(network)

        val tokenFiatRate = BigDecimal("1.00")
        val tokenStatus = CryptoCurrencyStatus(
            currency = token,
            value = CryptoCurrencyStatus.Loaded(
                amount = BigDecimal.ZERO,
                fiatAmount = BigDecimal.ZERO,
                fiatRate = tokenFiatRate,
                priceChange = BigDecimal.ZERO,
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
        val userWallet = createUserWallet()
        val maxFeePerGas = BigInteger("158320679232")
        val fee = createEip1559Fee(maxFeePerGas)

        coEvery { feeRepository.getEthereumFeeWithoutGas(userWallet.walletId, token) } returns fee
        coEvery {
            currenciesRepository.getNetworkCoin(
                userWalletId = userWallet.walletId,
                networkId = token.network.id,
                derivationPath = token.network.derivationPath,
            )
        } returns nativeCoin

        val nativeFiatRate = BigDecimal("0.20353756561552608")
        coEvery {
            quotesRepository.getMultiQuoteSyncOrNull(setOf(nativeCoin.id.rawCurrencyId!!))
        } returns setOf(
            QuoteStatus(
                rawCurrencyId = CryptoCurrency.RawID("polygon-ecosystem-token"),
                value = QuoteStatus.Data(
                    source = StatusSource.ACTUAL,
                    fiatRate = nativeFiatRate,
                    priceChange = BigDecimal("0.09000000000000007"),
                ),
            ),
        )
        val expected = expectedMinAmount(maxFeePerGas, nativeFiatRate, tokenFiatRate, token.decimals)
        val result = useCase(userWallet.walletId, tokenStatus).getOrNull()
        Truth.assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `GIVEN missing token fiat rate WHEN invoke THEN return left with error`() = runTest {
        val network = createNetwork()
        val token = createToken(network)
        val tokenStatus = CryptoCurrencyStatus(
            currency = token,
            value = CryptoCurrencyStatus.Custom(
                amount = BigDecimal.ZERO,
                fiatAmount = null,
                fiatRate = null,
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
        val userWallet = createUserWallet()
        val result = useCase(userWallet.walletId, tokenStatus)
        Truth.assertThat(result.isLeft()).isTrue()
        Truth.assertThat(result.leftOrNull()?.message).isEqualTo("Fiat rate is missing")
    }

    @Test
    fun `GIVEN quotes unavailable WHEN invoke THEN return left with error`() = runTest {
        val network = createNetwork()
        val nativeCoin = createNativeCoin(network)
        val token = createToken(network)
        val tokenStatus = CryptoCurrencyStatus(
            currency = token,
            value = CryptoCurrencyStatus.Custom(
                amount = BigDecimal.ZERO,
                fiatAmount = BigDecimal.ZERO,
                fiatRate = BigDecimal.ONE,
                priceChange = BigDecimal.ZERO,
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
        val userWallet = createUserWallet()
        val maxFeePerGas = BigInteger("158320679232")
        val fee = createEip1559Fee(maxFeePerGas)

        coEvery { feeRepository.getEthereumFeeWithoutGas(userWallet.walletId, token) } returns fee
        coEvery {
            currenciesRepository.getNetworkCoin(
                userWalletId = userWallet.walletId,
                networkId = token.network.id,
                derivationPath = token.network.derivationPath,
            )
        } returns nativeCoin

        coEvery {
            quotesRepository.getMultiQuoteSyncOrNull(setOf(nativeCoin.id.rawCurrencyId!!))
        } returns null
        val result = useCase(userWallet.walletId, tokenStatus)
        Truth.assertThat(result.isLeft()).isTrue()
        Truth.assertThat(result.leftOrNull()?.message).isEqualTo("Quotes for native coin are unavailable")
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

    private fun createUserWallet(): UserWallet {
        val wallet = mockk<UserWallet>(relaxed = true)
        every { wallet.walletId } returns UserWalletId("001122")
        return wallet
    }

    private fun createEip1559Fee(maxFeePerGas: BigInteger): Fee.Ethereum.EIP1559 {
        return Fee.Ethereum.EIP1559(
            amount = Amount(value = BigDecimal.ZERO, blockchain = Blockchain.Ethereum),
            gasLimit = BigInteger.ZERO,
            maxFeePerGas = maxFeePerGas,
            priorityFee = BigInteger.ZERO,
        )
    }

    private fun expectedMinAmount(
        maxFeePerGas: BigInteger,
        nativeFiatRate: BigDecimal,
        tokenFiatRate: BigDecimal,
        decimals: Int,
    ): BigDecimal {
        val gasLimit = BigInteger("350000")
        val nativeGas = maxFeePerGas.multiply(gasLimit).toBigDecimal().movePointLeft(decimals)
        val rateRatio = nativeFiatRate.divide(tokenFiatRate, decimals, RoundingMode.HALF_UP)
        val tokenValue = rateRatio.multiply(nativeGas)
        val feeBuffered = tokenValue.multiply(BigDecimal("1.25"))
        return feeBuffered
            .divide(BigDecimal("0.04"), decimals, RoundingMode.HALF_UP)
            .stripTrailingZeros()
    }
}