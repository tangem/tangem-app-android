package com.tangem.features.jointaccount.main.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.jointaccount.main.JointAccountMembersUM
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test

internal class ClearMembersAccountTransformerTest {

    @Test
    fun `GIVEN populated state WHEN transform THEN account data is reset and top bar is kept as is`() {
        // Arrange
        val topBar = JointAccountMembersUM.TopBarUM.Titled(title = stringReference("Members"), onBackClick = {})
        val prevState = createMembersUM(
            topBar = topBar,
            header = JointAccountMembersUM.HeaderUM(
                title = stringReference("Invite members"),
                progress = stringReference("1 of 3"),
            ),
            shareSafely = JointAccountMembersUM.ShareSafelyUM(
                title = stringReference("Share invites safely"),
                description = stringReference(""),
                onClick = {},
            ),
            joined = persistentListOf(
                JointAccountMembersUM.MemberUM.Joined(
                    id = "member_0",
                    avatar = JointAccountMembersUM.MemberAvatarUM(
                        monogram = "I",
                        color = CryptoPortfolioIcon.Color.Azure,
                    ),
                    name = stringReference("Ivan"),
                    role = stringReference("Creator"),
                    onInfoClick = {},
                ),
            ),
            freeSlots = persistentListOf(
                JointAccountMembersUM.MemberUM.FreeSlot(
                    id = "slot_0",
                    title = stringReference(""),
                    status = stringReference(""),
                    canInvite = true,
                    onInviteClick = {},
                ),
            ),
            otherMembersLabel = stringReference("Other members"),
            onActivateClick = {},
        )

        // Act
        val actual = ClearMembersAccountTransformer().transform(prevState)

        // Assert
        assertThat(actual.topBar).isEqualTo(topBar)
        assertThat(actual.header).isNull()
        assertThat(actual.shareSafely).isNull()
        assertThat(actual.joined).isEmpty()
        assertThat(actual.freeSlots).isEmpty()
        assertThat(actual.otherMembersLabel).isNull()
        assertThat(actual.onActivateClick).isNull()
    }
}