package com.tangem.features.jointaccount.main.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.jointaccount.main.JointAccountMembersUM
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test

internal class ClearMembersAccountTransformerTest {

    @Test
    fun `GIVEN populated state WHEN transform THEN account data is reset and the screen chrome is kept`() {
        // Arrange
        val prevState = createMembersUM(
            title = stringReference(value = "Invite members"),
            progress = stringReference(value = "1 of 3"),
            members = persistentListOf(
                JointAccountMembersUM.MemberUM.FreeSlot(
                    id = "slot_0",
                    title = TextReference.EMPTY,
                    status = TextReference.EMPTY,
                    canInvite = true,
                    onInviteClick = {},
                ),
            ),
            otherMembersLabel = stringReference(value = "Other members"),
            canArchive = true,
            activation = createActivation(),
        )

        // Act
        val actual = ClearMembersAccountTransformer().transform(prevState)

        // Assert
        val expected = prevState.copy(
            progress = TextReference.EMPTY,
            members = persistentListOf(),
            otherMembersLabel = null,
            canArchive = false,
            activation = null,
        )
        assertThat(actual).isEqualTo(expected)
    }
}