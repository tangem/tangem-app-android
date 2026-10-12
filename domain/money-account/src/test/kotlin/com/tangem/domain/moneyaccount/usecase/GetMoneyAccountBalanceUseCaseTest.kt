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
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.domain.networks.single.SingleNetworkStatusProducer
import com.tangem.domain.networks.single.SingleNetworkStatusSupplier
import com.tangem.domain.moneyaccount.currency.MoneyAccountCurrencyFactory
import com.tangem.domain.pay.TangemPayCurrencyFactory
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

    private val moneyAccountCurrencyFactory: MoneyAccountCurrencyFactory = mockk()
    private val singleNetworkStatusFetcher: SingleNetworkStatusFetcher = mockk()
    private val singleNetworkStatusSupplier: SingleNetworkStatusSupplier = mockk()
    private val singleQuoteStatusFetcher: SingleQuoteStatusFetcher = mockk()
    private val singleQuoteStatusSupplier: SingleQuoteStatusSupplier = mockk()

    private val useCase = GetMoneyAccountBalanceUseCase(
        moneyAccountCurrencyFactory = moneyAccountCurrencyFactory,
        singleNetworkStatusFetcher = singleNetworkStatusFetcher,
        singleNetworkStatusSupplier = singleNetworkStatusSupplier,
        singleQuoteStatusFetcher = singleQuoteStatusFetcher,
        singleQuoteStatusSupplier = singleQuoteStatusSupplier,
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
            moneyAccountCurrencyFactory,
            singleNetworkStatusFetcher,
            singleNetworkStatusSupplier,
            singleQuoteStatusFetcher,
            singleQuoteStatusSupplier,
        )
        every { moneyAccountCurrencyFactory.create(userWalletId) } returns token
        coEvery { singleNetworkStatusFetcher(any()) } returns Unit.right()
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
        val actual = useCase(userWalletId)

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
    fun `GIVEN verified status without quote WHEN invoke THEN balance without rate returned`() = runTest {
        // Arrange
        val amount = BigDecimal("12.34")
        coEvery { singleNetworkStatusSupplier.getSyncOrNull(networkParams) } returns
            networkStatusWith(NetworkStatus.Amount.Loaded(amount))
        coEvery { singleQuoteStatusSupplier.getSyncOrNull(quoteParams) } returns null

        // Act
        val actual = useCase(userWalletId)

        // Assert
        Truth.assertThat(actual.getOrNull()).isEqualTo(expectedBalance(amount = amount, fiatRate = null))
    }

    @Test
    fun `GIVEN no verified status WHEN invoke THEN left returned`() = runTest {
        // Arrange
        coEvery { singleNetworkStatusSupplier.getSyncOrNull(networkParams) } returns null
        coEvery { singleQuoteStatusSupplier.getSyncOrNull(quoteParams) } returns null

        // Act
        val actual = useCase(userWalletId)

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
        val actual = useCase(userWalletId)

        // Assert
        Truth.assertThat(actual.isLeft()).isTrue()
    }

    @Test
    fun `GIVEN token cannot be created WHEN invoke THEN left returned`() = runTest {
        // Arrange
        every { moneyAccountCurrencyFactory.create(userWalletId) } throws IllegalArgumentException()

        // Act
        val actual = useCase(userWalletId)

        // Assert
        Truth.assertThat(actual.isLeft()).isTrue()
        coVerify(exactly = 0) { singleNetworkStatusFetcher(any()) }
        coVerify(exactly = 0) { singleQuoteStatusFetcher(any()) }
    }

    private fun expectedBalance(amount: BigDecimal, fiatRate: BigDecimal?) = MoneyAccountStatusValue.Balance(
        cryptoAmount = amount,
        cryptoCurrency = token,
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