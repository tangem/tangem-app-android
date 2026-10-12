package com.tangem.features.hotwallet.wallethardwarebackup.model

import arrow.core.right
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.utils.TrackingContextProxy
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.sociallogin.SocialLoginFeatureToggles
import com.tangem.domain.wallets.analytics.WalletSettingsAnalyticEvents
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.wallets.usecase.IsWalletBackedUpUseCase
import com.tangem.features.hotwallet.HotWalletFeatureToggles
import com.tangem.features.hotwallet.WalletHardwareBackupComponent
import com.tangem.hot.sdk.model.HotWalletId
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class WalletHardwareBackupModelTest {

    private val router: Router = mockk(relaxUnitFun = true)
    private val getUserWalletUseCase: GetUserWalletUseCase = mockk()
    private val messageSender: UiMessageSender = mockk(relaxUnitFun = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxUnitFun = true)
    private val hotWalletFeatureToggles: HotWalletFeatureToggles = mockk()
    private val isWalletBackedUpUseCase: IsWalletBackedUpUseCase = mockk()
    private val socialLoginFeatureToggles: SocialLoginFeatureToggles = mockk()
    private val paramsContainer: ParamsContainer = mockk {
        every { require<WalletHardwareBackupComponent.Params>() } returns
            WalletHardwareBackupComponent.Params(userWalletId = WALLET_ID)
    }
    private val hotWalletId: HotWalletId = mockk {
        every { authType } returns HotWalletId.AuthType.NoPassword
    }
    private val hotWallet: UserWallet.Hot = mockk {
        every { walletId } returns WALLET_ID
        every { hotWalletId } returns this@WalletHardwareBackupModelTest.hotWalletId
    }

    @BeforeEach
    fun setUp() {
        clearMocks(router, messageSender, analyticsEventHandler, answers = false)
        every { getUserWalletUseCase.invoke(WALLET_ID) } returns hotWallet.right()
        every { hotWalletFeatureToggles.isGoogleDriveBackupEnabled } returns false
        every { socialLoginFeatureToggles.isSocialLoginEnabled } returns true
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `GIVEN no backup WHEN upgrading THEN the social backup is reported only with the toggle on`(
        isSocialLoginEnabled: Boolean,
    ) = runTest {
        // Arrange
        every { socialLoginFeatureToggles.isSocialLoginEnabled } returns isSocialLoginEnabled
        coEvery { isWalletBackedUpUseCase(hotWallet) } returns false
        val model = createModel(this)

        // Act
        model.uiState.value.blocks[UPGRADE_BLOCK].onClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            analyticsEventHandler.send(
                WalletSettingsAnalyticEvents.NoticeBackupFirst(
                    source = AnalyticsParam.ScreensSources.HardwareWallet.value,
                    action = WalletSettingsAnalyticEvents.NoticeBackupFirst.Action.Upgrade,
                    isSocialBackedUp = false.takeIf { isSocialLoginEnabled },
                ),
            )
        }
        verify(exactly = 0) { router.push(ofType<AppRoute.UpgradeWallet>(), any()) }
    }

    @Test
    fun `GIVEN a backed up wallet WHEN upgrading THEN the upgrade opens without the notice`() = runTest {
        // Arrange
        coEvery { isWalletBackedUpUseCase(hotWallet) } returns true
        val model = createModel(this)

        // Act
        model.uiState.value.blocks[UPGRADE_BLOCK].onClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { router.push(AppRoute.UpgradeWallet(userWalletId = WALLET_ID), any()) }
        verify(exactly = 0) { analyticsEventHandler.send(ofType<WalletSettingsAnalyticEvents.NoticeBackupFirst>()) }
    }

    private fun createModel(testScope: TestScope) = WalletHardwareBackupModel(
        paramsContainer = paramsContainer,
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        router = router,
        generateBuyTangemCardLinkUseCase = mockk(),
        unlockHotWalletContextualUseCase = mockk(),
        urlOpener = mockk(),
        getUserWalletUseCase = getUserWalletUseCase,
        messageSender = messageSender,
        trackingContextProxy = mockk(relaxUnitFun = true),
        analyticsEventHandler = analyticsEventHandler,
        hotWalletFeatureToggles = hotWalletFeatureToggles,
        getCloudBackupStatusUseCase = mockk(),
        isWalletBackedUpUseCase = isWalletBackedUpUseCase,
        socialLoginFeatureToggles = socialLoginFeatureToggles,
    )

    private companion object {
        val WALLET_ID = UserWalletId("011")
        const val UPGRADE_BLOCK = 1
    }
}