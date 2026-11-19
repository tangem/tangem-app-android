package com.tangem.domain.hotwallet

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.hotwallet.repository.HotWalletRepository
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.quotes.QuotesRepository
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CheckHotWalletBalanceThresholdUseCaseTest {

    private val hotWalletRepository: HotWalletRepository = mockk(relaxUnitFun = true)
    private val quotesRepository: QuotesRepository = mockk()
    private val useCase = CheckHotWalletBalanceThresholdUseCase(hotWalletRepository, quotesRepository)

    @BeforeEach
    fun resetMocks() {
        clearMocks(hotWalletRepository, quotesRepository)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a fresh balance WHEN checked THEN the flag is stored only when the threshold is reached`(
        model: BalanceModel,
    ) = runTest {
        // Arrange
        val statuses = model.tokens.map { token -> createStatus(rawId = token.rawId, fiatRate = token.balanceRate) }
        val storedQuotes = model.tokens.mapNotNullTo(mutableSetOf()) { token ->
            val rawId = token.rawId ?: return@mapNotNullTo null
            QuoteStatus(rawCurrencyId = CryptoCurrency.RawID(rawId), value = token.storedQuote)
        }
        coEvery { quotesRepository.getMultiQuoteSyncOrNull(any()) } returns storedQuotes

        // Act
        val actual = useCase(WALLET_ID, loaded(model.amount), statuses)

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(model.expected)
        coVerify(exactly = if (model.expected) 1 else 0) {
            hotWalletRepository.setBalanceThresholdReached(WALLET_ID, isReached = true)
        }
    }

    @ParameterizedTest
    @MethodSource("provideNotFreshBalances")
    fun `GIVEN a balance that is not fresh WHEN checked THEN it is left for the next load`(
        totalFiatBalance: TotalFiatBalance,
    ) = runTest {
        // Act
        val actual = useCase(WALLET_ID, totalFiatBalance, listOf(createStatus(BITCOIN, BigDecimal("100"))))

        // Assert
        assertThat(actual.getOrNull()).isFalse()
        coVerify(exactly = 0) { quotesRepository.getMultiQuoteSyncOrNull(any()) }
        coVerify(exactly = 0) { hotWalletRepository.setBalanceThresholdReached(any(), any()) }
    }

    @Test
    fun `GIVEN no token with a price WHEN checked THEN the threshold is not reached`() = runTest {
        // Arrange
        val statuses = listOf(
            createStatus(rawId = BITCOIN, fiatRate = null),
            createStatus(rawId = null, fiatRate = BigDecimal("100")),
        )

        // Act
        val actual = useCase(WALLET_ID, loaded(amount = "10000"), statuses)

        // Assert
        assertThat(actual.getOrNull()).isFalse()
        coVerify(exactly = 0) { quotesRepository.getMultiQuoteSyncOrNull(any()) }
        coVerify(exactly = 0) { hotWalletRepository.setBalanceThresholdReached(any(), any()) }
    }

    private fun provideTestModels() = listOf(
        BalanceModel(amount = "5000", expected = true),
        BalanceModel(amount = "4999.99", expected = false),
        // 4,500 EUR at 0.9 EUR per USD is 5,000 USD
        BalanceModel(amount = "4500", tokens = listOf(eurBitcoin()), expected = true),
        BalanceModel(amount = "4499", tokens = listOf(eurBitcoin()), expected = false),
        // The dust token's rounded prices imply 2 USD per EUR, which would put 3,000 EUR at 6,000 USD
        BalanceModel(amount = "3000", tokens = listOf(eurDust(), eurBitcoin()), expected = false),
        BalanceModel(amount = "4500", tokens = listOf(eurDust(), eurBitcoin()), expected = true),
        // The store already holds a quote in another currency than the balance was summed in
        BalanceModel(
            amount = "4500",
            tokens = listOf(TokenModel(balanceRate = BigDecimal("90"), storedQuote = quoteData("9000", "100"))),
            expected = false,
        ),
        BalanceModel(amount = "10000", tokens = listOf(TokenModel(storedQuote = QuoteStatus.Empty)), expected = false),
        BalanceModel(
            amount = "10000",
            tokens = listOf(TokenModel(storedQuote = quoteData(fiatRate = "100", fiatRateUSD = "0"))),
            expected = false,
        ),
        // A custom token has no quote and doesn't take part in the conversion
        BalanceModel(amount = "4500", tokens = listOf(TokenModel(rawId = null), eurBitcoin()), expected = true),
    )

    private fun provideNotFreshBalances() = listOf(
        TotalFiatBalance.Loading,
        TotalFiatBalance.Failed,
        loaded(amount = "10000", source = StatusSource.CACHE),
        loaded(amount = "10000", source = StatusSource.ONLY_CACHE),
    )

    internal data class BalanceModel(
        val amount: String,
        val tokens: List<TokenModel> = listOf(TokenModel()),
        val expected: Boolean,
    )

    internal data class TokenModel(
        val rawId: String? = BITCOIN,
        val balanceRate: BigDecimal? = BigDecimal("100"),
        val storedQuote: QuoteStatus.Value = quoteData(fiatRate = "100", fiatRateUSD = "100"),
    )

    private fun createStatus(rawId: String?, fiatRate: BigDecimal?): CryptoCurrencyStatus = mockk {
        every { currency.id.rawCurrencyId } returns rawId?.let(CryptoCurrency::RawID)
        every { value.fiatRate } returns fiatRate
    }

    private companion object {
        val WALLET_ID = UserWalletId("0123456789ABCDEF")
        const val BITCOIN = "bitcoin"
        const val DUST = "dust"

        fun loaded(amount: String, source: StatusSource = StatusSource.ACTUAL) =
            TotalFiatBalance.Loaded(amount = BigDecimal(amount), source = source)

        fun eurBitcoin() = TokenModel(
            rawId = BITCOIN,
            balanceRate = BigDecimal("90"),
            storedQuote = quoteData(fiatRate = "90", fiatRateUSD = "100"),
        )

        fun eurDust() = TokenModel(
            rawId = DUST,
            balanceRate = BigDecimal("0.00001"),
            storedQuote = quoteData(fiatRate = "0.00001", fiatRateUSD = "0.00002"),
        )

        fun quoteData(fiatRate: String, fiatRateUSD: String) = QuoteStatus.Data(
            source = StatusSource.ACTUAL,
            fiatRate = BigDecimal(fiatRate),
            fiatRateUSD = BigDecimal(fiatRateUSD),
            priceChange = BigDecimal.ZERO,
        )
    }
}