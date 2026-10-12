package com.tangem.features.jointaccount.main.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.features.jointaccount.main.JointAccountMembersUM
import org.junit.jupiter.api.Test

internal class UpdateMembersInitialStateTransformerTest {

    @Test
    fun `GIVEN placeholder state WHEN update initial state THEN the real close callback is wired`() {
        // Arrange
        var closed = false
        val onCloseClick = { closed = true }
        val prevState = createMembersUM()

        // Act
        val actual = UpdateMembersInitialStateTransformer(onCloseClick = onCloseClick).transform(prevState)

        // Assert
        val topBar = actual.topBar as JointAccountMembersUM.TopBarUM.Dismissible
        assertThat(topBar.canArchive).isFalse()
        topBar.onCloseClick()
        assertThat(closed).isTrue()
    }
}