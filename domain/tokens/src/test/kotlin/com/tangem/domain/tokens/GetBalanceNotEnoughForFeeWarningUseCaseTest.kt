package com.tangem.domain.tokens

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.model.FeePaidCurrency
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.CurrencyChecksRepository
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetBalanceNotEnoughForFeeWarningUseCaseTest {

    private val currenciesRepository: CurrenciesRepository = mockk()
    private val multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier = mockk()
    private val currencyChecksRepository: CurrencyChecksRepository = mockk()

    private val useCase = GetBalanceNotEnoughForFeeWarningUseCase(
        currenciesRepository = currenciesRepository,
        multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
        dispatchers = TestingCoroutineDispatcherProvider(),
        currencyChecksRepository = currencyChecksRepository,
    )

    private val userWalletId: UserWalletId = mockk()

    private val factory = MockCryptoCurrencyFactory()
    private val usdt = factory.createToken(blockchain = Blockchain.Tron, id = "usdt", contractAddress = "TUsdt")
    private val usdc = factory.createToken(blockchain = Blockchain.Tron, id = "usdc", contractAddress = "TUsdc")

    @BeforeEach
    fun setup() {
        // Not the Tron path under test — return a neutral fee-paid currency and disable EVM gasless.
        coEvery { currenciesRepository.getFeePaidCurrency(any(), any()) } returns FeePaidCurrency.Coin
        every { currencyChecksRepository.isNetworkSupportedForGaslessTx(any()) } returns false
    }

    @Test
    fun `GIVEN Tron same-token fee WHEN compensation fits balance THEN no warning`() = runTest {
        // Arrange — max send: the whole 2.80 balance is entered and the 2.74 compensation comes out of it.
        // The amount-subtraction path reduces the amount, so this must not be reported as insufficient.
        val result = useCase(
            fee = BigDecimal("2.74"),
            userWalletId = userWalletId,
            tokenStatus = statusOf(usdt, balance = BigDecimal("2.80")),
            feeStatus = statusOf(usdt, balance = BigDecimal("2.80")),
        )

        // Assert
        assertThat(result.getOrNull()).isNull()
    }

    @Test
    fun `GIVEN Tron same-token fee WHEN compensation alone exceeds balance THEN no warning`() = runTest {
        // Arrange — even here the warning belongs to TotalExceedsBalance, not to this use case:
        // reporting both would show two notifications for one problem.
        val result = useCase(
            fee = BigDecimal("2.74"),
            userWalletId = userWalletId,
            tokenStatus = statusOf(usdt, balance = BigDecimal("1.00")),
            feeStatus = statusOf(usdt, balance = BigDecimal("1.00")),
        )

        // Assert
        assertThat(result.getOrNull()).isNull()
    }

    @Test
    fun `GIVEN Tron cross-token fee WHEN compensation exceeds fee token balance THEN warning`() = runTest {
        // Arrange — sending USDT, paying fee in USDC: only the 2.74 compensation must fit the 2.00 balance.
        val result = useCase(
            fee = BigDecimal("2.74"),
            userWalletId = userWalletId,
            tokenStatus = statusOf(usdt, balance = BigDecimal("100")),
            feeStatus = statusOf(usdc, balance = BigDecimal("2.00")),
        )

        // Assert
        assertThat(result.getOrNull()).isInstanceOf(CryptoCurrencyWarning.BalanceNotEnoughForFee::class.java)
    }

    @Test
    fun `GIVEN Tron cross-token fee WHEN compensation fits fee token balance THEN no warning`() = runTest {
        // Arrange — the sent USDT balance is NOT part of the USDC fee check.
        val result = useCase(
            fee = BigDecimal("2.74"),
            userWalletId = userWalletId,
            tokenStatus = statusOf(usdt, balance = BigDecimal("0.15")),
            feeStatus = statusOf(usdc, balance = BigDecimal("3.00")),
        )

        // Assert
        assertThat(result.getOrNull()).isNull()
    }

    @Test
    fun `GIVEN regular Tron token send paying TRX fee WHEN TRX covers fee THEN no warning`() = runTest {
        // Arrange — non-gasless USDT send: the fee is paid in the TRX coin (feeStatus is the coin),
        // so the gasless branch must NOT own the decision and the TRX balance covers the 5 TRX fee.
        coEvery { currenciesRepository.getFeePaidCurrency(any(), any()) } returns FeePaidCurrency.Coin
        val trx = factory.createCoin(blockchain = Blockchain.Tron)
        val result = useCase(
            fee = BigDecimal("5"),
            userWalletId = userWalletId,
            tokenStatus = statusOf(usdt, balance = BigDecimal("100")),
            feeStatus = statusOf(trx, balance = BigDecimal("10")),
        )

        // Assert
        assertThat(result.getOrNull()).isNull()
    }

    @Test
    fun `GIVEN regular Tron token send paying TRX fee WHEN TRX below fee THEN warning`() = runTest {
        // Arrange — same non-gasless path, but not enough TRX: the generic coin-fee rule still fires.
        coEvery { currenciesRepository.getFeePaidCurrency(any(), any()) } returns FeePaidCurrency.Coin
        val trx = factory.createCoin(blockchain = Blockchain.Tron)
        val result = useCase(
            fee = BigDecimal("5"),
            userWalletId = userWalletId,
            tokenStatus = statusOf(usdt, balance = BigDecimal("100")),
            feeStatus = statusOf(trx, balance = BigDecimal("2")),
        )

        // Assert
        assertThat(result.getOrNull()).isInstanceOf(CryptoCurrencyWarning.BalanceNotEnoughForFee::class.java)
    }

    @Test
    fun `GIVEN Tron cross-token fee WHEN fee token balance unknown THEN no warning`() = runTest {
        // Arrange — balance unknown (fee still loads), so the notification-level guard is skipped.
        val result = useCase(
            fee = BigDecimal("2.74"),
            userWalletId = userWalletId,
            tokenStatus = statusOf(usdt, balance = BigDecimal("100")),
            feeStatus = statusOf(usdc, balance = null),
        )

        // Assert
        assertThat(result.getOrNull()).isNull()
    }

    private fun statusOf(currency: CryptoCurrency, balance: BigDecimal?): CryptoCurrencyStatus {
        val value = mockk<CryptoCurrencyStatus.Value> {
            every { amount } returns balance
        }
        return CryptoCurrencyStatus(currency = currency, value = value)
    }
}