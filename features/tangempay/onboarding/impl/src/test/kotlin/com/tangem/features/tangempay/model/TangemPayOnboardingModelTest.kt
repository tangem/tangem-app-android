@file:OptIn(ExperimentalCoroutinesApi::class)

package com.tangem.features.tangempay.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.common.routing.AppRoute
import com.tangem.core.error.UniversalError
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayEligibilityManager
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.usecase.ProduceTangemPayInitialDataUseCase
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.features.tangempay.components.TangemPayOnboardingComponent
import com.tangem.features.tangempay.ui.TangemPayOnboardingNavigation
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
    private val userWalletsListRepository: UserWalletsListRepository = mockk()

    private val deeplink = "tangem://onboard-visa"

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            router,
            repository,
            eligibilityManager,
            analytics,
            produceInitialDataUseCase,
            urlOpener,
            userWalletsListRepository,
        )
        every { tangemPayFeatureToggles.isResidencyStepEnabled } returns false
        coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(mockk<UserWallet>())
    }

    @Test
    fun `GIVEN valid deeplink WHEN model created THEN onboarding shown AND availability not checked`() = runTest {
        // Arrange
        coEvery { repository.validateDeeplink(deeplink) } returns true.right()

        // Act
        val model = createModel(TangemPayOnboardingComponent.Params.Deeplink(deeplink))
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value).isInstanceOf(TangemPayOnboardingScreenState.Content::class.java)
        coVerify(exactly = 0) { eligibilityManager.getTangemPayAvailability(any()) }
        verify(exactly = 0) { router.replaceAll(AppRoute.TangemPayHotWalletOnboarding, onComplete = any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN valid deeplink AND no wallets WHEN model created THEN hot wallet onboarding opened`() = runTest {
        // Arrange
        coEvery { repository.validateDeeplink(deeplink) } returns true.right()
        coEvery { userWalletsListRepository.userWalletsSync() } returns emptyList()

        // Act
        val model = createModel(TangemPayOnboardingComponent.Params.Deeplink(deeplink))
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { router.replaceAll(AppRoute.TangemPayHotWalletOnboarding, onComplete = any()) }
        assertThat(model.uiState.value).isInstanceOf(TangemPayOnboardingScreenState.Loading::class.java)
        verify(exactly = 0) { router.push(any(), any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN MobileOnboardingDeeplink AND no wallets WHEN model created THEN onboarding shown as before`() =
        runTest {
            // Arrange
            coEvery { userWalletsListRepository.userWalletsSync() } returns emptyList()

            // Act
            val model = createModel(TangemPayOnboardingComponent.Params.MobileOnboardingDeeplink)
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value).isInstanceOf(TangemPayOnboardingScreenState.Content::class.java)
            verify(exactly = 0) { router.replaceAll(AppRoute.TangemPayHotWalletOnboarding, onComplete = any()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN invalid deeplink WHEN model created THEN sorry screen shown AND screen not closed`() = runTest {
        // Arrange
        coEvery { repository.validateDeeplink(deeplink) } returns false.right()

        // Act
        val model = createModel(TangemPayOnboardingComponent.Params.Deeplink(deeplink))
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value).isInstanceOf(TangemPayOnboardingScreenState.NotAvailable::class.java)
        verify(exactly = 0) { router.pop() }
        coVerify(exactly = 0) { eligibilityManager.getTangemPayAvailability(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN deeplink validation error WHEN model created THEN screen closed AND availability not checked`() =
        runTest {
            // Arrange
            coEvery { repository.validateDeeplink(deeplink) } returns DEEPLINK_ERROR.left()

            // Act
            val model = createModel(TangemPayOnboardingComponent.Params.Deeplink(deeplink))
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { router.pop() }
            coVerify(exactly = 0) { eligibilityManager.getTangemPayAvailability(any()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN sorry screen shown WHEN got it clicked THEN screen closed`() = runTest {
        // Arrange
        coEvery { repository.validateDeeplink(deeplink) } returns false.right()
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
                eligibilityManager.getCompatibleWalletsIds(shouldExcludePaeraCustomers = true)
            } returns emptyList()
            val model = createModel(TangemPayOnboardingComponent.Params.MobileOnboardingDeeplink)
            advanceUntilIdle()
            val content = model.uiState.value as TangemPayOnboardingScreenState.Content

            // Act
            content.buttonConfig.onClick.invoke()
            advanceUntilIdle()

            // Assert
            coVerify { eligibilityManager.getCompatibleWalletsIds(shouldExcludePaeraCustomers = true) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN residency step enabled AND single wallet WHEN get card clicked THEN residency step opened for it`() =
        runTest {
            // Arrange
            every { tangemPayFeatureToggles.isResidencyStepEnabled } returns true
            val userWalletId = UserWalletId("011")
            coEvery { eligibilityManager.getCompatibleWalletsIds(shouldExcludePaeraCustomers = true) } returns
                listOf(userWalletId)
            coEvery { produceInitialDataUseCase(userWalletId) } returns Unit.right()
            coEvery { repository.getCustomerInfo(userWalletId) } returns
                buildCustomerInfo(kycStatus = KycStatus.PENDING).right()
            val model = createModel(TangemPayOnboardingComponent.Params.FromBannerOnMain)
            advanceUntilIdle()
            val content = model.uiState.value as TangemPayOnboardingScreenState.Content

            // Act
            content.buttonConfig.onClick.invoke()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { produceInitialDataUseCase(userWalletId) }
            verify(exactly = 1) {
                router.replaceAll(
                    AppRoute.Wallet,
                    AppRoute.TangemPayEligibility(
                        product = TangemPayProduct.PAYMENT_ACCOUNT,
                        userWalletId = userWalletId,
                    ),
                    onComplete = any(),
                )
            }
            model.onDestroy()
        }

    @Test
    fun `GIVEN residency step enabled AND several wallets WHEN get card clicked THEN wallet selector opened`() =
        runTest {
            // Arrange
            every { tangemPayFeatureToggles.isResidencyStepEnabled } returns true
            val walletsIds = listOf(UserWalletId("011"), UserWalletId("022"))
            coEvery { eligibilityManager.getCompatibleWalletsIds(shouldExcludePaeraCustomers = true) } returns
                walletsIds
            val model = createModel(TangemPayOnboardingComponent.Params.FromBannerOnMain)
            val openedSheets = mutableListOf<TangemPayOnboardingNavigation?>()
            model.bottomSheetNavigation.subscribe { event -> openedSheets += event.transformer(null) }
            advanceUntilIdle()
            val content = model.uiState.value as TangemPayOnboardingScreenState.Content

            // Act
            content.buttonConfig.onClick.invoke()
            advanceUntilIdle()

            // Assert
            assertThat(openedSheets).containsExactly(TangemPayOnboardingNavigation.WalletSelector(walletsIds))
            coVerify(exactly = 0) { produceInitialDataUseCase(any()) }
            verify(exactly = 0) { router.replaceAll(routes = anyVararg(), onComplete = any()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN residency step enabled AND wallet selector shown WHEN wallet selected THEN residency step opened`() =
        runTest {
            // Arrange
            every { tangemPayFeatureToggles.isResidencyStepEnabled } returns true
            val userWalletId = UserWalletId("022")
            coEvery { eligibilityManager.getCompatibleWalletsIds(shouldExcludePaeraCustomers = true) } returns
                listOf(UserWalletId("011"), userWalletId)
            coEvery { produceInitialDataUseCase(userWalletId) } returns Unit.right()
            coEvery { repository.getCustomerInfo(userWalletId) } returns
                buildCustomerInfo(kycStatus = KycStatus.PENDING).right()
            val model = createModel(TangemPayOnboardingComponent.Params.FromBannerOnMain)
            advanceUntilIdle()
            (model.uiState.value as TangemPayOnboardingScreenState.Content).buttonConfig.onClick.invoke()
            advanceUntilIdle()

            // Act
            model.onWalletSelected(userWalletId)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) {
                router.replaceAll(
                    AppRoute.Wallet,
                    AppRoute.TangemPayEligibility(
                        product = TangemPayProduct.PAYMENT_ACCOUNT,
                        userWalletId = userWalletId,
                    ),
                    onComplete = any(),
                )
            }
            model.onDestroy()
        }

    @Test
    fun `GIVEN residency step enabled AND no wallets WHEN get card clicked THEN screen closed`() = runTest {
        // Arrange
        every { tangemPayFeatureToggles.isResidencyStepEnabled } returns true
        coEvery { eligibilityManager.getCompatibleWalletsIds(shouldExcludePaeraCustomers = true) } returns emptyList()
        val model = createModel(TangemPayOnboardingComponent.Params.FromBannerOnMain)
        advanceUntilIdle()
        val content = model.uiState.value as TangemPayOnboardingScreenState.Content

        // Act
        content.buttonConfig.onClick.invoke()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { router.pop(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN KYC not approved WHEN onboarding starts THEN order not created before KYC`() = runTest {
        // Arrange
        val userWalletId = UserWalletId("011")
        coEvery { produceInitialDataUseCase(userWalletId) } returns Unit.right()
        coEvery { repository.getCustomerInfo(userWalletId) } returns
            buildCustomerInfo(kycStatus = KycStatus.PENDING).right()

        // Act
        val model = createModel(TangemPayOnboardingComponent.Params.HotWalletOnboarding(userWalletId))
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { repository.createOrder(any(), any()) }
        verify(exactly = 1) {
            router.replaceAll(AppRoute.Wallet, AppRoute.Kyc(userWalletId = userWalletId), onComplete = any())
        }
        model.onDestroy()
    }

    private fun buildCustomerInfo(kycStatus: KycStatus) = CustomerInfo(
        customerId = "cust_1",
        paymentAccount = null,
        productInstances = emptyList(),
        cards = emptyList(),
        kycStatus = kycStatus,
        state = CustomerInfo.State.NEW,
        fiatBalance = null,
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
            urlOpener = urlOpener,
            eligibilityManager = eligibilityManager,
            featureToggles = tangemPayFeatureToggles,
            onboardingStarter = TangemPayOnboardingStarter(
                router = router,
                uiMessageSender = mockk(relaxed = true),
                repository = repository,
                produceInitialDataUseCase = produceInitialDataUseCase,
                getCustomerOffersUseCase = mockk(relaxed = true),
                createHotWalletUseCase = mockk(relaxed = true),
                isHotWalletCreationSupported = mockk(relaxed = true),
                tangemPayFeatureToggles = tangemPayFeatureToggles,
            ),
            userWalletsListRepository = userWalletsListRepository,
        )
    }

    private companion object {
        val DEEPLINK_ERROR = object : UniversalError {
            override val errorCode: Int = 0
        }
    }
}