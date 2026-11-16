package com.tangem.features.allservices.impl.entries.model

import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.jointaccount.JointAccountFeatureToggles
import com.tangem.features.jointaccount.analytics.JointAccountAnalyticsEvent
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class JointAccountEntryModelTest {

    private val router: Router = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val jointAccountFeatureToggles: JointAccountFeatureToggles = mockk()

    private val userWallets = MutableStateFlow<List<UserWallet>?>(null)

    @BeforeEach
    fun resetMocks() {
        clearMocks(router, analyticsEventHandler, userWalletsListRepository, jointAccountFeatureToggles)
        every { userWalletsListRepository.userWallets } returns userWallets
        every { jointAccountFeatureToggles.isJointAccountEnabled } returns true
    }

    @Test
    fun `GIVEN an unlocked hot wallet WHEN model created THEN visible from the start`() = runTest {
        // Arrange
        userWallets.value = listOf(hotWallet(isLocked = false))

        // Act
        val model = createModel()

        // Assert
        assertThat(model.isVisible.value).isTrue()
        model.onDestroy()
    }

    @Test
    fun `GIVEN only a locked wallet WHEN model created THEN hidden`() = runTest {
        // Arrange
        userWallets.value = listOf(hotWallet(isLocked = true))

        // Act
        val model = createModel()

        // Assert
        assertThat(model.isVisible.value).isFalse()
        model.onDestroy()
    }

    @Test
    fun `GIVEN joint accounts disabled WHEN model created THEN hidden`() = runTest {
        // Arrange
        every { jointAccountFeatureToggles.isJointAccountEnabled } returns false
        userWallets.value = listOf(hotWallet(isLocked = false))

        // Act
        val model = createModel()

        // Assert
        assertThat(model.isVisible.value).isFalse()
        model.onDestroy()
    }

    @Test
    fun `GIVEN entry shown WHEN clicked THEN button event sent and flow opened without a wallet`() = runTest {
        // Arrange
        userWallets.value = listOf(hotWallet(isLocked = false))
        val model = createModel()
        val source = AppRoute.JointAccountCreation.Source.AllServices

        // Act
        model.onClick()

        // Assert
        verify(exactly = 1) {
            analyticsEventHandler.send(JointAccountAnalyticsEvent.ButtonCreateJointAccount(source = source))
        }
        verify(exactly = 1) { router.push(AppRoute.JointAccountCreation(userWalletId = null, source = source)) }
        model.onDestroy()
    }

    private fun TestScope.createModel() = JointAccountEntryModel(
        dispatchers = createTestingCoroutineDispatcherProvider(),
        router = router,
        analyticsEventHandler = analyticsEventHandler,
        userWalletsListRepository = userWalletsListRepository,
        jointAccountFeatureToggles = jointAccountFeatureToggles,
    )

    private fun hotWallet(isLocked: Boolean) = UserWallet.Hot(
        name = "Hot",
        walletId = UserWalletId("011"),
        hotWalletId = HotWalletId(value = "hot", authType = HotWalletId.AuthType.NoPassword),
        wallets = if (isLocked) null else emptyList(),
        isBackedUp = false,
    )
}