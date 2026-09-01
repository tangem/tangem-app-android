package com.tangem.features.tangempay.cashback.impl.model

import android.text.format.DateFormat
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CashbackDisplayMode
import com.tangem.domain.pay.model.CashbackDocument
import com.tangem.domain.pay.model.CashbackHistory
import com.tangem.domain.pay.model.CashbackPromotions
import com.tangem.domain.pay.model.CashbackSummary
import com.tangem.domain.pay.model.TangemPayCashback
import com.tangem.domain.pay.repository.CashbackRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.features.tangempay.cashback.api.TangemPayCashbackComponent
import com.tangem.features.tangempay.cashback.impl.ui.state.TangemPayCashbackScreenUM
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import org.joda.time.DateTime
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Locale

internal class TangemPayCashbackModelTest {

    private val defaultLocale = Locale.getDefault()
    private val userWalletId = UserWalletId("123")

    private val router: Router = mockk(relaxed = true)
    private val urlOpener: UrlOpener = mockk(relaxed = true)
    private val cashbackRepository: CashbackRepository = mockk()
    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)

    @BeforeEach
    fun setup() {
        Locale.setDefault(Locale.US)
        mockkStatic(DateFormat::class)
        // DateTimeFormatters caches its formatters in `lazy` object fields shared across the whole test JVM,
        // so this mock must mirror what ICU really does with the skeleton — see TangemPayCashbackDateFormatterTest.
        every { DateFormat.getBestDateTimePattern(any(), any()) } answers {
            val skeleton = secondArg<String>()
            if (skeleton == "d MMMM") "MMMM d" else skeleton
        }
        mockkObject(DateTimeFormatters)
        every { DateTimeFormatters.formatDateRange(any(), any(), any()) } returns "July 1 – 5"
        clearMocks(cashbackRepository)
        coEvery { cashbackRepository.getCashbackSummary(any()) } returns CashbackSummary.Disabled.right()
        coEvery { cashbackRepository.getCashbackPromotions(any()) } returns promotions().right()
        coEvery { cashbackRepository.getCashbackAccrualDocs(any()) } returns docs().right()
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(DateFormat::class)
        unmockkObject(DateTimeFormatters)
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun `GIVEN all sources load WHEN model created THEN tiles and sheets populated`() {
        // Act
        val model = createModel()

        // Assert
        assertThat(model.content().infoTiles).isNotNull()
        assertThat(model.content().infoTiles?.rate?.title)
            .isEqualTo(resourceReference(R.string.tangempay_cashback_rate_title_up_to, wrappedList("2")))
        assertThat(model.detailsSheet.value.rows).hasSize(DETAILS_ROWS_WITHOUT_PAYOUT)
        assertThat(model.accrualsSheet.value.docRows).hasSize(2)
    }

    @Test
    fun `GIVEN single card WHEN model created THEN rate tile shows its exact backend rate`() {
        // Arrange
        coEvery { cashbackRepository.getCashbackPromotions(any()) } returns
            promotions(cards = listOf(card(cardType = "prestige", title = "Prestige Card", rate = "1.5"))).right()

        // Act
        val model = createModel()

        // Assert
        assertThat(model.content().infoTiles?.rate?.title)
            .isEqualTo(resourceReference(R.string.tangempay_cashback_rate_title, wrappedList("1.5")))
    }

    @Test
    fun `GIVEN promotions load fails WHEN model created THEN error state`() {
        // Arrange
        coEvery { cashbackRepository.getCashbackPromotions(any()) } returns VisaApiError.Unspecified.left()

        // Act
        val model = createModel()

        // Assert
        assertThat(model.uiState.value).isInstanceOf(TangemPayCashbackScreenUM.Error::class.java)
    }

    @Test
    fun `GIVEN summary load fails WHEN model created THEN error state`() {
        // Arrange
        coEvery { cashbackRepository.getCashbackSummary(any()) } returns VisaApiError.Unspecified.left()

        // Act
        val model = createModel()

        // Assert
        assertThat(model.uiState.value).isInstanceOf(TangemPayCashbackScreenUM.Error::class.java)
    }

    @Test
    fun `GIVEN summary without cashback WHEN model created THEN paid-in row is dropped`() {
        // Act
        val model = createModel()

        // Assert
        assertThat(model.detailsSheet.value.rows).containsExactly(
            resourceReference(R.string.tangempay_cashback_details_tier, wrappedList("1", "Basic Card", "$30")),
            resourceReference(R.string.tangempay_cashback_details_tier, wrappedList("2", "Plus Card", "$30")),
            resourceReference(R.string.tangempay_cashback_details_eu_excluded),
            resourceReference(R.string.tangempay_cashback_details_cap, wrappedList("$300")),
        ).inOrder()
    }

    @Test
    fun `GIVEN enabled summary WHEN model created THEN paid-in row uses the backend payout currency`() {
        // Arrange
        coEvery { cashbackRepository.getCashbackSummary(any()) } returns enabledSummary(payoutCurrency = "USDT").right()
        coEvery { cashbackRepository.getCashbackHistory(any(), any()) } returns history().right()

        // Act
        val model = createModel()

        // Assert
        assertThat(model.detailsSheet.value.rows).containsExactly(
            resourceReference(R.string.tangempay_cashback_details_tier, wrappedList("1", "Basic Card", "$30")),
            resourceReference(R.string.tangempay_cashback_details_tier, wrappedList("2", "Plus Card", "$30")),
            resourceReference(R.string.tangempay_cashback_details_eu_excluded),
            resourceReference(R.string.tangempay_cashback_details_paid_in, wrappedList("USDT")),
            resourceReference(R.string.tangempay_cashback_details_cap, wrappedList("$300")),
        ).inOrder()
    }

    @Test
    fun `GIVEN summary and promotions both fail WHEN model created THEN error state`() {
        // Arrange
        coEvery { cashbackRepository.getCashbackSummary(any()) } throws RuntimeException("boom")
        coEvery { cashbackRepository.getCashbackPromotions(any()) } throws RuntimeException("boom")

        // Act
        val model = createModel()

        // Assert
        assertThat(model.uiState.value).isInstanceOf(TangemPayCashbackScreenUM.Error::class.java)
    }

    @Test
    fun `GIVEN summary and promotions both return error WHEN model created THEN error state`() {
        // Arrange
        coEvery { cashbackRepository.getCashbackSummary(any()) } returns VisaApiError.Unspecified.left()
        coEvery { cashbackRepository.getCashbackPromotions(any()) } returns VisaApiError.Unspecified.left()

        // Act
        val model = createModel()

        // Assert
        assertThat(model.uiState.value).isInstanceOf(TangemPayCashbackScreenUM.Error::class.java)
    }

    @Test
    fun `GIVEN error state WHEN reload succeeds THEN content shown`() {
        // Arrange
        coEvery { cashbackRepository.getCashbackSummary(any()) } throws RuntimeException("boom")
        coEvery { cashbackRepository.getCashbackPromotions(any()) } throws RuntimeException("boom")
        val model = createModel()
        val error = model.uiState.value as TangemPayCashbackScreenUM.Error
        coEvery { cashbackRepository.getCashbackSummary(any()) } returns CashbackSummary.Disabled.right()
        coEvery { cashbackRepository.getCashbackPromotions(any()) } returns promotions().right()

        // Act
        error.onReloadClick()

        // Assert
        assertThat(model.uiState.value).isInstanceOf(TangemPayCashbackScreenUM.Content::class.java)
    }

    @Test
    fun `GIVEN docs load fails WHEN model created THEN error state`() {
        // Arrange
        coEvery { cashbackRepository.getCashbackAccrualDocs(any()) } returns VisaApiError.Unspecified.left()

        // Act
        val model = createModel()

        // Assert
        assertThat(model.uiState.value).isInstanceOf(TangemPayCashbackScreenUM.Error::class.java)
    }

    @Test
    fun `GIVEN empty docs list WHEN model created THEN content shown with empty doc rows`() {
        // Arrange
        coEvery { cashbackRepository.getCashbackAccrualDocs(any()) } returns emptyList<CashbackDocument>().right()

        // Act
        val model = createModel()

        // Assert
        assertThat(model.uiState.value).isInstanceOf(TangemPayCashbackScreenUM.Content::class.java)
        assertThat(model.accrualsSheet.value.docRows).isEmpty()
        assertThat(model.accrualsSheet.value.infoRows).isNotEmpty()
    }

    @Test
    fun `GIVEN enabled summary AND history fails WHEN model created THEN error state`() {
        // Arrange
        coEvery { cashbackRepository.getCashbackSummary(any()) } returns enabledSummary().right()
        coEvery { cashbackRepository.getCashbackHistory(any(), any()) } returns VisaApiError.Unspecified.left()

        // Act
        val model = createModel()

        // Assert
        assertThat(model.uiState.value).isInstanceOf(TangemPayCashbackScreenUM.Error::class.java)
    }

    @Test
    fun `GIVEN additional cashback WHEN model created THEN additional cashback section populated`() {
        // Arrange
        coEvery { cashbackRepository.getCashbackPromotions(any()) } returns
            promotions(additional = listOf(additionalPromo())).right()

        // Act
        val model = createModel()

        // Assert
        assertThat(model.content().additionalCashback?.items).hasSize(1)
    }

    @Test
    fun `GIVEN no additional cashback WHEN model created THEN additional cashback section hidden`() {
        // Act
        val model = createModel()

        // Assert
        assertThat(model.content().additionalCashback).isNull()
    }

    @Test
    fun `GIVEN enabled summary and history WHEN model created THEN histogram populated`() {
        // Arrange
        coEvery { cashbackRepository.getCashbackSummary(any()) } returns enabledSummary().right()
        coEvery { cashbackRepository.getCashbackHistory(any(), any()) } returns history().right()

        // Act
        val model = createModel()

        // Assert
        assertThat(model.content().histogram).isNotNull()
        assertThat(model.content().histogram?.bars).hasSize(2)
    }

    @Test
    fun `GIVEN disabled summary WHEN model created THEN history not requested and histogram null`() {
        // Act
        val model = createModel()

        // Assert
        assertThat(model.content().histogram).isNull()
        coVerify(exactly = 0) { cashbackRepository.getCashbackHistory(any(), any()) }
    }

    private fun createModel() = TangemPayCashbackModel(
        dispatchers = TestingCoroutineDispatcherProvider(),
        paramsContainer = MutableParamsContainer(TangemPayCashbackComponent.Params(userWalletId = userWalletId)),
        router = router,
        urlOpener = urlOpener,
        cashbackRepository = cashbackRepository,
        analytics = analytics,
    )

    private fun TangemPayCashbackModel.content(): TangemPayCashbackScreenUM.Content =
        uiState.value as TangemPayCashbackScreenUM.Content

    private fun promotions(
        cards: List<CashbackPromotions.CardPromotion> = listOf(
            card(cardType = "basic", title = "Basic Card", rate = "1.0"),
            card(cardType = "plus", title = "Plus Card", rate = "2.0"),
        ),
        additional: List<CashbackPromotions.AdditionalCashback> = emptyList(),
    ) = CashbackPromotions(
        cards = cards,
        accountMonthlyCap = CashbackPromotions.MonthlyCap(amount = BigDecimal("300"), currency = "USD"),
        additionalCashback = additional,
    )

    private fun card(cardType: String, title: String, rate: String) = CashbackPromotions.CardPromotion(
        cardType = cardType,
        title = title,
        cashbackRate = BigDecimal(rate),
        minTransactionAmount = BigDecimal("30"),
        promotionId = "promo-$cardType",
    )

    private fun additionalPromo() = CashbackPromotions.AdditionalCashback(
        id = "promo-1",
        cardType = null,
        name = "Groceries increase",
        description = "+1% cashback for groceries stores",
        endDate = null,
        promoCap = null,
        minTransactionAmount = null,
        priority = 99,
    )

    private fun docs() = listOf(
        CashbackDocument(id = "excluded", title = "All categories without cashback", url = "https://x/excluded.pdf"),
        CashbackDocument(id = "terms", title = "Full terms of cashback program", url = "https://x/terms.pdf"),
    )

    private fun enabledSummary(payoutCurrency: String? = "USDC") = CashbackSummary.Enabled(
        displayMode = CashbackDisplayMode.FULL,
        cashback = TangemPayCashback(
            confirmedAmount = BigDecimal("32.15"),
            totalEarnedAmount = BigDecimal("132.15"),
            currency = "USD",
            payoutCurrency = payoutCurrency,
            previousPayout = null,
            period = TangemPayCashback.Period(
                year = 2026,
                month = 6,
                payoutStart = DateTime.parse("2026-07-02"),
                payoutEnd = DateTime.parse("2026-07-05"),
            ),
        ),
    )

    private fun history() = CashbackHistory(
        months = listOf(
            month(month = 5, amount = "26.10"),
            month(month = 6, amount = "32.15"),
        ),
    )

    private fun month(month: Int, amount: String) = CashbackHistory.MonthlyCashback(
        year = 2026,
        month = month,
        confirmedAmount = BigDecimal(amount),
        currency = "USD",
    )

    private companion object {
        const val DETAILS_ROWS_WITHOUT_PAYOUT = 4
    }
}