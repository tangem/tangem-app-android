package com.tangem.features.feed.earn.model.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds2.filter.TangemFilterItemUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.feed.earn.ui.state.EarnBestOpportunitiesUM
import com.tangem.features.feed.earn.ui.state.EarnFeedTabUM
import com.tangem.features.feed.earn.ui.state.EarnListUM
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Every transformer owns exactly one field of [EarnFeedTabUM]. The expectations are written as
 * `prevState.copy(theOwnedField = …)`, so a transformer that touched a second field — or the wrong one —
 * fails the comparison.
 */
internal class EarnFeedTabUMTransformerTest {

    private val onScroll: () -> Unit = {}

    /** Deliberately non-default in every field, so overwriting the wrong one is visible. */
    private val prevState = EarnFeedTabUM(
        mostlyUsed = EarnListUM.Error(onRetryClicked = {}),
        bestOpportunities = EarnBestOpportunitiesUM.Empty,
        filters = persistentListOf(TangemFilterItemUM.Inactive(id = "prev", label = stringReference("Prev")) {}),
        onSliderScroll = onScroll,
    )

    @Nested
    inner class UpdateBestOpportunitiesState {

        @Test
        fun `GIVEN a new best opportunities state WHEN transformed THEN only that section changes`() {
            // Arrange
            val newState = EarnBestOpportunitiesUM.Loading

            // Act
            val actual = UpdateBestOpportunitiesStateTransformer(newState).transform(prevState)

            // Assert
            assertThat(actual).isEqualTo(prevState.copy(bestOpportunities = newState))
        }
    }

    @Nested
    inner class UpdateEarnFeedTabUMInitialState {

        @Test
        fun `GIVEN a scroll handler WHEN transformed THEN only the handler changes`() {
            // Arrange
            val newOnScroll: () -> Unit = {}

            // Act
            val actual = UpdateEarnFeedTabUMInitialStateTransformer(newOnScroll).transform(prevState)

            // Assert
            assertThat(actual).isEqualTo(prevState.copy(onSliderScroll = newOnScroll))
        }
    }

    @Nested
    inner class UpdateOpportunitiesStateLoading {

        @Test
        fun `GIVEN a previous mostly used state WHEN transformed THEN only that section falls back to loading`() {
            // Act
            val actual = UpdateOpportunitiesStateLoadingTransformer().transform(prevState)

            // Assert
            assertThat(actual).isEqualTo(prevState.copy(mostlyUsed = EarnListUM.Loading))
        }
    }
}