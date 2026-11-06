package com.tangem.features.tangempay.eligibility.country

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.pay.TangemPayCountry
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.models.pay.TangemPayRegion
import com.tangem.domain.models.pay.TangemPayCustomerResidency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.usecase.ClearTangemPayCustomerResidencyUseCase
import com.tangem.domain.pay.usecase.GetTangemPayCountriesUseCase
import com.tangem.domain.pay.usecase.GetTangemPayProductsAvailabilityForCountryUseCase
import com.tangem.domain.pay.usecase.GetTangemPayRegionsUseCase
import com.tangem.domain.pay.usecase.StoreTangemPayCustomerResidencyUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.features.tangempay.eligibility.EligibilityInnerRoute
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
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
    private val getProductsAvailabilityUseCase: GetTangemPayProductsAvailabilityForCountryUseCase = mockk()
    private val storeCustomerResidencyUseCase: StoreTangemPayCustomerResidencyUseCase = mockk()
    private val clearCustomerResidencyUseCase: ClearTangemPayCustomerResidencyUseCase = mockk()
    private val onboardingRepository: OnboardingRepository = mockk()
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            router,
            getCountriesUseCase,
            getRegionsUseCase,
            getProductsAvailabilityUseCase,
            storeCustomerResidencyUseCase,
            clearCustomerResidencyUseCase,
            onboardingRepository,
            uiMessageSender,
            analytics,
        )
        coEvery { getCountriesUseCase() } returns listOf(GERMANY, USA).right()
        coEvery { getProductsAvailabilityUseCase(GERMANY.code, null) } returns
            setOf(TangemPayProduct.PAYMENT_ACCOUNT).right()
        coEvery { storeCustomerResidencyUseCase(any(), any()) } just Runs
        coEvery { clearCustomerResidencyUseCase(any()) } just Runs
    }

    @Test
    fun `GIVEN eligible country WHEN country clicked THEN customer residency cleared`() = runTest {
        // Arrange
        coEvery { onboardingRepository.getCustomerInfo(USER_WALLET_ID) } returns
            customerInfo(KycStatus.APPROVED).right()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.item(GERMANY.code).onClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { clearCustomerResidencyUseCase(USER_WALLET_ID) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN no products available in region WHEN region clicked THEN residency stored`() = runTest {
        // Arrange
        coEvery { getRegionsUseCase(USA.code) } returns listOf(TEXAS).right()
        coEvery { getProductsAvailabilityUseCase(USA.code, TEXAS.code) } returns emptySet<TangemPayProduct>().right()
        val regionsModel = createModel(
            testScope = this,
            step = ResidencyListComponent.Step.Regions(countryCode = USA.code, countryName = USA.name),
        )
        advanceUntilIdle()

        // Act
        regionsModel.item(TEXAS.code).onClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) {
            storeCustomerResidencyUseCase(
                userWalletId = USER_WALLET_ID,
                residency = TangemPayCustomerResidency(countryCode = USA.code, regionCode = TEXAS.code),
            )
        }
        verify(exactly = 1) {
            router.push(
                route = EligibilityInnerRoute.RegionUnavailable(countryCode = USA.code, regionCode = TEXAS.code),
                onComplete = any(),
            )
        }
        regionsModel.onDestroy()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN eligible country WHEN country clicked THEN next screen opened by KYC status`(
        model: ContinueModel,
    ) = runTest {
        // Arrange
        coEvery { onboardingRepository.getCustomerInfo(USER_WALLET_ID) } returns
            customerInfo(model.kycStatus).right()
        coEvery { getProductsAvailabilityUseCase(GERMANY.code, null) } returns model.availableProducts.right()
        val residencyModel = createModel(testScope = this)
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
            val model = createModel(testScope = this)
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

    @Test
    fun `GIVEN country with regions WHEN country and region clicked THEN single eligibility result sent`() =
        runTest {
            // Arrange
            coEvery { getProductsAvailabilityUseCase(USA.code, null) } returns
                setOf(TangemPayProduct.BANKING_ACCOUNT).right()
            coEvery { getProductsAvailabilityUseCase(USA.code, TEXAS.code) } returns
                setOf(TangemPayProduct.BANKING_ACCOUNT).right()
            coEvery { getRegionsUseCase(USA.code) } returns listOf(TEXAS).right()
            coEvery { onboardingRepository.getCustomerInfo(USER_WALLET_ID) } returns
                customerInfo(KycStatus.APPROVED).right()
            val countriesModel = createModel(testScope = this)
            val regionsModel = createModel(
                testScope = this,
                step = ResidencyListComponent.Step.Regions(countryCode = USA.code, countryName = USA.name),
            )
            advanceUntilIdle()

            // Act
            countriesModel.item(USA.code).onClick()
            advanceUntilIdle()
            regionsModel.item(TEXAS.code).onClick()
            advanceUntilIdle()

            // Assert
            val sentEvents = mutableListOf<AnalyticsEvent>()
            verify { analytics.send(capture(sentEvents)) }
            assertThat(sentEvents.filterIsInstance<TangemPayAnalyticsEvents.Eligibility.EligibilityCheckResult>())
                .containsExactly(
                    TangemPayAnalyticsEvents.Eligibility.EligibilityCheckResult(
                        isEligible = true,
                        country = USA.name,
                        state = TEXAS.name,
                    ),
                )
            verify(exactly = 1) {
                router.push(
                    route = EligibilityInnerRoute.Regions(countryCode = USA.code, countryName = USA.name),
                    onComplete = any(),
                )
            }
            countriesModel.onDestroy()
            regionsModel.onDestroy()
        }

    private fun provideTestModels() = availableProductsCombinations().flatMap { availableProducts ->
        listOf(
            ContinueModel(
                availableProducts = availableProducts,
                kycStatus = KycStatus.APPROVED,
                expectedRoutes = listOf(AppRoute.Wallet),
            ),
            ContinueModel(
                availableProducts = availableProducts,
                kycStatus = KycStatus.PENDING,
                expectedRoutes = listOf(AppRoute.Wallet, AppRoute.Kyc(userWalletId = USER_WALLET_ID)),
            ),
        )
    }

    private fun availableProductsCombinations(): List<Set<TangemPayProduct>> =
        TangemPayProduct.entries.map(::setOf) + listOf(TangemPayProduct.entries.toSet())

    private fun createModel(
        testScope: TestScope,
        step: ResidencyListComponent.Step = ResidencyListComponent.Step.Countries,
    ) = ResidencyListModel(
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        router = router,
        getCountriesUseCase = getCountriesUseCase,
        getRegionsUseCase = getRegionsUseCase,
        getProductsAvailabilityUseCase = getProductsAvailabilityUseCase,
        storeCustomerResidencyUseCase = storeCustomerResidencyUseCase,
        clearCustomerResidencyUseCase = clearCustomerResidencyUseCase,
        onboardingRepository = onboardingRepository,
        uiMessageSender = uiMessageSender,
        analytics = analytics,
        paramsContainer = MutableParamsContainer(
            value = ResidencyListComponent.Params(
                userWalletId = USER_WALLET_ID,
                step = step,
                onCloseClick = {},
            ),
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
        val availableProducts: Set<TangemPayProduct>,
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
        val USA = TangemPayCountry(
            code = "US",
            name = "United States",
            flagUrl = null,
            isSuggested = false,
            hasRegions = true,
        )
        val TEXAS = TangemPayRegion(code = "TX", name = "Texas", isSuggested = false)
    }
}