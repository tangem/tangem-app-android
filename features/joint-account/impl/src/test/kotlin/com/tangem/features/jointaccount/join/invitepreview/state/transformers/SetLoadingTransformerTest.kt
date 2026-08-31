package com.tangem.features.jointaccount.join.invitepreview.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.features.jointaccount.join.invitepreview.ui.state.JointAccountInvitePreviewUM
import org.junit.jupiter.api.Test

internal class SetLoadingTransformerTest {

    @Test
    fun `GIVEN any prev state WHEN transform THEN Loading is set`() {
        // Arrange
        val onCloseClick = {}
        val transformer = SetLoadingTransformer(onCloseClick = onCloseClick)
        val prevState = JointAccountInvitePreviewUM.Failed(onRetryClick = {}, onCloseClick = {})

        // Act
        val actual = transformer.transform(prevState = prevState)

        // Assert
        assertThat(actual).isEqualTo(JointAccountInvitePreviewUM.Loading(onCloseClick = onCloseClick))
    }
}