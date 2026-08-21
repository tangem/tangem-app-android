package com.tangem.features.details.model

import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.settings.HotWalletRestrictionManager
import com.tangem.domain.wallets.usecase.ApplyUserWalletListSortingUseCase
import com.tangem.domain.wallets.usecase.UnlockWalletUseCase
import com.tangem.features.details.entity.AddWalletBS
import com.tangem.features.details.utils.UserWalletSaver
import com.tangem.features.wallet.utils.UserWalletsFetcher
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class UserWalletListModelAddWalletTest {

    private val userWalletsFetcherFactory: UserWalletsFetcher.Factory = mockk()
    private val userWalletsFetcher: UserWalletsFetcher = mockk()
    private val router: Router = mockk(relaxUnitFun = true)
    private val messageSender: UiMessageSender = mockk(relaxUnitFun = true)
    private val userWalletSaver: UserWalletSaver = mockk(relaxUnitFun = true)
    private val hotWalletRestrictionManager: HotWalletRestrictionManager = mockk()
    private val unlockWalletUseCase: UnlockWalletUseCase = mockk()
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxUnitFun = true)
    private val applyUserWalletListSortingUseCase: ApplyUserWalletListSortingUseCase = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()

    @BeforeEach
    fun setUp() {
        clearMocks(
            userWalletsFetcherFactory,
            userWalletsFetcher,
            router,
            messageSender,
            userWalletSaver,
            hotWalletRestrictionManager,
            unlockWalletUseCase,
            analyticsEventHandler,
            applyUserWalletListSortingUseCase,
            userWalletsListRepository,
        )

        every { userWalletsFetcher.userWallets } returns flowOf(persistentListOf())
        every { userWalletsFetcherFactory.create(any(), any(), any(), any(), any()) } returns userWalletsFetcher
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(coldWallet()))
        setCreationRestriction(isEnabled = true)
    }

    @Test
    fun `GIVEN creation restricted WHEN add new wallet clicked THEN add wallet sheet shown`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.state.value.onAddNewWalletClick()

        // Assert
        assertThat(model.state.value.addWalletBSConfig.isShown).isTrue()
        assertThat(model.state.value.addWalletBSConfig.content).isInstanceOf(AddWalletBS::class.java)
        verify(exactly = 0) { router.push(any()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN creation not restricted WHEN add new wallet clicked THEN create wallet selection opened`() = runTest {
        // Arrange
        setCreationRestriction(isEnabled = false)
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.state.value.onAddNewWalletClick()

        // Assert
        verify(exactly = 1) { router.push(AppRoute.CreateWalletSelection) }
        assertThat(model.state.value.addWalletBSConfig.isShown).isFalse()

        model.onDestroy()
    }

    @Test
    fun `GIVEN sheet shown WHEN add hardware wallet clicked THEN card scanned and sheet hidden`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.state.value.onAddNewWalletClick()

        // Act
        model.addWalletSheetContent().onAddHardwareWalletClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { userWalletSaver.scanAndSaveUserWallet(any()) }
        assertThat(model.state.value.addWalletBSConfig.isShown).isFalse()
        assertThat(sentEvents().map(AnalyticsEvent::id))
            .contains("[Settings] Button - Add Hardware Wallet")

        model.onDestroy()
    }

    @Test
    fun `GIVEN sheet shown WHEN add mobile wallet clicked THEN coming soon dialog shown`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.state.value.onAddNewWalletClick()

        // Act
        model.addWalletSheetContent().onAddMobileWalletClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { messageSender.send(any<DialogMessage>()) }
        coVerify(exactly = 0) { userWalletSaver.scanAndSaveUserWallet(any()) }
        assertThat(model.state.value.addWalletBSConfig.isShown).isFalse()
        assertThat(sentEvents().map(AnalyticsEvent::id)).containsAtLeast(
            "[Settings] Button - Add Mobile Wallet",
            "[Settings] Notice - More Mobile Wallets",
        )

        model.onDestroy()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `wallets analytics param`(model: WalletsParamModel) = runTest {
        // Arrange
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(model.userWallets)
        val listModel = createModel(testScope = this)
        advanceUntilIdle()
        listModel.state.value.onAddNewWalletClick()

        // Act
        listModel.addWalletSheetContent().onAddMobileWalletClick()
        advanceUntilIdle()

        // Assert
        val walletsParams = sentEvents()
            .filter { it.id == "[Settings] Notice - More Mobile Wallets" }
            .map { it.params["Wallets"] }
        assertThat(walletsParams).containsExactly(model.expected)

        listModel.onDestroy()
    }

    private fun provideTestModels() = listOf(
        WalletsParamModel(userWallets = listOf(hotWallet()), expected = "Mobile"),
        WalletsParamModel(userWallets = listOf(coldWallet(), coldWallet()), expected = "Cold"),
        WalletsParamModel(userWallets = listOf(hotWallet(), coldWallet()), expected = "Multiple"),
        WalletsParamModel(userWallets = emptyList(), expected = "Unknown"),
    )

    internal data class WalletsParamModel(val userWallets: List<UserWallet>, val expected: String)

    private fun setCreationRestriction(isEnabled: Boolean) {
        every { hotWalletRestrictionManager.isCreationEnabled() } returns MutableStateFlow(isEnabled)
    }

    private fun sentEvents(): List<AnalyticsEvent> {
        val events = mutableListOf<AnalyticsEvent>()
        verify { analyticsEventHandler.send(capture(events)) }
        return events
    }

    private fun UserWalletListModel.addWalletSheetContent(): AddWalletBS =
        state.value.addWalletBSConfig.content as AddWalletBS

    private fun coldWallet(): UserWallet = mockk<UserWallet.Cold>()

    private fun hotWallet(): UserWallet = mockk<UserWallet.Hot>()

    private fun createModel(testScope: TestScope): UserWalletListModel {
        val testDispatcher = StandardTestDispatcher(testScope.testScheduler)

        return UserWalletListModel(
            userWalletsFetcherFactory = userWalletsFetcherFactory,
            router = router,
            messageSender = messageSender,
            dispatchers = TestingCoroutineDispatcherProvider(
                main = testDispatcher,
                mainImmediate = testDispatcher,
                io = testDispatcher,
                default = testDispatcher,
                single = testDispatcher,
            ),
            userWalletSaver = userWalletSaver,
            hotWalletRestrictionManager = hotWalletRestrictionManager,
            unlockWalletUseCase = unlockWalletUseCase,
            analyticsEventHandler = analyticsEventHandler,
            applyUserWalletListSortingUseCase = applyUserWalletListSortingUseCase,
            userWalletsListRepository = userWalletsListRepository,
        )
    }
}