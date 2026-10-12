package com.tangem.features.feed.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.deeplink.DeeplinkConst.CATEGORY_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.SECTION_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TYPE_KEY
import com.tangem.domain.models.earn.PreselectedEarnType
import com.tangem.features.feed.FeedFeatureToggles
import com.tangem.features.feed.v2.FeedEntryController
import com.tangem.features.feed.v2.FeedEntryRequest
import com.tangem.features.feed.v2.FeedSection
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DefaultDiscoverDeepLinkHandlerTest {

    private val appRouter: AppRouter = mockk()
    private val feedEntryController: FeedEntryController = mockk()
    private val feedFeatureToggles: FeedFeatureToggles = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(appRouter, feedEntryController, feedFeatureToggles)
        every { appRouter.popTo(any<AppRoute>(), any()) } just Runs
        every { appRouter.stack } returns listOf(AppRoute.Wallet, AppRoute.AppSettings)
        every { feedEntryController.request(any()) } just Runs
        every { feedFeatureToggles.isNewShtorkaEnabled } returns true
    }

    @Test
    fun `GIVEN no params WHEN handled THEN opens the wallet with an empty request`() {
        // Act
        createHandler(queryParams = emptyMap())

        // Assert
        verify(exactly = 1) { feedEntryController.request(FeedEntryRequest()) }
    }

    @Test
    fun `GIVEN assets section WHEN handled THEN requests the real assets section`() {
        // Act
        createHandler(queryParams = mapOf(SECTION_KEY to "assets"))

        // Assert
        verify(exactly = 1) { feedEntryController.request(FeedEntryRequest(section = FeedSection.Assets)) }
    }

    @Test
    fun `GIVEN unknown section WHEN handled THEN requests no section so the default tab opens`() {
        // Act
        createHandler(queryParams = mapOf(SECTION_KEY to "nonsense"))

        // Assert
        verify(exactly = 1) { feedEntryController.request(FeedEntryRequest(section = null)) }
    }

    @Test
    fun `GIVEN category WHEN handled THEN it is forwarded unvalidated for the tab to resolve`() {
        // Act
        createHandler(queryParams = mapOf(SECTION_KEY to "crypto", CATEGORY_KEY to "layer-1"))

        // Assert
        verify(exactly = 1) {
            feedEntryController.request(
                FeedEntryRequest(section = FeedSection.Crypto, categoryId = "layer-1"),
            )
        }
    }

    @Test
    fun `GIVEN blank category WHEN handled THEN no category is requested`() {
        // Act
        createHandler(queryParams = mapOf(SECTION_KEY to "crypto", CATEGORY_KEY to " "))

        // Assert
        verify(exactly = 1) { feedEntryController.request(FeedEntryRequest(section = FeedSection.Crypto)) }
    }

    @Test
    fun `GIVEN earn section and staking type WHEN handled THEN requests that earn type`() {
        // Act
        createHandler(queryParams = mapOf(SECTION_KEY to "earn", TYPE_KEY to "staking"))

        // Assert
        verify(exactly = 1) {
            feedEntryController.request(
                FeedEntryRequest(section = FeedSection.Earn, earnType = PreselectedEarnType.Staking),
            )
        }
    }

    @Test
    fun `GIVEN a type the app has no filter for WHEN handled THEN the section opens unfiltered`() {
        // Act
        createHandler(queryParams = mapOf(SECTION_KEY to "earn", TYPE_KEY to "embedded_yield"))

        // Assert
        verify(exactly = 1) { feedEntryController.request(FeedEntryRequest(section = FeedSection.Earn)) }
    }

    @Test
    fun `GIVEN a covered wallet WHEN handled THEN the screens above it are dropped`() {
        // Act
        createHandler(queryParams = emptyMap())

        // Assert
        verify(exactly = 1) { appRouter.popTo(AppRoute.Wallet, any()) }
    }

    @Test
    fun `GIVEN the wallet is already on top WHEN handled THEN it is not popped to again`() {
        // Arrange - popping to where the app already is reports a failed navigation to the user
        every { appRouter.stack } returns listOf(AppRoute.Wallet)

        // Act
        createHandler(queryParams = mapOf(SECTION_KEY to "assets"))

        // Assert
        verify(exactly = 1) { feedEntryController.request(FeedEntryRequest(section = FeedSection.Assets)) }
        verify(exactly = 0) { appRouter.popTo(any<AppRoute>(), any()) }
    }

    @Test
    fun `GIVEN a cold start WHEN handled THEN the wallet is left to open on its own`() {
        // Arrange - the wallet is not on the stack yet, so popping to it could only fail
        every { appRouter.stack } returns emptyList()

        // Act
        createHandler(queryParams = mapOf(SECTION_KEY to "earn"))

        // Assert
        verify(exactly = 1) { feedEntryController.request(FeedEntryRequest(section = FeedSection.Earn)) }
        verify(exactly = 0) { appRouter.popTo(any<AppRoute>(), any()) }
    }

    @Test
    fun `GIVEN the hub is disabled WHEN handled THEN nothing is requested and the wallet opens`() {
        // Arrange
        every { feedFeatureToggles.isNewShtorkaEnabled } returns false

        // Act
        createHandler(queryParams = mapOf(SECTION_KEY to "earn"))

        // Assert
        verify(exactly = 0) { feedEntryController.request(any()) }
        verify(exactly = 1) { appRouter.popTo(AppRoute.Wallet, any()) }
    }

    private fun createHandler(queryParams: Map<String, String>) = DefaultDiscoverDeepLinkHandler(
        queryParams = queryParams,
        feedEntryController = feedEntryController,
        feedFeatureToggles = feedFeatureToggles,
        appRouter = appRouter,
    )
}