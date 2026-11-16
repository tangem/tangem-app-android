package com.tangem.features.feed.deeplink

import android.net.Uri
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.deeplink.DeepLinkRequest
import com.tangem.common.routing.deeplink.DeeplinkConst.CATEGORY_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.SECTION_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TYPE_KEY
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.models.earn.PreselectedEarnType
import com.tangem.features.feed.FeedFeatureToggles
import com.tangem.features.feed.deeplink.DiscoverDeepLinkHandler.Companion.FILTER_KEY
import com.tangem.features.feed.v2.FeedEntryController
import com.tangem.features.feed.v2.FeedEntryRequest
import com.tangem.features.feed.v2.FeedFilterSheet
import com.tangem.features.feed.v2.FeedSection
import com.tangem.test.core.ProvideTestModels
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class DiscoverDeepLinkHandlerTest {

    private val router: Router = mockk()
    private val feedEntryController: FeedEntryController = mockk()
    private val feedFeatureToggles: FeedFeatureToggles = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(router, feedEntryController, feedFeatureToggles)
        every { router.popTo(any<AppRoute>(), any()) } just Runs
        every { feedEntryController.request(any()) } just Runs
        every { feedFeatureToggles.isDiscoverDeeplinkFiltersEnabled } returns false
    }

    @Test
    fun `GIVEN no params WHEN handled THEN opens the wallet with an empty request`() {
        // Act
        handle(queryParams = emptyMap())

        // Assert
        verify(exactly = 1) { feedEntryController.request(FeedEntryRequest()) }
    }

    @Test
    fun `GIVEN assets section WHEN handled THEN requests the real assets section`() {
        // Act
        handle(queryParams = mapOf(SECTION_KEY to "assets"))

        // Assert
        verify(exactly = 1) { feedEntryController.request(FeedEntryRequest(section = FeedSection.Assets)) }
    }

    @Test
    fun `GIVEN unknown section WHEN handled THEN requests no section so the default tab opens`() {
        // Act
        handle(queryParams = mapOf(SECTION_KEY to "nonsense"))

        // Assert
        verify(exactly = 1) { feedEntryController.request(FeedEntryRequest(section = null)) }
    }

    @Test
    fun `GIVEN category WHEN handled THEN it is forwarded unvalidated for the tab to resolve`() {
        // Act
        handle(queryParams = mapOf(SECTION_KEY to "crypto", CATEGORY_KEY to "layer-1"))

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
        handle(queryParams = mapOf(SECTION_KEY to "crypto", CATEGORY_KEY to " "))

        // Assert
        verify(exactly = 1) { feedEntryController.request(FeedEntryRequest(section = FeedSection.Crypto)) }
    }

    @Test
    fun `GIVEN earn section and staking type WHEN handled THEN requests that earn type`() {
        // Act
        handle(queryParams = mapOf(SECTION_KEY to "earn", TYPE_KEY to "staking"))

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
        handle(queryParams = mapOf(SECTION_KEY to "earn", TYPE_KEY to "embedded_yield"))

        // Assert
        verify(exactly = 1) { feedEntryController.request(FeedEntryRequest(section = FeedSection.Earn)) }
    }

    @Test
    fun `GIVEN any params WHEN handled THEN the stack pops back to the wallet`() {
        // Act
        handle(queryParams = emptyMap())

        // Assert
        verify(exactly = 1) { router.popTo(AppRoute.Wallet, any()) }
    }

    @Test
    fun `GIVEN toggle off and a filter WHEN handled THEN the section opens without a sheet`() {
        // Act
        handle(queryParams = mapOf(SECTION_KEY to "earn", FILTER_KEY to "type", TYPE_KEY to "yield"))

        // Assert
        verify(exactly = 1) {
            feedEntryController.request(
                FeedEntryRequest(section = FeedSection.Earn, earnType = PreselectedEarnType.Yield),
            )
        }
    }

    @Test
    fun `GIVEN toggle off and for you section WHEN handled THEN the default tab opens`() {
        // Act
        handle(queryParams = mapOf(SECTION_KEY to "foryou"))

        // Assert
        verify(exactly = 1) { feedEntryController.request(FeedEntryRequest()) }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class FiltersEnabled {

        @BeforeEach
        fun enableToggle() {
            every { feedFeatureToggles.isDiscoverDeeplinkFiltersEnabled } returns true
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN deeplink params WHEN handled THEN requests the expected entry`(model: EntryModel) {
            // Act
            handle(queryParams = model.queryParams)

            // Assert
            verify(exactly = 1) { feedEntryController.request(model.expected) }
        }

        @Test
        fun `GIVEN for you section WHEN handled THEN the wallet is uncovered`() {
            // Act
            handle(queryParams = mapOf(SECTION_KEY to "foryou"))

            // Assert
            verify(exactly = 1) { router.popTo(AppRoute.Wallet, any()) }
        }

        private fun provideTestModels() = listOf(
            EntryModel(
                queryParams = mapOf(SECTION_KEY to "foryou"),
                expected = FeedEntryRequest(isForYou = true),
            ),
            EntryModel(
                queryParams = mapOf(SECTION_KEY to "foryou", FILTER_KEY to "category", CATEGORY_KEY to "layer-1"),
                expected = FeedEntryRequest(isForYou = true),
            ),
            EntryModel(
                queryParams = mapOf(SECTION_KEY to "crypto", FILTER_KEY to "category"),
                expected = FeedEntryRequest(section = FeedSection.Crypto, sheet = FeedFilterSheet.Category),
            ),
            EntryModel(
                queryParams = mapOf(SECTION_KEY to "crypto", FILTER_KEY to "category", CATEGORY_KEY to "layer-1"),
                expected = FeedEntryRequest(
                    section = FeedSection.Crypto,
                    categoryId = "layer-1",
                    sheet = FeedFilterSheet.Category,
                ),
            ),
            EntryModel(
                queryParams = mapOf(SECTION_KEY to "assets", FILTER_KEY to "category"),
                expected = FeedEntryRequest(section = FeedSection.Assets, sheet = FeedFilterSheet.Category),
            ),
            EntryModel(
                queryParams = mapOf(SECTION_KEY to "earn", FILTER_KEY to "network"),
                expected = FeedEntryRequest(section = FeedSection.Earn, sheet = FeedFilterSheet.Network),
            ),
            EntryModel(
                queryParams = mapOf(SECTION_KEY to "earn", FILTER_KEY to "type", TYPE_KEY to "staking"),
                expected = FeedEntryRequest(
                    section = FeedSection.Earn,
                    earnType = PreselectedEarnType.Staking,
                    sheet = FeedFilterSheet.Type,
                ),
            ),
            EntryModel(
                queryParams = mapOf(SECTION_KEY to "crypto", FILTER_KEY to "type"),
                expected = FeedEntryRequest(section = FeedSection.Crypto),
            ),
            EntryModel(
                queryParams = mapOf(SECTION_KEY to "assets", FILTER_KEY to "network"),
                expected = FeedEntryRequest(section = FeedSection.Assets),
            ),
            EntryModel(
                queryParams = mapOf(SECTION_KEY to "earn", FILTER_KEY to "category"),
                expected = FeedEntryRequest(section = FeedSection.Earn),
            ),
            EntryModel(
                queryParams = mapOf(SECTION_KEY to "earn", FILTER_KEY to "nonsense"),
                expected = FeedEntryRequest(section = FeedSection.Earn),
            ),
            EntryModel(
                queryParams = mapOf(FILTER_KEY to "category"),
                expected = FeedEntryRequest(),
            ),
            EntryModel(
                queryParams = mapOf(SECTION_KEY to "assets"),
                expected = FeedEntryRequest(section = FeedSection.Assets),
            ),
        )
    }

    data class EntryModel(val queryParams: Map<String, String>, val expected: FeedEntryRequest)

    private fun handle(queryParams: Map<String, String>) = DiscoverDeepLinkHandler(
        feedEntryController = feedEntryController,
        feedFeatureToggles = feedFeatureToggles,
    ).handle(
        DeepLinkRequest(
            uri = mockk<Uri>(),
            queryParams = queryParams,
            scope = mockk<CoroutineScope>(),
            isFromOnNewIntent = false,
            router = router,
        ),
    )
}