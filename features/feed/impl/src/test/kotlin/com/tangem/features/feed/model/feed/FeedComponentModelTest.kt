package com.tangem.features.feed.model.feed

import android.text.format.DateFormat
import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.ui.DesignFeatureToggles
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.earn.usecase.FetchTopEarnTokensUseCase
import com.tangem.domain.earn.usecase.GetTopEarnTokensUseCase
import com.tangem.domain.markets.GetTopFiveMarketTokenUseCase
import com.tangem.domain.news.usecase.FetchTrendingNewsUseCase
import com.tangem.domain.news.usecase.ManageTrendingNewsUseCase
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioManager
import com.tangem.features.feed.components.feed.DefaultFeedComponent.FeedParams
import com.tangem.features.feed.model.feed.state.FeedStateController
import com.tangem.features.feed.ui.feed.state.ForYouBannerUM
import com.tangem.features.foryou.ForYouFeatureToggles
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FeedComponentModelTest {

    // --- shared mocks ---
    private val fetchTrendingNewsUseCase: FetchTrendingNewsUseCase = mockk(relaxed = true)
    private val manageTrendingNewsUseCase: ManageTrendingNewsUseCase = mockk()
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val fetchTopEarnTokensUseCase: FetchTopEarnTokensUseCase = mockk(relaxed = true)
    private val getTopEarnTokensUseCase: GetTopEarnTokensUseCase = mockk()
    private val appRouter: AppRouter = mockk(relaxed = true)
    private val designFeatureToggles: DesignFeatureToggles = mockk()
    private val addToPortfolioManagerFactory: AddToPortfolioManager.Factory = mockk(relaxed = true)
    private val getTopFiveMarketTokenUseCase: GetTopFiveMarketTokenUseCase = mockk(relaxed = true)
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk()
    private val feedClickIntents: FeedModelClickIntents = mockk(relaxed = true)

    @BeforeEach
    fun setUpDateFormatMock() {
        // DateTimeFormatters.dateDMMM is a lazy val that calls android.text.format.DateFormat
        // .getBestDateTimePattern — an Android stub not available in JVM unit tests.
        // Mirror the pattern used in TxHistoryInfoToTxHistoryDetailsUMConverterTest.
        mockkStatic(DateFormat::class)
        every { DateFormat.getBestDateTimePattern(any(), any()) } answers { secondArg() }
    }

    @AfterEach
    fun tearDownDateFormatMock() {
        unmockkStatic(DateFormat::class)
    }

    /**
     * Builds a [FeedComponentModel] wired into the given [TestScope], using a real
     * [FeedStateController] so we can read the initialised state directly.
     *
     * All deps unrelated to [ForYouFeatureToggles] are relaxed or stubbed with empty flows so
     * the model's background coroutines don't throw.
     */
    private fun TestScope.createModel(forYouFeatureToggles: ForYouFeatureToggles): FeedComponentModel {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val dispatchers = TestingCoroutineDispatcherProvider(
            main = testDispatcher,
            mainImmediate = testDispatcher,
            io = testDispatcher,
            default = testDispatcher,
            single = testDispatcher,
        )

        every { getSelectedAppCurrencyUseCase() } returns flowOf(Either.Right(AppCurrency.Default))
        every { manageTrendingNewsUseCase.observeTrendingNews() } returns emptyFlow()
        every { getTopEarnTokensUseCase() } returns emptyFlow()
        every { designFeatureToggles.isRedesignEnabled } returns false

        val paramsContainer = MutableParamsContainer(FeedParams(feedClickIntents = feedClickIntents))

        return FeedComponentModel(
            dispatchers = dispatchers,
            fetchTrendingNewsUseCase = fetchTrendingNewsUseCase,
            manageTrendingNewsUseCase = manageTrendingNewsUseCase,
            analyticsEventHandler = analyticsEventHandler,
            stateController = FeedStateController(),
            fetchTopEarnTokensUseCase = fetchTopEarnTokensUseCase,
            getTopEarnTokensUseCase = getTopEarnTokensUseCase,
            appRouter = appRouter,
            designFeatureToggles = designFeatureToggles,
            addToPortfolioManagerFactory = addToPortfolioManagerFactory,
            forYouFeatureToggles = forYouFeatureToggles,
            getTopFiveMarketTokenUseCase = getTopFiveMarketTokenUseCase,
            getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
            paramsContainer = paramsContainer,
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class `initialState forYouBannerUM` {

        @Test
        fun `GIVEN isForYouEnabled is true WHEN model initialises THEN forYouBannerUM is Content`() = runTest {
            // Arrange
            val toggles = mockk<ForYouFeatureToggles> { every { isForYouEnabled } returns true }

            // Act
            val model = createModel(forYouFeatureToggles = toggles)
            advanceUntilIdle()

            // Assert
            assertThat(model.state.value.forYouBannerUM).isInstanceOf(ForYouBannerUM.Content::class.java)

            model.onDestroy()
        }

        @Test
        fun `GIVEN isForYouEnabled is false WHEN model initialises THEN forYouBannerUM is Empty`() = runTest {
            // Arrange
            val toggles = mockk<ForYouFeatureToggles> { every { isForYouEnabled } returns false }

            // Act
            val model = createModel(forYouFeatureToggles = toggles)
            advanceUntilIdle()

            // Assert
            assertThat(model.state.value.forYouBannerUM).isEqualTo(ForYouBannerUM.Empty)

            model.onDestroy()
        }

        @Test
        fun `GIVEN isForYouEnabled is true WHEN Content banner clicked THEN openForYou invoked`() = runTest {
            // Arrange
            val toggles = mockk<ForYouFeatureToggles> { every { isForYouEnabled } returns true }

            // Act
            val model = createModel(forYouFeatureToggles = toggles)
            advanceUntilIdle()
            val banner = model.state.value.forYouBannerUM
            (banner as? ForYouBannerUM.Content)?.onClick?.invoke()

            // Assert – onClick must be wired to feedClickIntents::openForYou, not just any lambda
            assertThat(banner).isInstanceOf(ForYouBannerUM.Content::class.java)
            verify(exactly = 1) { feedClickIntents.openForYou() }

            model.onDestroy()
        }
    }
}