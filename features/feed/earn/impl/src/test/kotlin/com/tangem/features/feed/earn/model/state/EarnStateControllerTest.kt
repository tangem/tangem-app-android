package com.tangem.features.feed.earn.model.state

import com.google.common.truth.Truth.assertThat
import com.tangem.features.feed.earn.model.filters.state.EarnFilterChipsFactory
import com.tangem.features.feed.earn.model.state.transformers.UpdateBestOpportunitiesStateTransformer
import com.tangem.features.feed.earn.model.state.transformers.UpdateOpportunitiesStateLoadingTransformer
import com.tangem.features.feed.earn.ui.state.EarnBestOpportunitiesUM
import com.tangem.features.feed.earn.ui.state.EarnFeedTabUM
import com.tangem.features.feed.earn.ui.state.EarnListUM
import org.junit.jupiter.api.Test

internal class EarnStateControllerTest {

    private val controller = EarnStateController()

    @Test
    fun `GIVEN a fresh controller WHEN state read THEN both sections and the chips are loading`() {
        // Act
        val actual = controller.uiState.value

        // Assert — the tab opens fully in a loading state, so nothing flashes as empty before data arrives
        assertThat(actual).isEqualTo(
            EarnFeedTabUM(
                mostlyUsed = EarnListUM.Loading,
                bestOpportunities = EarnBestOpportunitiesUM.Loading,
                filters = EarnFilterChipsFactory.LOADING,
                onSliderScroll = actual.onSliderScroll,
            ),
        )
    }

    @Test
    fun `GIVEN a transformer WHEN update called THEN its result is published to the state flow`() {
        // Act
        controller.update(UpdateBestOpportunitiesStateTransformer(EarnBestOpportunitiesUM.Empty))

        // Assert
        assertThat(controller.uiState.value.bestOpportunities).isEqualTo(EarnBestOpportunitiesUM.Empty)
    }

    @Test
    fun `GIVEN two transformers WHEN update called twice THEN the second one sees the first one's result`() {
        // Arrange
        controller.update(UpdateBestOpportunitiesStateTransformer(EarnBestOpportunitiesUM.Empty))

        // Act — the loading transformer must leave the previously set section alone
        controller.update(UpdateOpportunitiesStateLoadingTransformer())

        // Assert
        assertThat(controller.uiState.value.bestOpportunities).isEqualTo(EarnBestOpportunitiesUM.Empty)
        assertThat(controller.uiState.value.mostlyUsed).isEqualTo(EarnListUM.Loading)
    }
}