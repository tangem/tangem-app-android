package com.tangem.features.tangempay.eligibility.country

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.pay.TangemPayCountry
import com.tangem.domain.models.pay.TangemPayEligibility
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.usecase.CheckTangemPayEligibilityUseCase
import com.tangem.domain.pay.usecase.GetTangemPayCountriesUseCase
import com.tangem.domain.pay.usecase.GetTangemPayRegionsUseCase
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.features.tangempay.eligibility.TangemPayEligibilityComponent
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ResidencyListModelTest {

    private val router: Router = mockk(relaxed = true)
    private val getCountriesUseCase: GetTangemPayCountriesUseCase = mockk()
    private val getRegionsUseCase: GetTangemPayRegionsUseCase = mockk()
    private val checkEligibilityUseCase: CheckTangemPayEligibilityUseCase = mockk()
    private val onboardingRepository: OnboardingRepository = mockk()
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            router,
            getCountriesUseCase,
            getRegionsUseCase,
            checkEligibilityUseCase,
            onboardingRepository,
            uiMessageSender,
        )
        coEvery { getCountriesUseCase() } returns listOf(GERMANY).right()
        coEvery { checkEligibilityUseCase(any(), GERMANY.code, null) } returns
            TangemPayEligibility(isAvailable = true, country = GERMANY.code, region = null).right()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN eligible country WHEN country clicked THEN next screen opened by KYC status`(
        model: ContinueModel,
    ) = runTest {
        // Arrange
        coEvery { onboardingRepository.getCustomerInfo(USER_WALLET_ID) } returns
            customerInfo(model.kycStatus).right()
        val residencyModel = createModel(testScope = this, product = model.product)
        advanceUntilIdle()

        // Act
        residencyModel.item(GERMANY.code).onClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { router.replaceAll(routes = model.expectedRoutes.toTypedArray(), onComplete = any()) }
        residencyModel.onDestroy()
    }

    @Test
    fun `GIVEN customer info request fails WHEN country clicked THEN error shown AND item loading released`() =
        runTest {
            // Arrange
            coEvery { onboardingRepository.getCustomerInfo(USER_WALLET_ID) } returns
                VisaApiError.ServerUnavailable.left()
            val model = createModel(testScope = this, product = TangemPayProduct.BANKING_ACCOUNT)
            advanceUntilIdle()

            // Act
            model.item(GERMANY.code).onClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { uiMessageSender.send(any()) }
            verify(exactly = 0) { router.replaceAll(routes = anyVararg(), onComplete = any()) }
            assertThat(model.item(GERMANY.code).isLoading).isFalse()
            model.onDestroy()
        }

    private fun provideTestModels() = TangemPayProduct.entries.flatMap { product ->
        listOf(
            ContinueModel(
                product = product,
                kycStatus = KycStatus.APPROVED,
                expectedRoutes = listOf(AppRoute.Wallet),
            ),
            ContinueModel(
                product = product,
                kycStatus = KycStatus.PENDING,
                expectedRoutes = listOf(AppRoute.Wallet, AppRoute.Kyc(userWalletId = USER_WALLET_ID)),
            ),
        )
    }

    private fun createModel(testScope: TestScope, product: TangemPayProduct) = ResidencyListModel(
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        router = router,
        getCountriesUseCase = getCountriesUseCase,
        getRegionsUseCase = getRegionsUseCase,
        checkEligibilityUseCase = checkEligibilityUseCase,
        onboardingRepository = onboardingRepository,
        uiMessageSender = uiMessageSender,
        analytics = mockk(relaxed = true),
        paramsContainer = MutableParamsContainer(
            value = TangemPayEligibilityComponent.Params(product = product, userWalletId = USER_WALLET_ID),
        ),
    )

    private fun customerInfo(kycStatus: KycStatus): CustomerInfo = mockk {
        every { this@mockk.kycStatus } returns kycStatus
    }

    private fun ResidencyListModel.item(id: String): ResidencyListUM.ItemUM {
        val content = uiState.value.content as ResidencyListUM.Content.Sections
        return content.sections.flatMap { it.items }.first { it.id == id }
    }

    data class ContinueModel(
        val product: TangemPayProduct,
        val kycStatus: KycStatus,
        val expectedRoutes: List<AppRoute>,
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        val GERMANY = TangemPayCountry(
            code = "DE",
            name = "Germany",
            flagUrl = null,
            isSuggested = false,
            hasRegions = false,
        )
    }
}