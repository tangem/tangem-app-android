package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.tangem.common.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.pay.TangemPayCard
import com.tangem.feature.wallet.child.wallet.model.intents.TangemPayIntents
import com.tangem.features.tangempay.entity.TangemPayMainUM
import com.tangem.test.core.ProvideTestModels
import com.google.common.truth.Truth.assertThat
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayMainBlockConverterTest {

    private val tangemPayIntents: TangemPayIntents = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(tangemPayIntents)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN multichain toggle WHEN convert Loaded THEN balance subtitle matches`(model: BalanceSubtitleModel) {
        // Arrange
        val converter = TangemPayMainBlockConverter(
            tangemPayClickIntents = tangemPayIntents,
            isAccountMultichainEnabled = model.isAccountMultichainEnabled,
        )

        // Act
        val result = converter.convert(paymentAccount(loadedValue()))

        // Assert
        val content = result as TangemPayMainUM.Content
        assertThat(content.balanceSubtitle).isEqualTo(model.expectedBalanceSubtitle)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN multichain toggle WHEN convert Deactivated THEN balance subtitle matches`(model: BalanceSubtitleModel) {
        // Arrange
        val converter = TangemPayMainBlockConverter(
            tangemPayClickIntents = tangemPayIntents,
            isAccountMultichainEnabled = model.isAccountMultichainEnabled,
        )

        // Act
        val result = converter.convert(paymentAccount(deactivatedValue()))

        // Assert
        val content = result as TangemPayMainUM.Content
        assertThat(content.balanceSubtitle).isEqualTo(model.expectedBalanceSubtitle)
    }

    @Test
    fun `GIVEN loaded account with cards WHEN convert THEN subtitle is cards count`() {
        // Arrange
        val converter = TangemPayMainBlockConverter(
            tangemPayClickIntents = tangemPayIntents,
            isAccountMultichainEnabled = true,
        )

        // Act
        val result = converter.convert(paymentAccount(loadedValue(cardsCount = 2)))

        // Assert
        val content = result as TangemPayMainUM.Content
        assertThat(content.subtitle).isEqualTo(
            pluralReference(id = R.plurals.tangempay_cards_count, count = 2, formatArgs = wrappedList(2)),
        )
    }

    @Test
    fun `GIVEN loaded account without cards WHEN convert THEN block is temporary unavailable`() {
        // Arrange
        val converter = TangemPayMainBlockConverter(
            tangemPayClickIntents = tangemPayIntents,
            isAccountMultichainEnabled = true,
        )

        // Act
        val result = converter.convert(paymentAccount(loadedValue(cardsCount = 0)))

        // Assert
        assertThat(result).isEqualTo(TangemPayMainUM.TemporaryUnavailable)
    }

    @Test
    fun `GIVEN card issue failed WHEN convert THEN block click opens the payment account`() {
        // Arrange
        val converter = TangemPayMainBlockConverter(
            tangemPayClickIntents = tangemPayIntents,
            isAccountMultichainEnabled = false,
        )
        val status = paymentAccount(PaymentAccountStatusValue.Error.CardIssueFailed(customerId = "customer"))

        // Act
        val result = converter.convert(status)

        // Assert
        assertThat(result).isInstanceOf(TangemPayMainUM.FailedToIssue::class.java)
        (result as TangemPayMainUM.FailedToIssue).onClick()
        verify(exactly = 1) { tangemPayIntents.openDetails(status) }
    }

    private fun provideTestModels() = listOf(
        BalanceSubtitleModel(
            isAccountMultichainEnabled = true,
            expectedBalanceSubtitle = stringReference("USD"),
        ),
        BalanceSubtitleModel(
            isAccountMultichainEnabled = false,
            expectedBalanceSubtitle = stringReference(ACCOUNT_CURRENCY_SYMBOL),
        ),
    )

    private fun paymentAccount(value: PaymentAccountStatusValue): AccountStatus.Payment {
        return AccountStatus.Payment(account = mockk(), value = value)
    }

    private fun loadedValue(cardsCount: Int = 1): PaymentAccountStatusValue.Loaded {
        return PaymentAccountStatusValue.Loaded(
            source = StatusSource.ACTUAL,
            customerId = "customer",
            paymentAccountAddress = "0xdeposit",
            balance = null,
            cryptoCurrency = accountToken(),
            networks = emptyList(),
            cards = List(cardsCount) { mockk<TangemPayCard>() },
            fiatRate = null,
            error = null,
            virtualAccount = null,
            tariffPlan = null,
        )
    }

    private fun deactivatedValue(): PaymentAccountStatusValue.Deactivated {
        return PaymentAccountStatusValue.Deactivated(
            source = StatusSource.ACTUAL,
            customerId = "customer",
            balance = null,
            cryptoCurrency = accountToken(),
            networks = emptyList(),
            fiatRate = null,
            error = null,
        )
    }

    private fun accountToken(): CryptoCurrency.Token = mockk {
        every { symbol } returns ACCOUNT_CURRENCY_SYMBOL
    }

    internal data class BalanceSubtitleModel(
        val isAccountMultichainEnabled: Boolean,
        val expectedBalanceSubtitle: TextReference,
    )

    private companion object {
        const val ACCOUNT_CURRENCY_SYMBOL = "USDC"
    }
}