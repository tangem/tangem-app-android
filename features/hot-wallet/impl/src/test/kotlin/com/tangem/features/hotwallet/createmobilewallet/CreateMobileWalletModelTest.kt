package com.tangem.features.hotwallet.createmobilewallet

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.event.OnboardingAnalyticsEvent
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessage
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.dialog.Dialogs
import com.tangem.domain.appsflyer.repository.AppsFlyerRepository
import com.tangem.domain.hotwallet.IsHotWalletCreationSupported
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.analytics.Settings.NoticeMobileWalletLimit
import com.tangem.domain.wallets.models.CreateHotWalletError
import com.tangem.domain.wallets.usecase.CreateHotWalletUseCase
import com.tangem.features.hotwallet.CreateMobileWalletComponent
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class CreateMobileWalletModelTest {

    private val paramsContainer: ParamsContainer = mockk()
    private val createHotWalletUseCase: CreateHotWalletUseCase = mockk()
    private val router: Router = mockk(relaxUnitFun = true)
    private val isHotWalletCreationSupported: IsHotWalletCreationSupported = mockk()
    private val uiMessageSender: UiMessageSender = mockk(relaxUnitFun = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val appsFlyerRepository: AppsFlyerRepository = mockk(relaxed = true)

    @BeforeEach
    fun setUp() {
        clearMocks(
            paramsContainer,
            createHotWalletUseCase,
            router,
            isHotWalletCreationSupported,
            uiMessageSender,
            analyticsEventHandler,
            appsFlyerRepository,
        )
        every { paramsContainer.require<CreateMobileWalletComponent.Params>() } returns
            CreateMobileWalletComponent.Params(source = AnalyticsParam.ScreensSources.Intro)
        every { isHotWalletCreationSupported() } returns true
    }

    @Test
    fun `GIVEN creation failed WHEN create clicked THEN error dialog sent and user stays in flow`() = runTest {
        // Arrange
        coEvery { createHotWalletUseCase(any(), any()) } returns CreateHotWalletError.Failed().left()
        val sentMessage = slot<UiMessage>()
        every { uiMessageSender.send(capture(sentMessage)) } returns Unit
        val model = createModel(testScope = this)

        // Act
        model.uiState.value.onCreateClick()
        advanceUntilIdle()

        // Assert
        val actual = sentMessage.captured as DialogMessage
        assertThat(actual.title).isEqualTo(resourceReference(R.string.common_something_went_wrong))
        assertThat(actual.message).isEqualTo(resourceReference(R.string.common_unknown_error))
        assertThat(model.uiState.value.isCreateButtonLoading).isFalse()
        verify(exactly = 0) { router.replaceAll(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN limit reached WHEN create clicked THEN limit reported and limit dialog sent`() = runTest {
        // Arrange
        coEvery { createHotWalletUseCase(any(), any()) } returns CreateHotWalletError.LimitReached.left()
        val sentMessage = slot<UiMessage>()
        every { uiMessageSender.send(capture(sentMessage)) } returns Unit
        val model = createModel(testScope = this)

        // Act
        model.uiState.value.onCreateClick()
        advanceUntilIdle()

        // Assert
        val expected = Dialogs.hotWalletsLimitReachedDialog()
        val actual = sentMessage.captured as DialogMessage
        assertThat(actual.message).isEqualTo(expected.message)
        assertThat(actual.title).isEqualTo(expected.title)
        verify(exactly = 1) {
            analyticsEventHandler.send(
                NoticeMobileWalletLimit(
                    type = NoticeMobileWalletLimit.Type.Create,
                    limit = NoticeMobileWalletLimit.Limit.MobileWallets,
                ),
            )
        }
        assertThat(model.uiState.value.isCreateButtonLoading).isFalse()
        verify(exactly = 0) { router.replaceAll(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN referral read throws WHEN wallet created THEN created event sent and wallet opened`() = runTest {
        // Arrange
        coEvery { createHotWalletUseCase(any(), any()) } returns USER_WALLET.right()
        coEvery { appsFlyerRepository.getConversionData() } throws IllegalStateException("corrupt store")
        val model = createModel(testScope = this)

        // Act
        model.uiState.value.onCreateClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            analyticsEventHandler.send(ofType<OnboardingAnalyticsEvent.CreateWallet.WalletCreatedSuccessfully>())
        }
        verify(exactly = 1) { router.replaceAll(AppRoute.Wallet) }
        model.onDestroy()
    }

    private fun createModel(testScope: TestScope): CreateMobileWalletModel = CreateMobileWalletModel(
        paramsContainer = paramsContainer,
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        createHotWalletUseCase = createHotWalletUseCase,
        router = router,
        trackingContextProxy = mockk(relaxed = true),
        isHotWalletCreationSupported = isHotWalletCreationSupported,
        uiMessageSender = uiMessageSender,
        analyticsEventHandler = analyticsEventHandler,
        appsFlyerRepository = appsFlyerRepository,
        hotWalletFeatureToggles = mockk(relaxed = true),
    )

    private companion object {
        val USER_WALLET = UserWallet.Hot(
            name = "Hot",
            walletId = UserWalletId("0A0B"),
            hotWalletId = HotWalletId(value = "hot", authType = HotWalletId.AuthType.NoPassword),
            wallets = null,
            isBackedUp = false,
        )
    }
}