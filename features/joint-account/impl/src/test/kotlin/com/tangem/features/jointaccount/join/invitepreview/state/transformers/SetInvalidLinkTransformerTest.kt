package com.tangem.features.jointaccount.join.invitepreview.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.features.jointaccount.join.invitepreview.ui.state.JointAccountInvitePreviewUM
import org.junit.jupiter.api.Test

internal class SetInvalidLinkTransformerTest {

    @Test
    fun `GIVEN any prev state WHEN transform THEN InvalidLink is set`() {
        // Arrange
        val onGotItClick = {}
        val transformer = SetInvalidLinkTransformer(onGotItClick = onGotItClick)
        val prevState = JointAccountInvitePreviewUM.Loading(onCloseClick = {})

        // Act
        val actual = transformer.transform(prevState = prevState)

        // Assert
        assertThat(actual).isEqualTo(JointAccountInvitePreviewUM.InvalidLink(onGotItClick = onGotItClick))
    }
}