package com.tangem.features.jointaccount.main.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.account.AccountIconUM
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
            header = JointAccountMembersUM.HeaderUM(title = stringReference("Invite members")),
            summaryCard = JointAccountMembersUM.SummaryCardUM(
                icon = AccountIconUM.CryptoPortfolio(
                    value = CryptoPortfolioIcon.Icon.Family,
                    color = CryptoPortfolioIcon.Color.Azure,
                ),
                name = stringReference("Family"),
                membersCount = stringReference("3 members"),
                approvals = stringReference("Any 2 of 3"),
                onApprovalsInfoClick = {},
            ),
            joined = persistentListOf(
                JointAccountMembersUM.MemberUM.Joined(
                    id = "member_0",
                    avatar = JointAccountMembersUM.MemberAvatarUM(
                        monogram = "I",
                        color = CryptoPortfolioIcon.Color.Azure,
                    ),
                    name = stringReference("Ivan"),
                    isYou = true,
                    isCreator = true,
                    status = JointAccountMembersUM.MemberStatusUM.Joined,
                    onInfoClick = {},
                ),
            ),
            freeSlots = persistentListOf(
                JointAccountMembersUM.MemberUM.FreeSlot(
                    id = "slot_0",
                    title = stringReference(""),
                    status = JointAccountMembersUM.SlotStatusUM.NotInvited,
                    action = JointAccountMembersUM.SlotActionUM.Invite(onClick = {}),
                ),
            ),
            membersLabel = stringReference("Members"),
            waitingLabel = stringReference("Waiting to join"),
            footer = JointAccountMembersUM.FooterUM(
                hint = null,
                button = JointAccountMembersUM.ConfirmButtonUM(
                    isEnabled = true,
                    onClick = {},
                ),
            ),
        )

        // Act
        val actual = ClearMembersAccountTransformer().transform(prevState)

        // Assert
        assertThat(actual.topBar).isEqualTo(topBar)
        assertThat(actual.header).isNull()
        assertThat(actual.summaryCard).isNull()
        assertThat(actual.joined).isEmpty()
        assertThat(actual.freeSlots).isEmpty()
        assertThat(actual.membersLabel).isNull()
        assertThat(actual.waitingLabel).isNull()
        assertThat(actual.footer).isNull()
    }
}