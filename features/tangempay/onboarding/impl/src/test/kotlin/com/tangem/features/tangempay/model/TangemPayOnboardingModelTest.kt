@file:OptIn(ExperimentalCoroutinesApi::class)

package com.tangem.features.tangempay.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.common.routing.AppRoute
import com.tangem.core.error.UniversalError
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayEligibilityManager
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.model.TangemPayEntryPoint
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.usecase.ProduceTangemPayInitialDataUseCase
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.features.tangempay.components.TangemPayOnboardingComponent
import com.tangem.features.tangempay.ui.TangemPayOnboardingScreenState
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayOnboardingModelTest {

    private val router: Router = mockk(relaxed = true)
    private val repository: OnboardingRepository = mockk(relaxed = true)
    private val eligibilityManager: TangemPayEligibilityManager = mockk()
    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)
    private val produceInitialDataUseCase: ProduceTangemPayInitialDataUseCase = mockk(relaxed = true)
    private val urlOpener: UrlOpener = mockk(relaxed = true)
    private val tangemPayFeatureToggles: TangemPayFeatureToggles = mockk()

    private val deeplink = "tangem://onboard-visa"

    @BeforeEach
    fun resetMocks() {
        clearMocks(router, repository, eligibilityManager, analytics, produceInitialDataUseCase, urlOpener)
        every { tangemPayFeatureToggles.isTiersPlusPlanEnabled } returns false
    }

    @ParameterizedTest
    @MethodSource("provideAvailabilityCases")
    fun `GIVEN valid deeplink WHEN model created THEN state reflects tangem pay availability`(
        case: AvailabilityCase,
    ) = runTest {
        // Arrange
        coEvery { repository.validateDeeplink(deeplink) } returns true.right()
        coEvery { eligibilityManager.getTangemPayAvailability(TangemPayEntryPoint.DEEPLINK) } returns case.isAvailable

        // Act
        val model = createModel(TangemPayOnboardingComponent.Params.Deeplink(deeplink))
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value).isInstanceOf(case.expectedState)
        coVerify(exactly = 1) { eligibilityManager.getTangemPayAvailability(TangemPayEntryPoint.DEEPLINK) }
        model.onDestroy()
    }

    @ParameterizedTest
    @MethodSource("provideRejectedDeeplinkCases")
    fun `GIVEN deeplink rejected WHEN model created THEN screen closed AND availability not checked`(
        validationResult: Either<UniversalError, Boolean>,
    ) = runTest {
        // Arrange
        coEvery { repository.validateDeeplink(deeplink) } returns validationResult

        // Act
        val model = createModel(TangemPayOnboardingComponent.Params.Deeplink(deeplink))
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { router.pop() }
        coVerify(exactly = 0) { eligibilityManager.getTangemPayAvailability(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN availability check fails WHEN model created THEN screen closed`() = runTest {
        // Arrange
        coEvery { repository.validateDeeplink(deeplink) } returns true.right()
        coEvery { eligibilityManager.getTangemPayAvailability(TangemPayEntryPoint.DEEPLINK) } throws
            RuntimeException("network error")

        // Act
        val model = createModel(TangemPayOnboardingComponent.Params.Deeplink(deeplink))
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { eligibilityManager.getTangemPayAvailability(TangemPayEntryPoint.DEEPLINK) }
        verify(exactly = 1) { router.pop() }
        model.onDestroy()
    }

    @Test
    fun `GIVEN unavailable shown WHEN got it clicked THEN screen closed`() = runTest {
        // Arrange
        coEvery { repository.validateDeeplink(deeplink) } returns true.right()
        coEvery { eligibilityManager.getTangemPayAvailability(TangemPayEntryPoint.DEEPLINK) } returns false
        val model = createModel(TangemPayOnboardingComponent.Params.Deeplink(deeplink))
        advanceUntilIdle()

        // Act
        model.uiState.value.onBack.invoke()

        // Assert
        verify(exactly = 1) { router.pop() }
        model.onDestroy()
    }

    @Test
    fun `GIVEN from banner entry WHEN model created THEN content shown without availability check`() = runTest {
        // Act
        val model = createModel(TangemPayOnboardingComponent.Params.FromBannerOnMain)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value).isInstanceOf(TangemPayOnboardingScreenState.Content::class.java)
        coVerify(exactly = 0) { eligibilityManager.getTangemPayAvailability(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN MobileOnboardingDeeplink WHEN model created THEN shows onboarding without validating deeplink`() =
        runTest {
            // Act
            val model = createModel(TangemPayOnboardingComponent.Params.MobileOnboardingDeeplink)
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value).isInstanceOf(TangemPayOnboardingScreenState.Content::class.java)
            coVerify(exactly = 0) { repository.validateDeeplink(any()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN MobileOnboardingDeeplink WHEN get card clicked THEN uses possible wallets ignoring eligibility`() =
        runTest {
            // Arrange
            coEvery {
                eligibilityManager.getPossibleWalletsIds(shouldExcludePaeraCustomers = true)
            } returns emptyList()
            val model = createModel(TangemPayOnboardingComponent.Params.MobileOnboardingDeeplink)
            advanceUntilIdle()
            val content = model.uiState.value as TangemPayOnboardingScreenState.Content

            // Act
            content.buttonConfig.onClick.invoke()
            advanceUntilIdle()

            // Assert
            coVerify { eligibilityManager.getPossibleWalletsIds(shouldExcludePaeraCustomers = true) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN tiers off and KYC not approved WHEN onboarding starts THEN order created before KYC`() = runTest {
        // Arrange
        val userWalletId = UserWalletId("011")
        every { tangemPayFeatureToggles.isTiersPlusPlanEnabled } returns false
        coEvery { produceInitialDataUseCase(userWalletId) } returns Unit.right()
        coEvery { repository.getCustomerInfo(userWalletId) } returns
            buildCustomerInfo(kycStatus = KycStatus.PENDING).right()
        coEvery { repository.createOrder(userWalletId) } returns "order_1".right()

        // Act
        val model = createModel(TangemPayOnboardingComponent.Params.HotWalletOnboarding(userWalletId))
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { repository.createOrder(userWalletId) }
        verify(exactly = 1) {
            router.replaceAll(AppRoute.Wallet, AppRoute.Kyc(userWalletId = userWalletId), onComplete = any())
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN tiers on and KYC not approved WHEN onboarding starts THEN order not created before KYC`() = runTest {
        // Arrange
        val userWalletId = UserWalletId("011")
        every { tangemPayFeatureToggles.isTiersPlusPlanEnabled } returns true
        coEvery { produceInitialDataUseCase(userWalletId) } returns Unit.right()
        coEvery { repository.getCustomerInfo(userWalletId) } returns
            buildCustomerInfo(kycStatus = KycStatus.PENDING).right()

        // Act
        val model = createModel(TangemPayOnboardingComponent.Params.HotWalletOnboarding(userWalletId))
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { repository.createOrder(any()) }
        verify(exactly = 1) {
            router.replaceAll(AppRoute.Wallet, AppRoute.Kyc(userWalletId = userWalletId), onComplete = any())
        }
        model.onDestroy()
    }

    private fun buildCustomerInfo(kycStatus: KycStatus) = CustomerInfo(
        customerId = "cust_1",
        productInstances = emptyList(),
        cards = emptyList(),
        kycStatus = kycStatus,
        state = CustomerInfo.State.NEW,
        fiatBalance = null,
        cryptoBalance = null,
        availableForWithdrawal = BigDecimal.ZERO,
        tariffPlan = null,
    )

    private fun TestScope.createModel(params: TangemPayOnboardingComponent.Params): TangemPayOnboardingModel {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        return TangemPayOnboardingModel(
            paramsContainer = MutableParamsContainer(params),
            dispatchers = TestingCoroutineDispatcherProvider(
                main = testDispatcher,
                mainImmediate = testDispatcher,
                io = testDispatcher,
                default = testDispatcher,
                single = testDispatcher,
            ),
            analytics = analytics,
            router = router,
            repository = repository,
            produceInitialDataUseCase = produceInitialDataUseCase,
            urlOpener = urlOpener,
            eligibilityManager = eligibilityManager,
            tangemPayFeatureToggles = tangemPayFeatureToggles,
        )
    }

    internal data class AvailabilityCase(
        val isAvailable: Boolean,
        val expectedState: Class<out TangemPayOnboardingScreenState>,
    )

    private fun provideAvailabilityCases() = listOf(
        AvailabilityCase(
            isAvailable = true,
            expectedState = TangemPayOnboardingScreenState.Content::class.java,
        ),
        AvailabilityCase(
            isAvailable = false,
            expectedState = TangemPayOnboardingScreenState.NotAvailable::class.java,
        ),
    )

    private fun provideRejectedDeeplinkCases(): List<Either<UniversalError, Boolean>> = listOf(
        false.right(),
        DEEPLINK_ERROR.left(),
    )

    private companion object {
        val DEEPLINK_ERROR = object : UniversalError {
            override val errorCode: Int = 0
        }
    }
}