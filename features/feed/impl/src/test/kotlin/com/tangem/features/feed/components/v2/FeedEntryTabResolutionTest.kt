package com.tangem.features.feed.components.v2

import androidx.compose.ui.graphics.vector.ImageVector
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_earth_20
import com.tangem.features.feed.nav.FeedFilterSheetRoute
import com.tangem.features.feed.nav.FeedRoute
import com.tangem.features.feed.nav.FeedTabComponent
import com.tangem.features.feed.nav.FeedTabContributor
import com.tangem.features.feed.nav.FeedTabId
import com.tangem.features.feed.v2.FeedEntryRequest
import com.tangem.features.feed.v2.FeedFilterSheet
import com.tangem.features.feed.v2.FeedSection
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FeedEntryTabResolutionTest {

    @Test
    fun `GIVEN a shown section WHEN resolved THEN its tab opens`() {
        // Act
        val tab = FeedEntryRequest(section = FeedSection.Earn).resolveTab(tabs = tabs())

        // Assert
        assertThat(tab).isEqualTo(FeedTabId.Earn)
    }

    @Test
    fun `GIVEN no section WHEN resolved THEN the first tab opens`() {
        // Act
        val tab = FeedEntryRequest().resolveTab(tabs = tabs())

        // Assert
        assertThat(tab).isEqualTo(FeedTabId.Crypto)
    }

    @Test
    fun `GIVEN a section this build does not show WHEN resolved THEN the first tab opens`() {
        // Act - the tab is behind a feature toggle, so the deeplink cannot reach it
        val tab = FeedEntryRequest(section = FeedSection.Assets)
            .resolveTab(tabs = tabs(ids = listOf(FeedTabId.Crypto, FeedTabId.Earn)))

        // Assert
        assertThat(tab).isEqualTo(FeedTabId.Crypto)
    }

    @Test
    fun `GIVEN no tabs at all WHEN resolved THEN there is nothing to open`() {
        // Act
        val tab = FeedEntryRequest(section = FeedSection.Crypto).resolveTab(tabs = emptyList())

        // Assert
        assertThat(tab).isNull()
    }

    @Test
    fun `GIVEN the requested picker on top WHEN checked THEN the host keeps it`() {
        // Arrange
        val request = FeedEntryRequest(section = FeedSection.Assets, sheet = FeedFilterSheet.Category)

        // Act
        val isShown = request.isShownBy(active = TestPickerRoute(tabId = FeedTabId.RealAssets))

        // Assert
        assertThat(isShown).isTrue()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN anything but the requested picker on top WHEN checked THEN the host clears the stack`(
        model: ShownByModel,
    ) {
        // Act
        val isShown = model.request.isShownBy(active = model.active)

        // Assert
        assertThat(isShown).isFalse()
    }

    private fun provideTestModels() = listOf(
        // another tab's category picker
        ShownByModel(
            request = FeedEntryRequest(section = FeedSection.Crypto, sheet = FeedFilterSheet.Category),
            active = TestPickerRoute(tabId = FeedTabId.RealAssets),
        ),
        // the same tab, another picker
        ShownByModel(
            request = FeedEntryRequest(section = FeedSection.Assets, sheet = FeedFilterSheet.Network),
            active = TestPickerRoute(tabId = FeedTabId.RealAssets),
        ),
        // no sheet asked for: the toggle is off, or the link has no filter
        ShownByModel(
            request = FeedEntryRequest(section = FeedSection.Assets, categoryId = "xstocks"),
            active = TestPickerRoute(tabId = FeedTabId.RealAssets),
        ),
        // not a picker at all
        ShownByModel(
            request = FeedEntryRequest(section = FeedSection.Assets, sheet = FeedFilterSheet.Category),
            active = FeedRoute.ForYou,
        ),
    )

    data class ShownByModel(val request: FeedEntryRequest, val active: FeedRoute)

    private data class TestPickerRoute(
        override val tabId: FeedTabId,
        override val sheet: FeedFilterSheet = FeedFilterSheet.Category,
    ) : FeedFilterSheetRoute

    private fun tabs(
        ids: List<FeedTabId> = listOf(FeedTabId.Crypto, FeedTabId.RealAssets, FeedTabId.Earn),
    ): List<FeedTabContributor> = ids.map(::TestContributor)

    private class TestContributor(override val id: FeedTabId) : FeedTabContributor {

        override val isAvailable: Boolean = true
        override val title: TextReference = stringReference(id.value)
        override val icon: ImageVector = Icons.ic_earth_20

        override fun createTab(context: AppComponentContext): FeedTabComponent = mockk()
    }
}