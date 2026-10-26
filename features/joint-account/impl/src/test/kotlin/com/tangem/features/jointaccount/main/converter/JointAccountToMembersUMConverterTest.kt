package com.tangem.features.jointaccount.main.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointAccountInvite
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.jointaccount.main.JointAccountMembersUM
import com.tangem.features.jointaccount.main.JointAccountMembersUM.SlotStatusUM
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.joda.time.DateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import com.tangem.core.ui.R as CoreUiR

/**
 * The screen reads its title/activation entirely from [JointAccount.status] and whether this wallet is the creator:
 * its own owner address matches the creator's, or, while that address is not derived yet, the backend revealed the
 * members' slots — it only does so to the creator. There is no "mode": every combination below is reachable from any
 * entry point once the account's own data matches it.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JointAccountToMembersUMConverterTest {

    private val onMemberInfoClick: (JointAccountMembersUM.MemberAvatarUM, TextReference, String) -> Unit =
        mockk(relaxed = true)
    private val onConfirmAddressClick: (String) -> Unit = mockk(relaxed = true)
    private val onApprovalsInfoClick: (Int, Int) -> Unit = mockk(relaxed = true)
    private val onCloseClick: () -> Unit = mockk(relaxed = true)

    private val converter = JointAccountToMembersUMConverter(
        callbacks = JointAccountToMembersUMConverter.Callbacks(
            onMemberInfoClick = onMemberInfoClick,
            onConfirmAddressClick = onConfirmAddressClick,
            onApprovalsInfoClick = onApprovalsInfoClick,
            onCloseClick = onCloseClick,
        ),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(onMemberInfoClick, onConfirmAddressClick, onApprovalsInfoClick, onCloseClick)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Collecting {

        @Test
        fun `GIVEN creator with two free slots WHEN convert THEN own role and progress of the account`() {
            // Arrange
            val input = input(account = pendingAccount(members = listOf(creator)))

            // Act
            val state = converter.convert(input)

            // Assert
            assertThat(state.joined).hasSize(1)
            assertThat(state.freeSlots).hasSize(2)
            val own = state.joined.first()
            assertThat(own.name).isEqualTo(stringReference(creator.name))
            assertThat(own.avatar.monogram).isEqualTo("I")
        }

        @Test
        fun `GIVEN creator on a pending account WHEN convert THEN creator title is shown`() {
            // Arrange
            val input = input(account = pendingAccount(members = listOf(creator)))

            // Act
            val state = converter.convert(input)

            // Assert
            assertThat(state.header?.title).isEqualTo(
                resourceReference(CoreUiR.string.joint_account_invite_members_title),
            )
            assertThat(state.topBar).isInstanceOf(JointAccountMembersUM.TopBarUM.Dismissible::class.java)
        }

        @Test
        fun `GIVEN non-creator member on a pending account WHEN convert THEN waiting title is shown`() {
            // Arrange
            val input = input(account = memberViewAccount(members = listOf(creator, member)))

            // Act
            val state = converter.convert(input)

            // Assert
            assertThat(state.header?.title).isEqualTo(
                resourceReference(CoreUiR.string.joint_account_invite_members_waiting_title),
            )
            assertThat(state.topBar).isInstanceOf(JointAccountMembersUM.TopBarUM.Dismissible::class.java)
        }

        @Test
        fun `GIVEN account name not arrived yet WHEN convert THEN no slot can be invited`() {
            // Arrange
            val input = input(account = pendingAccount(members = listOf(creator)), accountName = null)

            // Act
            val slots = converter.convert(input).freeSlots

            // Assert
            assertThat(slots.map { it.action }).containsExactly(null, null)
        }

        @Test
        fun `GIVEN no slots revealed and no own address WHEN convert THEN no slot can be invited`() {
            // Arrange
            val input = input(account = memberViewAccount(members = listOf(creator)))

            // Act
            val slots = converter.convert(input).freeSlots

            // Assert
            assertThat(slots.map { it.action }).containsExactly(null, null)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ReadyToActivate {

        @Test
        fun `GIVEN confirming account WHEN confirm clicked THEN the callback gets this account's id`() {
            // Arrange
            val input = input(
                account = pendingAccount(members = listOf(creator, member))
                    .copy(status = JointAccount.Status.CONFIRMING),
            )

            // Act
            converter.convert(input).footer?.button?.onClick?.invoke()

            // Assert
            verify(exactly = 1) { onConfirmAddressClick(ACCOUNT_ID) }
        }

        @Test
        fun `GIVEN confirming account WHEN convert THEN close button is still shown, not back`() {
            // Arrange
            val input = input(
                account = pendingAccount(members = listOf(creator, member))
                    .copy(status = JointAccount.Status.CONFIRMING),
            )

            // Act
            val state = converter.convert(input)

            // Assert
            assertThat(state.topBar).isInstanceOf(JointAccountMembersUM.TopBarUM.Dismissible::class.java)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Footer {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN account status, role and progress WHEN convert THEN footer matches`(model: FooterModel) {
            // Arrange
            val account = if (model.isCreator) {
                pendingAccount(members = model.members)
            } else {
                memberViewAccount(members = model.members)
            }
            val input = input(
                account = account.copy(status = model.status, invites = model.invites),
                ownAddress = if (model.isCreator) creator.address else member.address,
            )

            // Act
            val footer = converter.convert(input).footer

            // Assert
            val actual = footer?.let { FooterExpectation(hint = it.hint, isButtonEnabled = it.button?.isEnabled) }
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            FooterModel(
                status = JointAccount.Status.PENDING,
                isCreator = true,
                expected = FooterExpectation(hint = INVITE_TWO, isButtonEnabled = false),
            ),
            FooterModel(
                status = JointAccount.Status.PENDING,
                isCreator = true,
                invites = listOf(liveInvite(slot = 1)),
                expected = FooterExpectation(hint = INVITE_MORE_ONE, isButtonEnabled = false),
            ),
            FooterModel(
                status = JointAccount.Status.PENDING,
                isCreator = true,
                invites = listOf(expiredInvite(slot = 2)),
                expected = FooterExpectation(hint = INVITE_MORE_ONE, isButtonEnabled = false),
            ),
            FooterModel(
                status = JointAccount.Status.PENDING,
                isCreator = true,
                invites = listOf(liveInvite(slot = 1), expiredInvite(slot = 2)),
                expected = FooterExpectation(hint = AWAITING_TWO, isButtonEnabled = false),
            ),
            FooterModel(
                status = JointAccount.Status.PENDING,
                isCreator = false,
                expected = FooterExpectation(hint = AWAITING_TWO, isButtonEnabled = false),
            ),
            FooterModel(
                status = JointAccount.Status.PENDING,
                isCreator = true,
                members = listOf(creator, member, thirdMember),
                expected = FooterExpectation(hint = EVERYONE_JOINED, isButtonEnabled = false),
            ),
            FooterModel(
                status = JointAccount.Status.PENDING,
                isCreator = false,
                members = listOf(creator, member, thirdMember),
                expected = FooterExpectation(hint = EVERYONE_JOINED, isButtonEnabled = false),
            ),
            FooterModel(
                status = JointAccount.Status.CONFIRMING,
                isCreator = true,
                members = listOf(creator, member, thirdMember),
                expected = FooterExpectation(hint = null, isButtonEnabled = true),
            ),
            FooterModel(
                status = JointAccount.Status.CONFIRMING,
                isCreator = false,
                members = listOf(creator, member, thirdMember),
                expected = FooterExpectation(hint = null, isButtonEnabled = true),
            ),
            FooterModel(
                status = JointAccount.Status.CONFIRMING,
                isCreator = false,
                members = listOf(creator, member.copy(signature = SIGNATURE), thirdMember),
                expected = FooterExpectation(hint = STILL_NEED_TO_CONFIRM_TWO, isButtonEnabled = null),
            ),
            FooterModel(
                status = JointAccount.Status.CONFIRMING,
                isCreator = true,
                members = listOf(
                    creator.copy(signature = SIGNATURE),
                    member.copy(signature = SIGNATURE),
                    thirdMember,
                ),
                expected = FooterExpectation(hint = STILL_NEED_TO_CONFIRM_ONE, isButtonEnabled = null),
            ),
            FooterModel(status = JointAccount.Status.ACTIVE, isCreator = true, expected = null),
            FooterModel(status = JointAccount.Status.ACTIVE, isCreator = false, expected = null),
        )
    }

    data class FooterModel(
        val status: JointAccount.Status,
        val isCreator: Boolean,
        val members: List<JointAccount.Member> = listOf(creator),
        val invites: List<JointAccountInvite> = emptyList(),
        val expected: FooterExpectation?,
    )

    data class FooterExpectation(val hint: TextReference?, val isButtonEnabled: Boolean?)

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Completed {

        @Test
        fun `GIVEN active account WHEN convert THEN plain members title in the top bar`() {
            // Arrange
            val input = input(
                account = pendingAccount(members = listOf(creator, member)).copy(status = JointAccount.Status.ACTIVE),
            )

            // Act
            val state = converter.convert(input)

            // Assert
            assertThat(state.header).isNull()
            val topBar = state.topBar as JointAccountMembersUM.TopBarUM.Titled
            assertThat(topBar.title).isEqualTo(resourceReference(CoreUiR.string.joint_account_members_title))
        }

        @Test
        fun `GIVEN active account WHEN convert THEN no member row has a status badge`() {
            // Arrange
            val input = input(
                account = pendingAccount(members = listOf(creator, member.copy(signature = SIGNATURE)))
                    .copy(status = JointAccount.Status.ACTIVE),
            )

            // Act
            val statuses = converter.convert(input).joined.map { it.status }

            // Assert
            assertThat(statuses).containsExactly(
                JointAccountMembersUM.MemberStatusUM.None,
                JointAccountMembersUM.MemberStatusUM.None,
            )
        }

        @Test
        fun `GIVEN active account of the creator WHEN convert THEN it looks the same as any other member`() {
            // Arrange
            val creatorInput = input(
                account = pendingAccount(members = listOf(creator, member)).copy(status = JointAccount.Status.ACTIVE),
            )
            val memberInput = creatorInput.copy(
                account = memberViewAccount(members = listOf(creator, member))
                    .copy(status = JointAccount.Status.ACTIVE),
            )

            // Act
            val creatorState = converter.convert(creatorInput)
            val memberState = converter.convert(memberInput)

            // Assert
            assertThat(creatorState.topBar).isEqualTo(memberState.topBar)
        }

        @Test
        fun `GIVEN creator of an active account with a free slot WHEN convert THEN the slot offers no button`() {
            // Arrange
            val input = input(
                account = pendingAccount(members = listOf(creator)).copy(
                    status = JointAccount.Status.ACTIVE,
                    invites = listOf(liveInvite(slot = 1)),
                ),
                ownAddress = creator.address,
            )

            // Act
            val slots = converter.convert(input).freeSlots

            // Assert
            assertThat(slots.map { it.status to it.action }).containsExactly(
                SlotStatusUM.WaitingToJoin to null,
                SlotStatusUM.WaitingToJoin to null,
            )
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a status with no design of its own WHEN convert THEN it falls back to the completed read-only roster`(
            status: JointAccount.Status,
        ) {
            // Arrange
            val input = input(account = pendingAccount(members = listOf(creator, member)).copy(status = status))

            // Act
            val state = converter.convert(input)

            // Assert
            assertThat(state.topBar).isInstanceOf(JointAccountMembersUM.TopBarUM.Titled::class.java)
            assertThat(state.header).isNull()
            assertThat(state.joined.map { it.status }).containsExactly(
                JointAccountMembersUM.MemberStatusUM.None,
                JointAccountMembersUM.MemberStatusUM.None,
            )
        }

        private fun provideTestModels() = listOf(JointAccount.Status.CANCELLED, JointAccount.Status.UNKNOWN)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OwnAddress {

        @Test
        fun `GIVEN own address of a joined member WHEN convert THEN own row is You and creator row is Creator`() {
            // Arrange
            val input = input(
                account = memberViewAccount(members = listOf(creator, member)),
                ownAddress = member.address.lowercase(),
            )

            // Act
            val state = converter.convert(input)

            // Assert
            assertThat(state.joined.map { it.isYou to it.isCreator }).containsExactly(
                false to true,
                true to false,
            ).inOrder()
            assertThat(state.topBar).isInstanceOf(JointAccountMembersUM.TopBarUM.Dismissible::class.java)
        }

        @Test
        fun `GIVEN creator address without slots WHEN convert THEN creator rights and own row is You and Creator`() {
            // Arrange
            val input = input(
                account = memberViewAccount(members = listOf(creator, member)),
                ownAddress = creator.address,
            )

            // Act
            val state = converter.convert(input)

            // Assert
            assertThat(state.joined.map { it.isYou to it.isCreator }).containsExactly(
                true to true,
                false to false,
            ).inOrder()
            assertThat(state.joined.map { it.status }.distinct())
                .containsExactly(JointAccountMembersUM.MemberStatusUM.Joined)
            assertThat(state.header?.title).isEqualTo(
                resourceReference(CoreUiR.string.joint_account_invite_members_title),
            )
            assertThat(state.freeSlots.single().action).isNull()
            assertThat(state.topBar).isInstanceOf(JointAccountMembersUM.TopBarUM.Dismissible::class.java)
        }

        @Test
        fun `GIVEN own address of a member but slots revealed WHEN convert THEN the address wins`() {
            // Arrange
            val input = input(
                account = pendingAccount(members = listOf(creator, member)),
                ownAddress = member.address,
            )

            // Act
            val state = converter.convert(input)

            // Assert
            assertThat(state.header?.title).isEqualTo(
                resourceReference(CoreUiR.string.joint_account_invite_members_waiting_title),
            )
            assertThat(state.freeSlots.map { it.action }.distinct()).containsExactly(null)
        }

        @Test
        fun `GIVEN own address unknown and slots revealed WHEN convert THEN this device is the creator`() {
            // Arrange
            val input = input(account = pendingAccount(members = listOf(creator, member)), ownAddress = null)

            // Act
            val state = converter.convert(input)

            // Assert
            assertThat(state.header?.title).isEqualTo(
                resourceReference(CoreUiR.string.joint_account_invite_members_title),
            )
            assertThat(state.joined.map { it.isYou }).containsExactly(false, false)
            assertThat(state.freeSlots.single().action).isNull()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Confirming {

        private val confirmingAccount = pendingAccount(
            members = listOf(creator, member.copy(signature = SIGNATURE), thirdMember),
        ).copy(status = JointAccount.Status.CONFIRMING)

        @Test
        fun `GIVEN confirming account WHEN convert THEN header is Confirm the account with the address subtitle`() {
            // Arrange
            val input = input(account = confirmingAccount)

            // Act
            val header = converter.convert(input).header

            // Assert
            assertThat(header).isEqualTo(
                JointAccountMembersUM.HeaderUM(
                    title = resourceReference(CoreUiR.string.joint_account_confirm_account_title),
                    subtitle = resourceReference(CoreUiR.string.joint_account_confirm_account_subtitle),
                ),
            )
        }

        @Test
        fun `GIVEN confirming account WHEN convert THEN verified members are Confirmed and the rest Not confirmed`() {
            // Arrange
            val input = input(account = confirmingAccount)

            // Act
            val statuses = converter.convert(input).joined.map { it.status }

            // Assert
            assertThat(statuses).containsExactly(
                JointAccountMembersUM.MemberStatusUM.NotConfirmed,
                JointAccountMembersUM.MemberStatusUM.Confirmed,
                JointAccountMembersUM.MemberStatusUM.NotConfirmed,
            ).inOrder()
        }

        @Test
        fun `GIVEN pending account WHEN convert THEN every joined member is Joined`() {
            // Arrange
            val input = input(account = pendingAccount(members = listOf(creator, member.copy(signature = SIGNATURE))))

            // Act
            val statuses = converter.convert(input).joined.map { it.status }.distinct()

            // Assert
            assertThat(statuses).containsExactly(JointAccountMembersUM.MemberStatusUM.Joined)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SlotStatus {

        @Test
        fun `GIVEN no invite for the slot WHEN convert THEN slot reads Not invited`() {
            // Act
            val slot = convertSlot(invites = emptyList())

            // Assert
            assertThat(slot.status).isEqualTo(SlotStatusUM.NotInvited)
            assertThat(slot.action).isNull()
        }

        @Test
        fun `GIVEN an invite of another slot only WHEN convert THEN slot reads Not invited`() {
            // Act
            val slot = convertSlot(invites = listOf(liveInvite(slot = 2)))

            // Assert
            assertThat(slot.status).isEqualTo(SlotStatusUM.NotInvited)
            assertThat(slot.action).isNull()
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN invite past its deadline WHEN convert THEN slot reads Expired`(
            expiresAt: DateTime,
        ) {
            // Act
            val slot = convertSlot(invites = listOf(JointAccountInvite(slot = 1, expiresAtMillis = expiresAt.millis)))

            // Assert
            assertThat(slot.status).isEqualTo(SlotStatusUM.Expired)
            assertThat(slot.action).isNull()
        }

        private fun provideTestModels() = listOf(NOW.minusMinutes(1), NOW)

        @Test
        fun `GIVEN live invite WHEN convert THEN slot reads Invited`() {
            // Act
            val slot = convertSlot(invites = listOf(liveInvite(slot = 1)))

            // Assert
            assertThat(slot.status).isInstanceOf(SlotStatusUM.Invited::class.java)
            assertThat(slot.action).isNull()
        }

        @Test
        fun `GIVEN invite expiring later today WHEN convert THEN label is Until today with the time`() {
            // Arrange
            val expiresAt = NOW.plusHours(1)

            // Act
            val slot = convertSlot(invites = listOf(JointAccountInvite(slot = 1, expiresAtMillis = expiresAt.millis)))

            // Assert
            assertThat(slot.status).isEqualTo(
                SlotStatusUM.Invited(
                    label = resourceReference(
                        id = CoreUiR.string.joint_account_invite_members_until_today,
                        formatArgs = wrappedList(DateTimeFormatters.timeFormatter.print(expiresAt)),
                    ),
                ),
            )
        }

        @Test
        fun `GIVEN invite expiring tomorrow WHEN convert THEN label is Until tomorrow with the time`() {
            // Arrange
            val expiresAt = NOW.plusDays(1).withHourOfDay(9)

            // Act
            val slot = convertSlot(invites = listOf(JointAccountInvite(slot = 1, expiresAtMillis = expiresAt.millis)))

            // Assert
            assertThat(slot.status).isEqualTo(
                SlotStatusUM.Invited(
                    label = resourceReference(
                        id = CoreUiR.string.joint_account_invite_members_until_tomorrow,
                        formatArgs = wrappedList(DateTimeFormatters.timeFormatter.print(expiresAt)),
                    ),
                ),
            )
        }

        @Test
        fun `GIVEN invite expiring on another day WHEN convert THEN label is Until with the full date`() {
            // Arrange
            val expiresAt = NOW.plusDays(3)

            // Act
            val slot = convertSlot(invites = listOf(JointAccountInvite(slot = 1, expiresAtMillis = expiresAt.millis)))

            // Assert
            assertThat(slot.status).isEqualTo(
                SlotStatusUM.Invited(
                    label = resourceReference(
                        id = CoreUiR.string.joint_account_invite_members_until,
                        formatArgs = wrappedList(FreeSlotUMFactory.dateTimeFormatter().print(expiresAt)),
                    ),
                ),
            )
        }

        @Test
        fun `GIVEN a member, not the creator WHEN convert THEN free slots read Waiting to join without buttons`() {
            // Arrange
            val input = input(
                account = memberViewAccount(members = listOf(creator, member)),
                ownAddress = member.address,
            )

            // Act
            val slots = converter.convert(input).freeSlots

            // Assert
            assertThat(slots.map { it.status }.distinct()).containsExactly(SlotStatusUM.WaitingToJoin)
            assertThat(slots.map { it.action }.distinct()).containsExactly(null)
        }

        private fun convertSlot(invites: List<JointAccountInvite>): JointAccountMembersUM.MemberUM.FreeSlot {
            val input = input(
                account = pendingAccount(members = listOf(creator)).copy(membersCount = 2, invites = invites),
                ownAddress = creator.address,
            )
            return converter.convert(input).freeSlots.single()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SlotNumbering {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN joined members WHEN convert THEN free slots are numbered as the viewer sees them`(
            model: NumberingModel,
        ) {
            // Arrange
            val account = pendingAccount(members = model.members).copy(membersCount = model.membersCount)

            // Act
            val slots = converter.convert(input(account = account)).freeSlots

            // Assert
            assertThat(slots.map { it.id to it.title }).containsExactlyElementsIn(
                model.expectedNumbers.map { number ->
                    "slot_$number" to resourceReference(
                        id = CoreUiR.string.joint_account_invite_members_member_number,
                        formatArgs = wrappedList(number),
                    )
                },
            ).inOrder()
        }

        private fun provideTestModels() = listOf(
            NumberingModel(
                members = listOf(creator.copy(slot = null)),
                membersCount = FIVE_SLOTS,
                expectedNumbers = listOf(1, 2, 3, 4),
            ),
            NumberingModel(
                members = listOf(creator, member, thirdMember).map { it.copy(slot = null) },
                membersCount = FIVE_SLOTS,
                expectedNumbers = listOf(3, 4),
            ),
            NumberingModel(
                members = listOf(creator, member.copy(slot = 2)),
                membersCount = THREE_SLOTS,
                expectedNumbers = listOf(1),
            ),
            NumberingModel(
                members = listOf(creator, member.copy(slot = 3)),
                membersCount = FIVE_SLOTS,
                expectedNumbers = listOf(1, 2, 4),
            ),
        )
    }

    data class NumberingModel(
        val members: List<JointAccount.Member>,
        val membersCount: Int,
        val expectedNumbers: List<Int>,
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SectionLabels {

        @Test
        fun `GIVEN joined members and free slots WHEN convert THEN one Members section for both`() {
            // Arrange
            val input = input(account = pendingAccount(members = listOf(creator)))

            // Act
            val state = converter.convert(input)

            // Assert
            assertThat(state.membersLabel).isEqualTo(resourceReference(CoreUiR.string.joint_account_members_title))
            assertThat(state.freeSlots).hasSize(2)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SummaryCard {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN account record WHEN convert THEN card shows its name, icon, size and approval rule`(
            status: JointAccount.Status,
        ) {
            // Arrange
            val input = input(
                account = pendingAccount(members = listOf(creator, member)).copy(status = status),
                accountName = ACCOUNT_NAME,
                accountIcon = ACCOUNT_ICON,
            )
            val expected = JointAccountMembersUM.SummaryCardUM(
                icon = AccountIconUM.CryptoPortfolio(value = ACCOUNT_ICON.value, color = ACCOUNT_ICON.color),
                name = stringReference(ACCOUNT_NAME.value),
                membersCount = pluralReference(
                    id = CoreUiR.plurals.joint_account_members_count,
                    count = THREE_SLOTS,
                    formatArgs = wrappedList(THREE_SLOTS),
                ),
                approvals = resourceReference(
                    id = CoreUiR.string.joint_account_invite_members_approvals_value,
                    formatArgs = wrappedList(2, THREE_SLOTS),
                ),
                onApprovalsInfoClick = NO_OP,
            )

            // Act
            val card = converter.convert(input).summaryCard

            // Assert
            assertThat(card?.copy(onApprovalsInfoClick = NO_OP)).isEqualTo(expected)
        }

        @Test
        fun `GIVEN card WHEN approvals info clicked THEN callback gets threshold and total`() {
            // Arrange
            val input = input(
                account = pendingAccount(members = listOf(creator)),
                accountName = ACCOUNT_NAME,
                accountIcon = ACCOUNT_ICON,
            )

            // Act
            converter.convert(input).summaryCard!!.onApprovalsInfoClick()

            // Assert
            verify(exactly = 1) { onApprovalsInfoClick(2, THREE_SLOTS) }
        }

        @Test
        fun `GIVEN account record not arrived yet WHEN convert THEN no card`() {
            // Arrange
            val input = input(account = pendingAccount(members = listOf(creator)), accountName = null)

            // Act
            val state = converter.convert(input)

            // Assert
            assertThat(state.summaryCard).isNull()
        }

        private fun provideTestModels() = listOf(
            JointAccount.Status.PENDING,
            JointAccount.Status.CONFIRMING,
            JointAccount.Status.ACTIVE,
        )
    }

    private fun input(
        account: JointAccount,
        ownAddress: String? = null,
        accountName: AccountName? = ACCOUNT_NAME,
        accountIcon: CryptoPortfolioIcon? = null,
    ) = JointAccountToMembersUMConverter.Input(
        account = account,
        ownAddress = ownAddress,
        accountName = accountName,
        accountIcon = accountIcon,
        nowMillis = NOW.millis,
    )

    /** The account as its creator receives it: every member carries its slot */
    private fun pendingAccount(members: List<JointAccount.Member>) = JointAccount(
        cryptoAccountId = ACCOUNT_ID,
        membersCount = THREE_SLOTS,
        threshold = 2,
        safeAddress = null,
        status = JointAccount.Status.PENDING,
        members = members,
        source = StatusSource.ACTUAL,
    )

    /** The account as anyone but its creator receives it: the backend withholds the slots */
    private fun memberViewAccount(members: List<JointAccount.Member>) =
        pendingAccount(members = members.map { it.copy(slot = null) })

    private companion object {
        const val ACCOUNT_ID = "a1"
        const val THREE_SLOTS = 3
        const val FIVE_SLOTS = 5
        const val SIGNATURE = "0xsigned"

        val NOW: DateTime = DateTime(2026, 10, 20, 12, 0)

        val INVITE_TWO = pluralReference(
            id = CoreUiR.plurals.joint_account_invite_members_invite_count,
            count = 2,
            formatArgs = wrappedList(2),
        )
        val INVITE_MORE_ONE = pluralReference(
            id = CoreUiR.plurals.joint_account_invite_members_invite_more,
            count = 1,
            formatArgs = wrappedList(1),
        )
        val AWAITING_TWO = pluralReference(
            id = CoreUiR.plurals.joint_account_invite_members_awaiting_to_join,
            count = 2,
            formatArgs = wrappedList(2),
        )
        val EVERYONE_JOINED = resourceReference(CoreUiR.string.joint_account_invite_members_everyone_joined)
        val STILL_NEED_TO_CONFIRM_TWO = pluralReference(
            id = CoreUiR.plurals.joint_account_members_still_need_to_confirm,
            count = 2,
            formatArgs = wrappedList(2),
        )
        val STILL_NEED_TO_CONFIRM_ONE = pluralReference(
            id = CoreUiR.plurals.joint_account_members_still_need_to_confirm,
            count = 1,
            formatArgs = wrappedList(1),
        )

        val NO_OP: () -> Unit = {}
        val ACCOUNT_NAME = AccountName.Custom(value = "Family savings").getOrNull()!!
        val ACCOUNT_ICON = CryptoPortfolioIcon.ofCustomAccount(
            value = CryptoPortfolioIcon.Icon.Family,
            color = CryptoPortfolioIcon.Color.CandyGrapeFizz,
        )

        val creator = JointAccount.Member(
            name = "Ivan",
            address = "0xC0FFEE",
            role = JointAccount.Role.CREATOR,
            slot = JointAccount.CREATOR_SLOT,
        )
        val member = JointAccount.Member(
            name = "Dasha",
            address = "0xDA5HA",
            role = JointAccount.Role.MEMBER,
            slot = 1,
        )
        val thirdMember = member.copy(name = "Bob", address = "0xB0B", slot = 2)

        fun liveInvite(slot: Int) = JointAccountInvite(slot = slot, expiresAtMillis = NOW.plusDays(3).millis)

        fun expiredInvite(slot: Int) = JointAccountInvite(slot = slot, expiresAtMillis = NOW.minusDays(1).millis)
    }
}