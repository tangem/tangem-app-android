package com.tangem.features.jointaccount.main.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import org.junit.jupiter.api.Test
import com.tangem.core.ui.R as CoreUiR

internal class UpdateMembersInitialStateTransformerTest {

    @Test
    fun `GIVEN invite mode WHEN transform THEN invite title and wired screen actions`() {
        // Arrange
        val onShareSafelyClick = {}
        val onArchiveClick = {}
        val onCloseClick = {}
        val transformer = UpdateMembersInitialStateTransformer(
            isInviteMode = true,
            onShareSafelyClick = onShareSafelyClick,
            onArchiveClick = onArchiveClick,
            onCloseClick = onCloseClick,
        )

        // Act
        val actual = transformer.transform(createMembersUM())

        // Assert
        assertThat(actual.title).isEqualTo(resourceReference(CoreUiR.string.joint_account_invite_members_title))
        assertThat(actual.shareSafely?.title)
            .isEqualTo(resourceReference(CoreUiR.string.joint_account_invite_members_safety_title))
        assertThat(actual.shareSafely?.onClick).isSameInstanceAs(onShareSafelyClick)
        assertThat(actual.onArchiveClick).isSameInstanceAs(onArchiveClick)
        assertThat(actual.onCloseClick).isSameInstanceAs(onCloseClick)
    }

    @Test
    fun `GIVEN reference mode WHEN transform THEN members title`() {
        // Arrange
        val transformer = UpdateMembersInitialStateTransformer(
            isInviteMode = false,
            onShareSafelyClick = {},
            onArchiveClick = {},
            onCloseClick = {},
        )

        // Act
        val actual = transformer.transform(createMembersUM())

        // Assert
        assertThat(actual.title).isEqualTo(resourceReference(CoreUiR.string.joint_account_members_title))
    }

    @Test
    fun `GIVEN account data in the state WHEN transform THEN it is untouched`() {
        // Arrange
        val prevState = createMembersUM(
            progress = stringReference(value = "1 of 3"),
            canArchive = true,
            activation = createActivation(),
        )
        val transformer = UpdateMembersInitialStateTransformer(
            isInviteMode = true,
            onShareSafelyClick = {},
            onArchiveClick = {},
            onCloseClick = {},
        )

        // Act
        val actual = transformer.transform(prevState)

        // Assert
        assertThat(actual.progress).isEqualTo(prevState.progress)
        assertThat(actual.members).isEqualTo(prevState.members)
        assertThat(actual.canArchive).isEqualTo(prevState.canArchive)
        assertThat(actual.activation).isEqualTo(prevState.activation)
    }
}