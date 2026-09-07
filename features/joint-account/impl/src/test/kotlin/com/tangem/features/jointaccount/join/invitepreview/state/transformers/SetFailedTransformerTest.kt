package com.tangem.features.jointaccount.join.invitepreview.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.features.jointaccount.join.invitepreview.ui.state.JointAccountInvitePreviewUM
import org.junit.jupiter.api.Test

internal class SetFailedTransformerTest {

    @Test
    fun `GIVEN any prev state WHEN transform THEN Failed is set`() {
        // Arrange
        val onRetryClick = {}
        val onCloseClick = {}
        val transformer = SetFailedTransformer(onRetryClick = onRetryClick, onCloseClick = onCloseClick)
        val prevState = JointAccountInvitePreviewUM.Loading(onCloseClick = {})

        // Act
        val actual = transformer.transform(prevState = prevState)

        // Assert
        val expected = JointAccountInvitePreviewUM.Failed(onRetryClick = onRetryClick, onCloseClick = onCloseClick)
        assertThat(actual).isEqualTo(expected)
    }
}