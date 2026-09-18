package com.tangem.features.feed.components.v2.model

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.router.pages.Pages
import com.arkivanov.decompose.router.pages.PagesNavigation
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.feed.components.v2.createTestingCoroutineDispatcherProvider
import com.tangem.features.feed.nav.FeedTabContributor
import com.tangem.features.feed.nav.FeedTabId
import com.tangem.features.feed.nav.FeedTabSet
import com.tangem.features.feed.ui.FeedTabUM
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class FeedHomeModelTest {

    private val tabSet: FeedTabSet = mockk()
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)

    @Test
    fun `GIVEN contributed tabs WHEN the model is created THEN the row mirrors them in order`() = runTest {
        // Arrange
        every { tabSet.tabs } returns listOf(contributor(FeedTabId.Crypto, "Crypto"), contributor(FeedTabId.Earn, "Earn"))

        // Act
        val model = createModel(testScope = this)

        // Assert
        assertThat(model.tabs).containsExactly(
            FeedTabUM(id = "crypto", title = stringReference("Crypto"), icon = CryptoIcon),
            FeedTabUM(id = "earn", title = stringReference("Earn"), icon = EarnIcon),
        ).inOrder()
        model.onDestroy()
    }

    @Test
    fun `GIVEN contributed tabs WHEN a tab is looked up by id THEN its contributor is returned`() = runTest {
        // Arrange
        val earn = contributor(FeedTabId.Earn, "Earn")
        every { tabSet.tabs } returns listOf(contributor(FeedTabId.Crypto, "Crypto"), earn)

        // Act
        val model = createModel(testScope = this)

        // Assert
        assertThat(model.contributorsById[FeedTabId.Earn]).isSameInstanceAs(earn)
        assertThat(model.contributorsById[FeedTabId.Predictions]).isNull()
        model.onDestroy()
    }

    @Test
    fun `GIVEN a contributed tab WHEN a block asks to open it THEN that page is selected`() = runTest {
        // Arrange
        every { tabSet.tabs } returns listOf(contributor(FeedTabId.Crypto, "Crypto"), contributor(FeedTabId.Earn, "Earn"))
        val model = createModel(testScope = this)
        val selectedIndices = model.pagesNavigation.trackSelection(pageCount = 2)

        // Act
        model.topBlocksCallbacks.onOpenTab(FeedTabId.Earn)

        // Assert
        assertThat(selectedIndices).containsExactly(1)
        model.onDestroy()
    }

    @Test
    fun `GIVEN a tab this feed does not contribute WHEN a block asks to open it THEN nothing is selected`() = runTest {
        // Arrange
        every { tabSet.tabs } returns listOf(contributor(FeedTabId.Crypto, "Crypto"))
        val model = createModel(testScope = this)
        val selectedIndices = model.pagesNavigation.trackSelection(pageCount = 1)

        // Act
        model.topBlocksCallbacks.onOpenTab(FeedTabId.Earn)

        // Assert
        assertThat(selectedIndices).isEmpty()
        model.onDestroy()
    }

    private fun createModel(testScope: TestScope): FeedHomeModel = FeedHomeModel(
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        tabSet = tabSet,
        analyticsEventHandler = analyticsEventHandler,
    )

    private fun contributor(id: FeedTabId, title: String): FeedTabContributor = mockk {
        every { this@mockk.id } returns id
        every { this@mockk.title } returns stringReference(title)
        every { icon } returns if (id == FeedTabId.Earn) EarnIcon else CryptoIcon
    }

    /** Replays each navigation event against a page set of [pageCount] and records the index it selects. */
    private fun PagesNavigation<FeedTabId>.trackSelection(pageCount: Int): List<Int> {
        val selected = mutableListOf<Int>()
        val pages = Pages(
            items = List(pageCount) { index -> FeedTabId("tab$index") },
            selectedIndex = 0,
        )
        subscribe { event -> selected.add(event.transformer(pages).selectedIndex) }
        return selected
    }

    private companion object {

        fun icon(name: String): ImageVector = ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).build()

        val CryptoIcon = icon(name = "crypto")
        val EarnIcon = icon(name = "earn")
    }
}