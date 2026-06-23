package com.tangem.features.home.impl.model

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.entity.InitScreenLaunchMode
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.card.ScanCardProcessor
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.settings.usercountry.GetUserCountryUseCase
import com.tangem.domain.settings.usercountry.models.UserCountry
import com.tangem.domain.wallets.builder.ColdUserWalletBuilder
import com.tangem.domain.wallets.usecase.SaveWalletUseCase
import com.tangem.feature.referral.domain.ShouldShowMobileWalletPromoUseCase
import com.tangem.features.home.api.HomeComponent
import com.tangem.features.home.api.HomeFeatureToggles
import com.tangem.features.home.impl.ui.state.Stories
import com.tangem.features.home.impl.ui.state.getRestrictedStories
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class HomeModelTest {

    private val scanCardProcessor: ScanCardProcessor = mockk()
    private val cardSdkConfigRepository: CardSdkConfigRepository = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk()
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val router: Router = mockk(relaxed = true)
    private val getUserCountryUseCase: GetUserCountryUseCase = mockk()
    private val coldUserWalletBuilderFactory: ColdUserWalletBuilder.Factory = mockk(relaxed = true)
    private val saveWalletUseCase: SaveWalletUseCase = mockk(relaxed = true)
    private val userWalletsListRepository: UserWalletsListRepository = mockk(relaxed = true)
    private val shouldShowMobileWalletPromoUseCase: ShouldShowMobileWalletPromoUseCase = mockk(relaxed = true)
    private val homeFeatureToggles: HomeFeatureToggles = mockk()
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)

    private val progressSlot = slot<suspend (Boolean) -> Unit>()

    @BeforeEach
    fun setUp() {
        every { homeFeatureToggles.isStoriesContainerEnabled } returns false
        every { getUserCountryUseCase.invoke() } returns emptyFlow()
        coEvery { settingsRepository.shouldSaveAccessCodes() } returns false
        coEvery {
            scanCardProcessor.scan(
                analyticsSource = any(),
                shouldCheckIsAlreadyActivated = any(),
                cardId = any(),
                onProgressStateChange = capture(progressSlot),
                onWalletNotCreated = any(),
                onCancel = any(),
                onFailure = any(),
                onSuccess = any(),
            )
        } just Runs
    }

    @Test
    fun `GIVEN toggle enabled WHEN model created THEN isStoriesContainerEnabled is true`() = runTest {
        // Arrange
        every { homeFeatureToggles.isStoriesContainerEnabled } returns true

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.isStoriesContainerEnabled).isTrue()
        model.onDestroy()
    }

    @Test
    fun `GIVEN toggle disabled WHEN model created THEN isStoriesContainerEnabled is false`() = runTest {
        // Arrange
        every { homeFeatureToggles.isStoriesContainerEnabled } returns false

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.isStoriesContainerEnabled).isFalse()
        model.onDestroy()
    }

    @Test
    fun `GIVEN model created WHEN no country emitted THEN storiesConfig is non-closable looping and in sync`() =
        runTest {
            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            val state = model.uiState.value
            assertThat(state.storiesConfig.isRestartable).isTrue()
            assertThat(state.storiesConfig.isCloseButtonVisible).isFalse()
            assertThat(state.storiesConfig.stories).isEqualTo(state.stories)
            assertThat(state.stories).containsExactlyElementsIn(getRestrictedStories()).inOrder()
            model.onDestroy()
        }

    @Test
    fun `GIVEN FCA-restricted country WHEN model created THEN Currencies excluded and config in sync`() = runTest {
        // Arrange
        every { getUserCountryUseCase.invoke() } returns flowOf(UserCountry.Other(code = "GB").right())

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        val state = model.uiState.value
        assertThat(state.stories).containsExactlyElementsIn(getRestrictedStories()).inOrder()
        assertThat(state.stories).doesNotContain(Stories.Currencies)
        assertThat(state.storiesConfig.stories).isEqualTo(state.stories)
        model.onDestroy()
    }

    @Test
    fun `GIVEN non-restricted country WHEN model created THEN all stories shown and config in sync`() = runTest {
        // Arrange
        every { getUserCountryUseCase.invoke() } returns flowOf(UserCountry.Russia.right())

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        val state = model.uiState.value
        assertThat(state.stories).containsExactlyElementsIn(Stories.entries).inOrder()
        assertThat(state.storiesConfig.stories).isEqualTo(state.stories)
        model.onDestroy()
    }

    @Test
    fun `GIVEN scan in progress WHEN loading toggles THEN storiesConfig instance is not replaced`() = runTest {
        // Arrange
        val model = createModel(testScope = this, launchMode = InitScreenLaunchMode.WithCardScan)
        advanceUntilIdle()
        val initialConfig = model.uiState.value.storiesConfig

        // Act + Assert — loading on
        progressSlot.captured.invoke(true)
        advanceUntilIdle()
        assertThat(model.uiState.value.scanInProgress).isTrue()
        assertThat(model.uiState.value.storiesConfig).isSameInstanceAs(initialConfig)

        // Act + Assert — loading off
        progressSlot.captured.invoke(false)
        advanceUntilIdle()
        assertThat(model.uiState.value.scanInProgress).isFalse()
        assertThat(model.uiState.value.storiesConfig).isSameInstanceAs(initialConfig)

        model.onDestroy()
    }

    private fun createModel(
        testScope: TestScope,
        launchMode: InitScreenLaunchMode = InitScreenLaunchMode.Standard,
        paramsContainer: ParamsContainer = MutableParamsContainer(
            value = HomeComponent.Params(launchMode = launchMode),
        ),
    ): HomeModel {
        return HomeModel(
            paramsContainer = paramsContainer,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            scanCardProcessor = scanCardProcessor,
            cardSdkConfigRepository = cardSdkConfigRepository,
            settingsRepository = settingsRepository,
            analyticsEventHandler = analyticsEventHandler,
            router = router,
            getUserCountryUseCase = getUserCountryUseCase,
            coldUserWalletBuilderFactory = coldUserWalletBuilderFactory,
            saveWalletUseCase = saveWalletUseCase,
            userWalletsListRepository = userWalletsListRepository,
            shouldShowMobileWalletPromoUseCase = shouldShowMobileWalletPromoUseCase,
            homeFeatureToggles = homeFeatureToggles,
            uiMessageSender = uiMessageSender,
        )
    }

    private fun TestScope.createTestingCoroutineDispatcherProvider(): TestingCoroutineDispatcherProvider {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        return TestingCoroutineDispatcherProvider(
            main = testDispatcher,
            mainImmediate = testDispatcher,
            io = testDispatcher,
            default = testDispatcher,
            single = testDispatcher,
        )
    }
}