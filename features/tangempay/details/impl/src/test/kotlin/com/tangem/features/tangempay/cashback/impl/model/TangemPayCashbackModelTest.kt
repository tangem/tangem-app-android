package com.tangem.features.tangempay.cashback.impl.model

import android.text.format.DateFormat
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.domain.models.account.TangemPayCustomerTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlan
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CashbackDisplayMode
import com.tangem.domain.pay.model.CashbackDocument
import com.tangem.domain.pay.model.CashbackHistory
import com.tangem.domain.pay.model.CashbackPromotions
import com.tangem.domain.pay.model.CashbackSummary
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.model.TangemPayCashback
import com.tangem.domain.pay.repository.CashbackRepository
import com.tangem.domain.pay.repository.OnboardingRepository
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
    private val onboardingRepository: OnboardingRepository = mockk()

    @BeforeEach
    fun setup() {
        Locale.setDefault(Locale.US)
        mockkStatic(DateFormat::class)
        every { DateFormat.getBestDateTimePattern(any(), any()) } answers { secondArg() }
        mockkObject(DateTimeFormatters)
        every { DateTimeFormatters.formatDateRange(any(), any(), any()) } returns "July 1 – 5"
        clearMocks(cashbackRepository, onboardingRepository)
        coEvery { cashbackRepository.getCashbackSummary(any()) } returns CashbackSummary.Disabled.right()
        coEvery { cashbackRepository.getCashbackPromotions(any()) } returns promotions().right()
        coEvery { cashbackRepository.getCashbackAccrualDocs(any()) } returns docs().right()
        coEvery { onboardingRepository.getCustomerInfo(any()) } returns
            customerInfo(tierId = "basic", planName = "Basic").right()
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
            .isEqualTo(resourceReference(R.string.tangempay_cashback_rate_title, wrappedList("1")))
        assertThat(model.detailsSheet.value.rows).hasSize(DETAILS_ROWS_WITH_CAP)
        assertThat(model.accrualsSheet.value.docRows).hasSize(2)
    }

    @Test
    fun `GIVEN PLUS plan WHEN model created THEN rate tile shows up to the max rate`() {
        // Arrange
        coEvery { onboardingRepository.getCustomerInfo(any()) } returns
            customerInfo(tierId = "plus", planName = "Plus").right()

        // Act
        val model = createModel()

        // Assert
        assertThat(model.content().infoTiles?.rate?.title)
            .isEqualTo(resourceReference(R.string.tangempay_cashback_rate_title_up_to, wrappedList("2")))
    }

    @Test
    fun `GIVEN promotions load fails WHEN model created THEN tiles hidden and details rows empty`() {
        // Arrange
        coEvery { cashbackRepository.getCashbackPromotions(any()) } throws RuntimeException("boom")

        // Act
        val model = createModel()

        // Assert
        assertThat(model.content().infoTiles).isNull()
        assertThat(model.detailsSheet.value.rows).isEmpty()
    }

    @Test
    fun `GIVEN summary load fails WHEN model created THEN tiles still shown`() {
        // Arrange
        coEvery { cashbackRepository.getCashbackSummary(any()) } throws RuntimeException("boom")

        // Act
        val model = createModel()

        // Assert
        assertThat(model.content().infoTiles).isNotNull()
        assertThat(model.detailsSheet.value.rows).hasSize(DETAILS_ROWS_WITH_CAP)
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
    fun `GIVEN customer info fails WHEN model created THEN rate title falls back to the base rate`() {
        // Arrange
        coEvery { onboardingRepository.getCustomerInfo(any()) } throws RuntimeException("boom")

        // Act
        val model = createModel()

        // Assert
        assertThat(model.content().infoTiles?.rate?.title)
            .isEqualTo(resourceReference(R.string.tangempay_cashback_rate_title, wrappedList("1")))
    }

    @Test
    fun `GIVEN docs load fails WHEN model created THEN accrual doc rows empty but info rows kept`() {
        // Arrange
        coEvery { cashbackRepository.getCashbackAccrualDocs(any()) } throws RuntimeException("boom")

        // Act
        val model = createModel()

        // Assert
        assertThat(model.accrualsSheet.value.docRows).isEmpty()
        assertThat(model.accrualsSheet.value.infoRows).isNotEmpty()
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
        onboardingRepository = onboardingRepository,
    )

    private fun TangemPayCashbackModel.content(): TangemPayCashbackScreenUM.Content =
        uiState.value as TangemPayCashbackScreenUM.Content

    private fun promotions(
        additional: List<CashbackPromotions.AdditionalCashback> = emptyList(),
    ) = CashbackPromotions(
        cardTiers = listOf(
            CashbackPromotions.CardTier(
                tier = "basic",
                label = "Basic",
                scope = "All purchases",
                minTransactionAmount = BigDecimal("30"),
                monthlyCapAmount = BigDecimal("100"),
            ),
            CashbackPromotions.CardTier(
                tier = "plus",
                label = "Plus",
                scope = "All purchases",
                minTransactionAmount = BigDecimal("30"),
                monthlyCapAmount = BigDecimal("300"),
            ),
        ),
        monthlyCap = CashbackPromotions.MonthlyCap(amount = BigDecimal("150"), currency = "USD"),
        additionalCashback = additional,
    )

    private fun additionalPromo() = CashbackPromotions.AdditionalCashback(
        id = "promo-1",
        name = "Groceries increase",
        description = "+1% cashback for groceries stores",
        isPermanent = true,
        endDate = null,
    )

    private fun docs() = listOf(
        CashbackDocument(id = "excluded", title = "All categories without cashback", url = "https://x/excluded.pdf"),
        CashbackDocument(id = "terms", title = "Full terms of cashback program", url = "https://x/terms.pdf"),
    )

    private fun enabledSummary() = CashbackSummary.Enabled(
        displayMode = CashbackDisplayMode.FULL,
        cashback = TangemPayCashback(
            confirmedAmount = BigDecimal("32.15"),
            pendingAmount = BigDecimal.ZERO,
            currency = "USD",
            payoutCurrency = "USDC",
            payoutNetwork = "Polygon",
            period = TangemPayCashback.Period(
                year = 2026,
                month = 6,
                payoutStart = DateTime.parse("2026-07-02"),
                payoutEnd = DateTime.parse("2026-07-05"),
            ),
        ),
    )

    private fun history() = CashbackHistory(
        currency = "USD",
        months = listOf(
            CashbackHistory.MonthlyCashback(year = 2026, month = 5, confirmedAmount = BigDecimal("26.10")),
            CashbackHistory.MonthlyCashback(year = 2026, month = 6, confirmedAmount = BigDecimal("32.15")),
        ),
    )

    private fun customerInfo(tierId: String, planName: String): CustomerInfo {
        val currentPlan = TangemPayTariffPlan(
            id = "plan-$tierId",
            tierId = tierId,
            isBasicTier = tierId == "basic",
            name = planName,
            programName = "program",
            descriptionItems = emptyList(),
        )
        val customerTariffPlanMock = mockk<TangemPayCustomerTariffPlan> { every { plan } returns currentPlan }
        return mockk { every { tariffPlan } returns customerTariffPlanMock }
    }

    private companion object {
        const val DETAILS_ROWS_WITH_CAP = 5
    }
}