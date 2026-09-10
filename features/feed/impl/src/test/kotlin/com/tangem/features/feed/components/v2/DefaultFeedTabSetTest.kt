package com.tangem.features.feed.components.v2

import androidx.compose.ui.graphics.vector.ImageVector
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_earth_20
import com.tangem.features.feed.nav.FeedTabComponent
import com.tangem.features.feed.nav.FeedTabContributor
import com.tangem.features.feed.nav.FeedTabId
import io.mockk.mockk
import org.junit.jupiter.api.Test

/** The only guard that a tab behind an unreleased toggle cannot reach users. */
internal class DefaultFeedTabSetTest {

    @Test
    fun `GIVEN unavailable contributor WHEN tabs built THEN it is dropped`() {
        // Arrange
        val contributors = setOf(
            TestContributor(id = FeedTabId.Crypto, isAvailable = true),
            TestContributor(id = FeedTabId.RealAssets, isAvailable = false),
        )

        // Act
        val tabs = DefaultFeedTabSet(contributors = contributors).tabs

        // Assert
        assertThat(tabs.map(FeedTabContributor::id)).containsExactly(FeedTabId.Crypto)
    }

    @Test
    fun `GIVEN available contributors WHEN tabs built THEN they follow the host order`() {
        // Arrange
        val contributors = setOf(
            TestContributor(id = FeedTabId.Earn, isAvailable = true),
            TestContributor(id = FeedTabId.Crypto, isAvailable = true),
            TestContributor(id = FeedTabId.RealAssets, isAvailable = true),
        )

        // Act
        val tabs = DefaultFeedTabSet(contributors = contributors).tabs

        // Assert
        assertThat(tabs.map(FeedTabContributor::id))
            .containsExactly(FeedTabId.Crypto, FeedTabId.RealAssets, FeedTabId.Earn)
            .inOrder()
    }

    private class TestContributor(
        override val id: FeedTabId,
        override val isAvailable: Boolean,
    ) : FeedTabContributor {

        override val title: TextReference = stringReference(id.value)
        override val icon: ImageVector = Icons.ic_earth_20

        override fun createTab(context: AppComponentContext): FeedTabComponent = mockk()
    }
}