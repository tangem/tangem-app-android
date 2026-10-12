package com.tangem.features.jointaccount.main.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.utils.StringsSigns
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.models.StatusSource
import com.tangem.features.jointaccount.main.JointAccountMembersUM
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import com.tangem.core.ui.R as CoreUiR

/**
 * The screen reads its title/progress/share-safely/archive/activation entirely from [JointAccount.status]
 * and whether this wallet is the creator: its own owner address matches the creator's, or, while that address
 * is not derived yet, it holds the account's invites — only the creating device ever does. There is no
 * "mode": every combination below is reachable from any entry point once the account's own data matches it.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JointAccountToMembersUMConverterTest {

    private val onMemberInfoClick: (JointAccountMembersUM.MemberAvatarUM, TextReference, String) -> Unit = mockk(relaxed = true)
    private val onInviteClick: (String) -> Unit = mockk(relaxed = true)
    private val onActivateClick: (String) -> Unit = mockk(relaxed = true)
    private val onShareSafelyClick: () -> Unit = mockk(relaxed = true)
    private val onArchiveClick: (String) -> Unit = mockk(relaxed = true)
    private val onCloseClick: () -> Unit = mockk(relaxed = true)

    private val converter = JointAccountToMembersUMConverter(
        callbacks = JointAccountToMembersUMConverter.Callbacks(
            onMemberInfoClick = onMemberInfoClick,
            onInviteClick = onInviteClick,
            onActivateClick = onActivateClick,
            onShareSafelyClick = onShareSafelyClick,
            onArchiveClick = onArchiveClick,
            onCloseClick = onCloseClick,
        ),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(onMemberInfoClick, onInviteClick, onActivateClick, onShareSafelyClick, onArchiveClick, onCloseClick)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Collecting {

        @Test
        fun `GIVEN creator with two free slots WHEN convert THEN own role and progress of the account`() {
            // Arrange
            val input = input(account = pendingAccount(members = listOf(creator)), invites = listOf(INVITE_A, INVITE_B))

            // Act
            val state = converter.convert(input)

            // Assert
            assertThat(state.joined).hasSize(1)
            assertThat(state.freeSlots).hasSize(2)
            val own = state.joined.first()
            assertThat(own.name).isEqualTo(stringReference(creator.name))
            assertThat(own.avatar.monogram).isEqualTo("I")
            assertThat(state.otherMembersLabel).isNotNull()
        }

        @Test
        fun `GIVEN creator on a pending account WHEN convert THEN title share-safely and archive are shown`() {
            // Arrange
            val input = input(account = pendingAccount(members = listOf(creator)), invites = listOf(INVITE_A))

            // Act
            val state = converter.convert(input)

            // Assert
            assertThat(state.header?.title).isEqualTo(resourceReference(CoreUiR.string.joint_account_invite_members_title))
            assertThat(state.header?.progress).isEqualTo(
                resourceReference(CoreUiR.string.joint_account_invite_members_subtitle, wrappedList(1, THREE_SLOTS)),
            )
            assertThat(state.shareSafely).isNotNull()
            assertThat(state.onActivateClick).isNull()
            val topBar = state.topBar as JointAccountMembersUM.TopBarUM.Dismissible
            assertThat(topBar.canArchive).isTrue()
        }

        @Test
        fun `GIVEN non-creator member on a pending account WHEN convert THEN waiting title with no share-safely or archive`() {
            // Arrange
            val input = input(account = pendingAccount(members = listOf(creator, member)), invites = emptyList())

            // Act
            val state = converter.convert(input)

            // Assert
            assertThat(state.header?.title).isEqualTo(
                resourceReference(CoreUiR.string.joint_account_invite_members_waiting_title),
            )
            assertThat(state.shareSafely).isNull()
            val topBar = state.topBar as JointAccountMembersUM.TopBarUM.Dismissible
            assertThat(topBar.canArchive).isFalse()
        }

        @Test
        fun `GIVEN invites on this device WHEN convert THEN every free slot offers its own invite`() {
            // Arrange
            val input = input(account = pendingAccount(members = listOf(creator)), invites = listOf(INVITE_A, INVITE_B))

            // Act
            val slots = converter.convert(input).freeSlots

            // Assert
            assertThat(slots.map { it.canInvite }).containsExactly(true, true)

            // Act
            slots.forEach { it.onInviteClick() }

            // Assert
            verify(exactly = 1) { onInviteClick(INVITE_A) }
            verify(exactly = 1) { onInviteClick(INVITE_B) }
        }

        @Test
        fun `GIVEN member redeemed a later invite WHEN convert THEN free slot offers the unredeemed one`() {
            // Arrange
            val input = input(
                account = pendingAccount(members = listOf(creator, member.copy(inviteId = INVITE_B))),
                invites = listOf(INVITE_A, INVITE_B),
            )

            // Act
            val slot = converter.convert(input).freeSlots.single()
            slot.onInviteClick()

            // Assert
            verify(exactly = 1) { onInviteClick(INVITE_A) }
            verify(exactly = 0) { onInviteClick(INVITE_B) }
        }

        @Test
        fun `GIVEN redeemed invite differs in case WHEN convert THEN it is still excluded`() {
            // Arrange
            val input = input(
                account = pendingAccount(members = listOf(creator, member.copy(inviteId = INVITE_B.uppercase()))),
                invites = listOf(INVITE_A, INVITE_B),
            )

            // Act
            val slot = converter.convert(input).freeSlots.single()
            slot.onInviteClick()

            // Assert
            verify(exactly = 1) { onInviteClick(INVITE_A) }
            verify(exactly = 0) { onInviteClick(INVITE_B) }
        }

        @Test
        fun `GIVEN no invites on this device WHEN convert THEN no slot can be invited`() {
            // Arrange
            val input = input(account = pendingAccount(members = listOf(creator)), invites = emptyList())

            // Act
            val slots = converter.convert(input).freeSlots

            // Assert
            assertThat(slots.map { it.canInvite }).containsExactly(false, false)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ReadyToActivate {

        @Test
        fun `GIVEN confirming account of the creator WHEN convert THEN activation is offered`() {
            // Arrange
            val input = input(
                account = pendingAccount(members = listOf(creator, member)).copy(status = JointAccount.Status.CONFIRMING),
                invites = listOf(INVITE_A),
            )

            // Act
            val state = converter.convert(input)

            // Assert
            assertThat(state.onActivateClick).isNotNull()
        }

        @Test
        fun `GIVEN confirming account WHEN activation clicked THEN the callback gets this account's id`() {
            // Arrange
            val input = input(
                account = pendingAccount(members = listOf(creator, member)).copy(status = JointAccount.Status.CONFIRMING),
                invites = listOf(INVITE_A),
            )

            // Act
            converter.convert(input).onActivateClick?.invoke()

            // Assert
            verify(exactly = 1) { onActivateClick(ACCOUNT_ID) }
        }

        @Test
        fun `GIVEN confirming account without invites WHEN convert THEN activation is not offered`() {
            // Arrange
            val input = input(
                account = pendingAccount(members = listOf(creator, member)).copy(status = JointAccount.Status.CONFIRMING),
                invites = emptyList(),
            )

            // Act
            val state = converter.convert(input)

            // Assert
            assertThat(state.onActivateClick).isNull()
        }

        @Test
        fun `GIVEN confirming account WHEN convert THEN close button is still shown, not back`() {
            // Arrange
            val input = input(
                account = pendingAccount(members = listOf(creator, member)).copy(status = JointAccount.Status.CONFIRMING),
                invites = listOf(INVITE_A),
            )

            // Act
            val state = converter.convert(input)

            // Assert
            assertThat(state.topBar).isInstanceOf(JointAccountMembersUM.TopBarUM.Dismissible::class.java)
        }

        @Test
        fun `GIVEN confirming account of the creator WHEN convert THEN share-safely is not offered`() {
            // Arrange — every slot is filled once the account reaches CONFIRMING, so there is nothing left
            // to share.
            val input = input(
                account = pendingAccount(members = listOf(creator, member)).copy(status = JointAccount.Status.CONFIRMING),
                invites = listOf(INVITE_A),
            )

            // Act
            val state = converter.convert(input)

            // Assert
            assertThat(state.shareSafely).isNull()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Completed {

        @Test
        fun `GIVEN active account WHEN convert THEN plain members title with neither progress share-safely nor archiving`() {
            // Arrange
            val input = input(
                account = pendingAccount(members = listOf(creator, member)).copy(status = JointAccount.Status.ACTIVE),
                invites = listOf(INVITE_A),
            )

            // Act
            val state = converter.convert(input)

            // Assert
            assertThat(state.header).isNull()
            assertThat(state.shareSafely).isNull()
            assertThat(state.onActivateClick).isNull()
            val topBar = state.topBar as JointAccountMembersUM.TopBarUM.Titled
            assertThat(topBar.title).isEqualTo(resourceReference(CoreUiR.string.joint_account_members_title))
        }

        @Test
        fun `GIVEN active account of the creator WHEN convert THEN it looks the same as any other member`() {
            // Arrange
            val creatorInput = input(
                account = pendingAccount(members = listOf(creator, member)).copy(status = JointAccount.Status.ACTIVE),
                invites = listOf(INVITE_A),
            )
            val memberInput = creatorInput.copy(invites = emptyList())

            // Act
            val creatorState = converter.convert(creatorInput)
            val memberState = converter.convert(memberInput)

            // Assert
            assertThat(creatorState.topBar).isEqualTo(memberState.topBar)
            assertThat(creatorState.shareSafely).isEqualTo(memberState.shareSafely)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a status with no design of its own WHEN convert THEN it falls back to the completed read-only roster`(
            status: JointAccount.Status,
        ) {
            // Arrange
            val input = input(
                account = pendingAccount(members = listOf(creator, member)).copy(status = status),
                invites = listOf(INVITE_A),
            )

            // Act
            val state = converter.convert(input)

            // Assert
            assertThat(state.topBar).isInstanceOf(JointAccountMembersUM.TopBarUM.Titled::class.java)
            assertThat(state.header).isNull()
        }

        private fun provideTestModels() = listOf(JointAccount.Status.CANCELLED, JointAccount.Status.UNKNOWN)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OwnAddress {

        @Test
        fun `GIVEN own address of a joined member WHEN convert THEN own row is You and creator stays Creator`() {
            // Arrange
            val input = input(
                account = pendingAccount(members = listOf(creator, member)),
                invites = emptyList(),
                ownAddress = member.address.lowercase(),
            )

            // Act
            val state = converter.convert(input)

            // Assert
            assertThat(state.joined.map { it.role }).containsExactly(
                resourceReference(CoreUiR.string.joint_account_invite_members_creator),
                resourceReference(CoreUiR.string.joint_account_members_role_you),
            ).inOrder()
            val topBar = state.topBar as JointAccountMembersUM.TopBarUM.Dismissible
            assertThat(topBar.canArchive).isFalse()
        }

        @Test
        fun `GIVEN own address of the creator without invites WHEN convert THEN creator rights and You Creator`() {
            // Arrange
            val input = input(
                account = pendingAccount(members = listOf(creator, member)),
                invites = emptyList(),
                ownAddress = creator.address,
            )

            // Act
            val state = converter.convert(input)

            // Assert
            assertThat(state.joined.first().role).isEqualTo(
                combinedReference(
                    resourceReference(CoreUiR.string.joint_account_members_role_you),
                    stringReference(" ${StringsSigns.DOT} "),
                    resourceReference(CoreUiR.string.joint_account_invite_members_creator),
                ),
            )
            assertThat(state.joined[1].role).isEqualTo(resourceReference(CoreUiR.string.joint_account_members_role_joined))
            assertThat(state.header?.title).isEqualTo(resourceReference(CoreUiR.string.joint_account_invite_members_title))
            val topBar = state.topBar as JointAccountMembersUM.TopBarUM.Dismissible
            assertThat(topBar.canArchive).isTrue()
        }

        @Test
        fun `GIVEN own address of a member but invites on this device WHEN convert THEN the address wins`() {
            // Arrange
            val input = input(
                account = pendingAccount(members = listOf(creator, member)),
                invites = listOf(INVITE_A),
                ownAddress = member.address,
            )

            // Act
            val state = converter.convert(input)

            // Assert
            assertThat(state.header?.title).isEqualTo(
                resourceReference(CoreUiR.string.joint_account_invite_members_waiting_title),
            )
            assertThat(state.freeSlots.none { it.canInvite }).isTrue()
        }
    }

    private fun input(account: JointAccount, invites: List<String>, ownAddress: String? = null) =
        JointAccountToMembersUMConverter.Input(account = account, invites = invites, ownAddress = ownAddress)

    private fun pendingAccount(members: List<JointAccount.Member>) = JointAccount(
        cryptoAccountId = ACCOUNT_ID,
        membersCount = THREE_SLOTS,
        threshold = 2,
        safeAddress = null,
        status = JointAccount.Status.PENDING,
        members = members,
        source = StatusSource.ACTUAL,
    )

    private companion object {
        const val ACCOUNT_ID = "a1"
        const val THREE_SLOTS = 3
        const val INVITE_A = "invite-a"
        const val INVITE_B = "invite-b"

        val creator = JointAccount.Member(
            name = "Ivan",
            address = "0xC0FFEE",
            role = JointAccount.Role.CREATOR,
        )
        val member = JointAccount.Member(
            name = "Dasha",
            address = "0xDA5HA",
            role = JointAccount.Role.MEMBER,
        )
    }
}