package com.tangem.features.jointaccount.main.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.features.jointaccount.main.JointAccountMembersUM
import com.tangem.features.jointaccount.main.entity.MemberCardConfig
import com.tangem.utils.StringsSigns
import org.junit.jupiter.api.Test
import com.tangem.core.ui.R as CoreUiR

internal class UpdateMembersAccountTransformerTest {

    @Test
    fun `GIVEN one joined of three WHEN transform THEN joined member and two free slots`() {
        // Arrange
        val transformer = createTransformer(
            account = createJointAccount(members = listOf(fixtureCreator)),
            invites = listOf("invite-a"),
        )

        // Act
        val actual = transformer.transform(createMembersUM())

        // Assert
        assertThat(actual.members).hasSize(3)
        assertThat(actual.members.filterIsInstance<JointAccountMembersUM.MemberUM.Joined>()).hasSize(1)
        assertThat(actual.members.filterIsInstance<JointAccountMembersUM.MemberUM.FreeSlot>()).hasSize(2)
        assertThat(actual.progress).isEqualTo(
            resourceReference(
                id = CoreUiR.string.joint_account_invite_members_subtitle,
                formatArgs = wrappedList(1, 3),
            ),
        )
        assertThat(actual.otherMembersLabel).isNotNull()
        assertThat(actual.canArchive).isTrue()
    }

    @Test
    fun `GIVEN full account WHEN transform THEN no free slots and no slots label`() {
        // Arrange
        val transformer = createTransformer(
            account = createJointAccount(
                membersCount = 2,
                members = listOf(fixtureCreator, fixtureMember),
            ),
        )

        // Act
        val actual = transformer.transform(createMembersUM())

        // Assert
        assertThat(actual.members.filterIsInstance<JointAccountMembersUM.MemberUM.FreeSlot>()).isEmpty()
        assertThat(actual.otherMembersLabel).isNull()
    }

    @Test
    fun `GIVEN invites on this device WHEN slot invite clicked THEN the remaining invite is shared`() {
        // Arrange
        val shared = mutableListOf<String>()
        val transformer = createTransformer(
            account = createJointAccount(
                members = listOf(fixtureCreator, fixtureMember.copy(inviteId = "invite-a")),
            ),
            invites = listOf("invite-a", "invite-b"),
            onShareInviteClick = shared::add,
        )

        // Act
        val slot = transformer.transform(createMembersUM()).members
            .filterIsInstance<JointAccountMembersUM.MemberUM.FreeSlot>()
            .single()
        slot.onInviteClick()

        // Assert
        assertThat(slot.canInvite).isTrue()
        // The joined member redeemed invite-a, so the free slot offers the unredeemed one
        assertThat(shared).containsExactly("invite-b")
    }

    @Test
    fun `GIVEN no invites on this device WHEN transform THEN no slot can be invited`() {
        // Arrange
        val transformer = createTransformer(
            account = createJointAccount(members = listOf(fixtureCreator)),
            invites = emptyList(),
        )

        // Act
        val slots = transformer.transform(createMembersUM()).members
            .filterIsInstance<JointAccountMembersUM.MemberUM.FreeSlot>()

        // Assert
        assertThat(slots.map { it.canInvite }).containsExactly(false, false)
    }

    @Test
    fun `GIVEN this device is the creator's WHEN transform THEN the creator row reads You dot Creator`() {
        // Arrange
        val transformer = createTransformer(
            account = createJointAccount(members = listOf(fixtureCreator)),
            invites = listOf("invite-a"),
        )

        // Act
        val creatorRow = transformer.transform(createMembersUM()).members
            .filterIsInstance<JointAccountMembersUM.MemberUM.Joined>()
            .single()

        // Assert
        assertThat(creatorRow.role).isEqualTo(
            combinedReference(
                resourceReference(CoreUiR.string.joint_account_members_role_you),
                stringReference(" ${StringsSigns.DOT} "),
                resourceReference(CoreUiR.string.joint_account_invite_members_creator),
            ),
        )
    }

