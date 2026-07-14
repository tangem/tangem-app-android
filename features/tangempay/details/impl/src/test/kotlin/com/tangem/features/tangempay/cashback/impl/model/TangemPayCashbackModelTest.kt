package com.tangem.features.tangempay.cashback.impl.model

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.account.TangemPayCustomerTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlan
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CashbackDocument
import com.tangem.domain.pay.model.CashbackPromotions
import com.tangem.domain.pay.model.CashbackSummary
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.repository.CashbackRepository
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.features.tangempay.cashback.api.TangemPayCashbackComponent
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
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
        clearMocks(cashbackRepository, onboardingRepository)
        coEvery { cashbackRepository.getCashbackSummary(any()) } returns CashbackSummary.Disabled.right()
        coEvery { cashbackRepository.getCashbackPromotions(any()) } returns promotions().right()
        coEvery { cashbackRepository.getCashbackAccrualDocs(any()) } returns docs().right()
        coEvery { onboardingRepository.getCustomerInfo(any()) } returns
            customerInfo(TangemPayTariffPlan.Type.BASIC).right()
    }

    @AfterEach
    fun tearDown() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun `GIVEN all sources load WHEN model created THEN tiles and sheets populated`() {
        // Act
        val model = createModel()

        // Assert
        assertThat(model.uiState.value.infoTiles).isNotNull()
        assertThat(model.uiState.value.infoTiles?.rate?.title).isEqualTo(stringReference("Cashback 1%"))
        assertThat(model.detailsSheet.value.rows).hasSize(2)
        assertThat(model.accrualsSheet.value.docRows).hasSize(2)
    }

    @Test
    fun `GIVEN PLUS plan WHEN model created THEN rate tile shows the Plus tier rate`() {
        // Arrange
        coEvery { onboardingRepository.getCustomerInfo(any()) } returns
            customerInfo(TangemPayTariffPlan.Type.PLUS).right()

        // Act
        val model = createModel()

        // Assert
        assertThat(model.uiState.value.infoTiles?.rate?.title).isEqualTo(stringReference("Cashback 2%"))
    }

    @Test
    fun `GIVEN promotions load fails WHEN model created THEN tiles hidden and details rows empty`() {
        // Arrange
        coEvery { cashbackRepository.getCashbackPromotions(any()) } throws RuntimeException("boom")

        // Act
        val model = createModel()

        // Assert
        assertThat(model.uiState.value.infoTiles).isNull()
        assertThat(model.detailsSheet.value.rows).isEmpty()
    }

    @Test
    fun `GIVEN summary load fails WHEN model created THEN tiles still shown`() {
        // Arrange
        coEvery { cashbackRepository.getCashbackSummary(any()) } throws RuntimeException("boom")

        // Act
        val model = createModel()

        // Assert
        assertThat(model.uiState.value.infoTiles).isNotNull()
        assertThat(model.detailsSheet.value.rows).hasSize(2)
    }

    @Test
    fun `GIVEN customer info fails WHEN model created THEN rate tile falls back to the first tier`() {
        // Arrange
        coEvery { onboardingRepository.getCustomerInfo(any()) } throws RuntimeException("boom")

        // Act
        val model = createModel()

        // Assert
        assertThat(model.uiState.value.infoTiles?.rate?.title).isEqualTo(stringReference("Cashback 1%"))
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

    private fun createModel() = TangemPayCashbackModel(
        dispatchers = TestingCoroutineDispatcherProvider(),
        paramsContainer = MutableParamsContainer(TangemPayCashbackComponent.Params(userWalletId = userWalletId)),
        router = router,
        urlOpener = urlOpener,
        cashbackRepository = cashbackRepository,
        onboardingRepository = onboardingRepository,
    )

    private fun promotions() = CashbackPromotions(
        cardTiers = listOf(
            CashbackPromotions.CardTier(
                tier = "basic",
                label = "Basic cards",
                scope = "All purchases",
                minTransactionAmount = BigDecimal("30"),
                monthlyCapAmount = BigDecimal("100"),
            ),
            CashbackPromotions.CardTier(
                tier = "plus",
                label = "Plus cards",
                scope = "All purchases",
                minTransactionAmount = BigDecimal("30"),
                monthlyCapAmount = BigDecimal("300"),
            ),
        ),
    )

    private fun docs() = listOf(
        CashbackDocument(id = "excluded", title = "All categories without cashback", url = "https://x/excluded.pdf"),
        CashbackDocument(id = "terms", title = "Full terms of cashback program", url = "https://x/terms.pdf"),
    )

    private fun customerInfo(planType: TangemPayTariffPlan.Type): CustomerInfo {
        val tariffPlanMock = mockk<TangemPayTariffPlan> {
            every { type } returns planType
            every { name } returns planType.name
        }
        val customerTariffPlanMock = mockk<TangemPayCustomerTariffPlan> { every { plan } returns tariffPlanMock }
        return mockk { every { tariffPlan } returns customerTariffPlanMock }
    }
}