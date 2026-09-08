package com.tangem.features.tangempay.campaign

import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.features.tangempay.components.TangemPayCampaignComponent
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class TangemPayCampaignModelTest {

    private val router: Router = mockk(relaxed = true)
    private val userWalletsListRepository: UserWalletsListRepository = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(router, userWalletsListRepository)
    }

    @Test
    fun `GIVEN campaign key WHEN model created THEN state is Loading`() = runTest {
        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value).isEqualTo(TangemPayCampaignUM.Loading)
        model.onDestroy()
    }

    @Test
    fun `GIVEN no wallets WHEN onClose THEN replaces all with Home`() = runTest {
        // Arrange
        coEvery { userWalletsListRepository.userWalletsSync() } returns emptyList()
        val model = createModel(testScope = this)

        // Act
        model.onClose()
        advanceUntilIdle()

        // Assert
        verify { router.replaceAll(AppRoute.Home()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN wallets present WHEN onClose THEN replaces all with Wallet`() = runTest {
        // Arrange
        coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(mockk<UserWallet>())
        val model = createModel(testScope = this)

        // Act
        model.onClose()
        advanceUntilIdle()

        // Assert
        verify { router.replaceAll(AppRoute.Wallet) }
        model.onDestroy()
    }

    private fun createModel(testScope: TestScope): TangemPayCampaignModel {
        val testDispatcher = StandardTestDispatcher(testScope.testScheduler)
        return TangemPayCampaignModel(
            paramsContainer = MutableParamsContainer(
                TangemPayCampaignComponent.Params(campaignKey = CAMPAIGN_KEY),
            ),
            dispatchers = TestingCoroutineDispatcherProvider(
                main = testDispatcher,
                mainImmediate = testDispatcher,
                io = testDispatcher,
                default = testDispatcher,
                single = testDispatcher,
            ),
            router = router,
            userWalletsListRepository = userWalletsListRepository,
        )
    }

    private companion object {
        const val CAMPAIGN_KEY = "tpay_for_oxford"
    }
}