    @Test
    fun `GIVEN another device WHEN transform THEN the creator row reads Creator and members read Joined`() {
        // Arrange
        val transformer = createTransformer(
            account = createJointAccount(members = listOf(fixtureCreator, fixtureMember)),
            invites = emptyList(),
        )

        // Act
        val joined = transformer.transform(createMembersUM()).members
            .filterIsInstance<JointAccountMembersUM.MemberUM.Joined>()

        // Assert
        assertThat(joined.map { it.role }).containsExactly(
            resourceReference(CoreUiR.string.joint_account_invite_members_creator),
            resourceReference(CoreUiR.string.joint_account_members_role_joined),
        ).inOrder()
    }

    @Test
    fun `GIVEN member row WHEN info clicked THEN the member card carries the name and address`() {
        // Arrange
        val configs = mutableListOf<MemberCardConfig>()
        val transformer = createTransformer(
            account = createJointAccount(members = listOf(fixtureCreator)),
            onMemberInfoClick = configs::add,
        )

        // Act
        transformer.transform(createMembersUM()).members
            .filterIsInstance<JointAccountMembersUM.MemberUM.Joined>()
            .single()
            .onInfoClick()

        // Assert
        val config = configs.single()
        assertThat(config.name).isEqualTo(stringReference(FIXTURE_CREATOR_NAME))
        assertThat(config.address).isEqualTo(fixtureCreator.address)
        assertThat(config.avatar.monogram).isEqualTo("I")
    }

    @Test
    fun `GIVEN confirming account of the creator WHEN transform THEN activation passes the account id`() {
        // Arrange
        val activated = mutableListOf<String>()
        val transformer = createTransformer(
            account = createJointAccount(
                status = JointAccount.Status.CONFIRMING,
                members = listOf(fixtureCreator, fixtureMember),
            ),
            invites = listOf("invite-a"),
            onActivateClick = activated::add,
        )

        // Act
        val activation = transformer.transform(createMembersUM()).activation

        // Assert
        assertThat(activation).isNotNull()
        activation?.onActivateClick?.invoke()
        assertThat(activated).containsExactly(FIXTURE_ACCOUNT_ID)
    }

    @Test
    fun `GIVEN pending account WHEN transform THEN activation is not offered`() {
        // Arrange
        val transformer = createTransformer(
            account = createJointAccount(status = JointAccount.Status.PENDING),
            invites = listOf("invite-a"),
        )

        // Act
        val actual = transformer.transform(createMembersUM())

        // Assert
        assertThat(actual.activation).isNull()
    }

    @Test
    fun `GIVEN non-creator device in invite mode WHEN transform THEN waiting title and share-safely hidden`() {
        // Arrange
        val transformer = createTransformer(
            account = createJointAccount(members = listOf(fixtureCreator, fixtureMember)),
            invites = emptyList(),
        )

        // Act
        val actual = transformer.transform(createMembersUM())

        // Assert
        assertThat(actual.title).isEqualTo(
            resourceReference(CoreUiR.string.joint_account_invite_members_waiting_title),
        )
        assertThat(actual.shareSafely).isNull()
        assertThat(actual.canArchive).isFalse()
    }

    @Test
    fun `GIVEN reference mode WHEN transform THEN neither inviting nor activation nor archiving`() {
        // Arrange
        val transformer = createTransformer(
            account = createJointAccount(
                status = JointAccount.Status.CONFIRMING,
                members = listOf(fixtureCreator, fixtureMember),
            ),
            invites = listOf("invite-a"),
            isInviteMode = false,
        )

        // Act
        val actual = transformer.transform(createMembersUM())

        // Assert
        assertThat(actual.activation).isNull()
        assertThat(actual.canArchive).isFalse()
        val slots = actual.members.filterIsInstance<JointAccountMembersUM.MemberUM.FreeSlot>()
        assertThat(slots.map { it.canInvite }).doesNotContain(true)
    }

    private fun createTransformer(
        account: JointAccount,
        invites: List<String> = emptyList(),
        isInviteMode: Boolean = true,
        onShareInviteClick: (String) -> Unit = {},
        onMemberInfoClick: (MemberCardConfig) -> Unit = {},
        onActivateClick: (String) -> Unit = {},
    ): UpdateMembersAccountTransformer = UpdateMembersAccountTransformer(
        account = account,
        invites = invites,
        isInviteMode = isInviteMode,
        onShareInviteClick = onShareInviteClick,
        onMemberInfoClick = onMemberInfoClick,
        onActivateClick = onActivateClick,
    )
}