package com.tangem.domain.moneyaccount.usecase

import arrow.core.right
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.domain.moneyaccount.yield.MoneyAccountYieldModeStatusProvider
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.domain.networks.single.SingleNetworkStatusProducer
import com.tangem.domain.networks.single.SingleNetworkStatusSupplier
import com.tangem.domain.tangempay.TangemPayCurrencyFactory
import com.tangem.domain.quotes.single.SingleQuoteStatusFetcher
import com.tangem.domain.quotes.single.SingleQuoteStatusProducer
import com.tangem.domain.quotes.single.SingleQuoteStatusSupplier
import com.google.common.truth.Truth
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class GetMoneyAccountBalanceUseCaseTest {

    private val singleNetworkStatusFetcher: SingleNetworkStatusFetcher = mockk()
    private val singleNetworkStatusSupplier: SingleNetworkStatusSupplier = mockk()
    private val singleQuoteStatusFetcher: SingleQuoteStatusFetcher = mockk()
    private val singleQuoteStatusSupplier: SingleQuoteStatusSupplier = mockk()
    private val yieldModeStatusProvider: MoneyAccountYieldModeStatusProvider = mockk()

    private val useCase = GetMoneyAccountBalanceUseCase(
        singleNetworkStatusFetcher = singleNetworkStatusFetcher,
        singleNetworkStatusSupplier = singleNetworkStatusSupplier,
        singleQuoteStatusFetcher = singleQuoteStatusFetcher,
        singleQuoteStatusSupplier = singleQuoteStatusSupplier,
        yieldModeStatusProvider = yieldModeStatusProvider,
    )

    private val userWalletId = UserWalletId("011")
    private val network: Network = mockk()
    private val tokenId: CryptoCurrency.ID = mockk()
    private val token: CryptoCurrency.Token = mockk {
        every { this@mockk.network } returns this@GetMoneyAccountBalanceUseCaseTest.network
        every { id } returns tokenId
    }
    private val networkParams = SingleNetworkStatusProducer.Params(userWalletId, network)
    private val quoteParams = SingleQuoteStatusProducer.Params(rawCurrencyId = TangemPayCurrencyFactory.TOKEN_ID)

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            singleNetworkStatusFetcher,
            singleNetworkStatusSupplier,
            singleQuoteStatusFetcher,
            singleQuoteStatusSupplier,
            yieldModeStatusProvider,
        )
        coEvery { singleNetworkStatusFetcher(any()) } returns Unit.right()
        coEvery { yieldModeStatusProvider.get(any(), any(), any()) } returns null
        coEvery { singleQuoteStatusFetcher(any()) } returns Unit.right()
    }

    @Test
    fun `GIVEN verified status and quote WHEN invoke THEN balance with rate returned`() = runTest {
        // Arrange
        val amount = BigDecimal("12.34")
        val fiatRate = BigDecimal("0.92")
        coEvery { singleNetworkStatusSupplier.getSyncOrNull(networkParams) } returns
            networkStatusWith(NetworkStatus.Amount.Loaded(amount))
        coEvery { singleQuoteStatusSupplier.getSyncOrNull(quoteParams) } returns quoteStatusWith(fiatRate)

        // Act
        val actual = useCase(userWalletId, token)

        // Assert
        Truth.assertThat(actual.getOrNull()).isEqualTo(expectedBalance(amount = amount, fiatRate = fiatRate))
        coVerify(exactly = 1) {
            singleNetworkStatusFetcher(
                SingleNetworkStatusFetcher.Params(
                    userWalletId = userWalletId,
                    network = network,
                    extraTokens = setOf(token),
                ),
            )
            singleQuoteStatusFetcher(
                SingleQuoteStatusFetcher.Params(rawCurrencyId = TangemPayCurrencyFactory.TOKEN_ID, appCurrencyId = null),
            )
        }
    }

    @Test
    fun `GIVEN yield supply status provided WHEN invoke THEN balance carries it`() = runTest {
        // Arrange
        val amount = BigDecimal("12.34")
        val networkStatus = networkStatusWith(NetworkStatus.Amount.Loaded(amount))
        val verified = networkStatus.value as NetworkStatus.Verified
        val yieldSupplyStatus = YieldSupplyStatus(
            isActive = true,
            isInitialized = true,
            isAllowedToSpend = true,
            effectiveProtocolBalance = BigDecimal("10"),
        )
        coEvery { singleNetworkStatusSupplier.getSyncOrNull(networkParams) } returns networkStatus
        coEvery { singleQuoteStatusSupplier.getSyncOrNull(quoteParams) } returns null
        coEvery { yieldModeStatusProvider.get(userWalletId, token, verified) } returns yieldSupplyStatus

        // Act
        val actual = useCase(userWalletId, token)

        // Assert
        val expected = expectedBalance(amount = amount, fiatRate = null).copy(yieldSupplyStatus = yieldSupplyStatus)
        Truth.assertThat(actual.getOrNull()).isEqualTo(expected)
    }

    @Test
    fun `GIVEN verified status without quote WHEN invoke THEN balance without rate returned`() = runTest {
        // Arrange
        val amount = BigDecimal("12.34")
        coEvery { singleNetworkStatusSupplier.getSyncOrNull(networkParams) } returns
            networkStatusWith(NetworkStatus.Amount.Loaded(amount))
        coEvery { singleQuoteStatusSupplier.getSyncOrNull(quoteParams) } returns null

        // Act
        val actual = useCase(userWalletId, token)

        // Assert
        Truth.assertThat(actual.getOrNull()).isEqualTo(expectedBalance(amount = amount, fiatRate = null))
    }

    @Test
    fun `GIVEN no verified status WHEN invoke THEN left returned`() = runTest {
        // Arrange
        coEvery { singleNetworkStatusSupplier.getSyncOrNull(networkParams) } returns null
        coEvery { singleQuoteStatusSupplier.getSyncOrNull(quoteParams) } returns null

        // Act
        val actual = useCase(userWalletId, token)

        // Assert
        Truth.assertThat(actual.isLeft()).isTrue()
    }

    @Test
    fun `GIVEN token amount not loaded WHEN invoke THEN left returned`() = runTest {
        // Arrange
        coEvery { singleNetworkStatusSupplier.getSyncOrNull(networkParams) } returns
            networkStatusWith(NetworkStatus.Amount.NotFound)
        coEvery { singleQuoteStatusSupplier.getSyncOrNull(quoteParams) } returns null

        // Act
        val actual = useCase(userWalletId, token)

        // Assert
        Truth.assertThat(actual.isLeft()).isTrue()
    }

    private fun expectedBalance(amount: BigDecimal, fiatRate: BigDecimal?) = MoneyAccountStatusValue.Balance(
        cryptoAmount = amount,
        depositAddress = DEPOSIT_ADDRESS,
        currencyCode = "USD",
        fiatRate = fiatRate,
    )

    private fun networkStatusWith(amount: NetworkStatus.Amount): NetworkStatus {
        val verified: NetworkStatus.Verified = mockk {
            every { amounts } returns mapOf(tokenId to amount)
            every { address } returns NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(
                    type = NetworkAddress.Address.Type.Primary,
                    value = DEPOSIT_ADDRESS,
                ),
            )
        }
        return mockk { every { value } returns verified }
    }

    private fun quoteStatusWith(fiatRate: BigDecimal) = QuoteStatus(
        rawCurrencyId = TangemPayCurrencyFactory.TOKEN_ID,
        value = QuoteStatus.Data(
            source = StatusSource.ACTUAL,
            fiatRate = fiatRate,
            fiatRateUSD = fiatRate,
            priceChange = BigDecimal.ZERO,
        ),
    )

    private companion object {
        const val DEPOSIT_ADDRESS = "0xdeposit"
    }
}