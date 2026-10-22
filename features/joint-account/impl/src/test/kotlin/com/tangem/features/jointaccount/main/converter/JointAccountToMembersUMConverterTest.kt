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
import com.tangem.features.jointaccount.main.state.transformers.click
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.joda.time.DateTime
import org.junit.jupiter.params.ParameterizedTest
import com.tangem.core.ui.R as CoreUiR

/**
 * The screen reads its title/activation entirely from [JointAccount.status]
 * and whether this wallet is the creator: its own owner address matches the creator's, or, while that address
 * is not derived yet, it holds the account's invites — only the creating device ever does. There is no
 * "mode": every combination below is reachable from any entry point once the account's own data matches it.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JointAccountToMembersUMConverterTest {

    private val onMemberInfoClick: (JointAccountMembersUM.MemberAvatarUM, TextReference, String) -> Unit = mockk(relaxed = true)
    private val onInviteClick: (String, TextReference) -> Unit = mockk(relaxed = true)
    private val onConfirmAddressClick: (String) -> Unit = mockk(relaxed = true)
    private val onApprovalsInfoClick: (Int, Int) -> Unit = mockk(relaxed = true)
    private val onCloseClick: () -> Unit = mockk(relaxed = true)

    private val converter = JointAccountToMembersUMConverter(
        callbacks = JointAccountToMembersUMConverter.Callbacks(
            onMemberInfoClick = onMemberInfoClick,
            onInviteClick = onInviteClick,
            onConfirmAddressClick = onConfirmAddressClick,
            onApprovalsInfoClick = onApprovalsInfoClick,
            onCloseClick = onCloseClick,
        ),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(onMemberInfoClick, onInviteClick, onConfirmAddressClick, onApprovalsInfoClick, onCloseClick)
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
        }

        @Test
        fun `GIVEN creator on a pending account WHEN convert THEN creator title is shown`() {
            // Arrange
            val input = input(account = pendingAccount(members = listOf(creator)), invites = listOf(INVITE_A))

            // Act
            val state = converter.convert(input)

            // Assert
            assertThat(state.header?.title).isEqualTo(resourceReference(CoreUiR.string.joint_account_invite_members_title))
            assertThat(state.topBar).isInstanceOf(JointAccountMembersUM.TopBarUM.Dismissible::class.java)
        }

        @Test
        fun `GIVEN non-creator member on a pending account WHEN convert THEN waiting title is shown`() {
            // Arrange
            val input = input(account = pendingAccount(members = listOf(creator, member)), invites = emptyList())

            // Act
            val state = converter.convert(input)

            // Assert
            assertThat(state.header?.title).isEqualTo(
                resourceReference(CoreUiR.string.joint_account_invite_members_waiting_title),
            )
            assertThat(state.topBar).isInstanceOf(JointAccountMembersUM.TopBarUM.Dismissible::class.java)
        }

        @Test
        fun `GIVEN invites on this device WHEN convert THEN every free slot offers its own invite`() {
            // Arrange
            val input = input(account = pendingAccount(members = listOf(creator)), invites = listOf(INVITE_A, INVITE_B))

            // Act
            val slots = converter.convert(input).freeSlots

            // Assert
            assertThat(slots.map { it.action }.all { it is JointAccountMembersUM.SlotActionUM.Invite }).isTrue()

            // Act
            slots.forEach { it.action.click() }

            // Assert
            verify(exactly = 1) { onInviteClick(INVITE_A, ACCOUNT_NAME_REFERENCE) }
            verify(exactly = 1) { onInviteClick(INVITE_B, ACCOUNT_NAME_REFERENCE) }
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
            slot.action.click()

            // Assert
            verify(exactly = 1) { onInviteClick(INVITE_A, ACCOUNT_NAME_REFERENCE) }
            verify(exactly = 0) { onInviteClick(INVITE_B, ACCOUNT_NAME_REFERENCE) }
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
            slot.action.click()

            // Assert
            verify(exactly = 1) { onInviteClick(INVITE_A, ACCOUNT_NAME_REFERENCE) }
            verify(exactly = 0) { onInviteClick(INVITE_B, ACCOUNT_NAME_REFERENCE) }
        }

        @Test
        fun `GIVEN account name not arrived yet WHEN convert THEN no slot can be invited`() {
            // Arrange
            val input = input(
                account = pendingAccount(members = listOf(creator)),
                invites = listOf(INVITE_A, INVITE_B),
                accountName = null,
            )

            // Act
            val slots = converter.convert(input).freeSlots

            // Assert
            assertThat(slots.map { it.action }).containsExactly(null, null)
        }

        @Test
        fun `GIVEN no invites on this device WHEN convert THEN no slot can be invited`() {
            // Arrange
            val input = input(account = pendingAccount(members = listOf(creator)), invites = emptyList())

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
                account = pendingAccount(members = listOf(creator, member)).copy(status = JointAccount.Status.CONFIRMING),
                invites = listOf(INVITE_A),
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
                account = pendingAccount(members = listOf(creator, member)).copy(status = JointAccount.Status.CONFIRMING),
                invites = listOf(INVITE_A),
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
            val input = input(
                account = pendingAccount(members = model.members).copy(status = model.status),
                invites = if (model.isCreator) listOf(INVITE_A, INVITE_B) else emptyList(),
                sharedInvites = model.sharedInvites,
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
                expected = FooterExpectation(hint = INVITE_MORE_TWO, isButtonEnabled = false),
            ),
            FooterModel(
                status = JointAccount.Status.PENDING,
                isCreator = true,
                sharedInvites = setOf(INVITE_A),
                expected = FooterExpectation(hint = INVITE_MORE_ONE, isButtonEnabled = false),
            ),
            FooterModel(
                status = JointAccount.Status.PENDING,
                isCreator = true,
                sharedInvites = setOf(INVITE_A, INVITE_B),
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
                members = listOf(creator, member, member.copy(address = "0xthird")),
                expected = FooterExpectation(hint = EVERYONE_JOINED, isButtonEnabled = false),
            ),
            FooterModel(
                status = JointAccount.Status.PENDING,
                isCreator = false,
                members = listOf(creator, member, member.copy(address = "0xthird")),
                expected = FooterExpectation(hint = EVERYONE_JOINED, isButtonEnabled = false),
            ),
            FooterModel(
                status = JointAccount.Status.CONFIRMING,
                isCreator = true,
                members = listOf(creator, member, member.copy(address = "0xthird")),
                expected = FooterExpectation(hint = null, isButtonEnabled = true),
            ),
            FooterModel(
                status = JointAccount.Status.CONFIRMING,
                isCreator = false,
                members = listOf(creator, member, member.copy(address = "0xthird")),
                expected = FooterExpectation(hint = null, isButtonEnabled = false),
            ),
            FooterModel(
                status = JointAccount.Status.CONFIRMING,
                isCreator = false,
                members = listOf(creator, member.copy(isVerified = true), member.copy(address = "0xthird")),
                expected = FooterExpectation(hint = STILL_NEED_TO_CONFIRM_TWO, isButtonEnabled = null),
            ),
            FooterModel(
                status = JointAccount.Status.CONFIRMING,
                isCreator = true,
                members = listOf(creator.copy(isVerified = true), member.copy(isVerified = true), member.copy(address = "0xthird")),
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
        val sharedInvites: Set<String> = emptySet(),
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
                invites = listOf(INVITE_A),
            )

            // Act
            val state = converter.convert(input)

            // Assert
            assertThat(state.header).isNull()
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
        fun `GIVEN own address of a joined member WHEN convert THEN own row is You and creator row is Creator`() {
            // Arrange
            val input = input(
                account = pendingAccount(members = listOf(creator, member)),
                invites = emptyList(),
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
        fun `GIVEN own address of the creator without invites WHEN convert THEN creator rights and own row is You and Creator`() {
            // Arrange
            val input = input(
                account = pendingAccount(members = listOf(creator, member)),
                invites = emptyList(),
                ownAddress = creator.address,
            )

            // Act
            val state = converter.convert(input)

            // Assert
            assertThat(state.joined.map { it.isYou to it.isCreator }).containsExactly(
                true to true,
                false to false,
            ).inOrder()
            assertThat(state.joined.map { it.status }.distinct()).containsExactly(JointAccountMembersUM.MemberStatusUM.Joined)
            assertThat(state.header?.title).isEqualTo(resourceReference(CoreUiR.string.joint_account_invite_members_title))
            assertThat(state.topBar).isInstanceOf(JointAccountMembersUM.TopBarUM.Dismissible::class.java)
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
            assertThat(state.freeSlots.map { it.action }.distinct()).containsExactly(null)
        }
    }

    @Test
    fun `GIVEN a shared invite WHEN converted THEN only its slot reads Invited`() {
        // Arrange
        val account = pendingAccount(members = listOf(creator))

        // Act
        val state = converter.convert(
            input(account = account, invites = listOf(INVITE_A, INVITE_B), sharedInvites = setOf(INVITE_B)),
        )

        // Assert
        assertThat(state.freeSlots.map { it.status }).containsExactly(
            JointAccountMembersUM.SlotStatusUM.NotInvited,
            JointAccountMembersUM.SlotStatusUM.Invited(resourceReference(CoreUiR.string.joint_account_invite_members_invited)),
        ).inOrder()
        assertThat(state.freeSlots.map { it.action?.javaClass }).containsExactly(
            JointAccountMembersUM.SlotActionUM.Invite::class.java,
            JointAccountMembersUM.SlotActionUM.ReInvite::class.java,
        ).inOrder()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Confirming {

        private val confirmingAccount = pendingAccount(
            members = listOf(creator, member.copy(isVerified = true), member.copy(address = "0xthird")),
        ).copy(status = JointAccount.Status.CONFIRMING)

        @Test
        fun `GIVEN confirming account WHEN convert THEN header is Confirm the account with the address subtitle`() {
            // Arrange
            val input = input(account = confirmingAccount, invites = emptyList())

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
            val input = input(account = confirmingAccount, invites = emptyList())

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
            val input = input(
                account = pendingAccount(members = listOf(creator, member.copy(isVerified = true))),
                invites = listOf(INVITE_A),
            )

            // Act
            val statuses = converter.convert(input).joined.map { it.status }.distinct()

            // Assert
            assertThat(statuses).containsExactly(JointAccountMembersUM.MemberStatusUM.Joined)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SlotExpiry {

        private val now = DateTime(2026, 10, 20, 12, 0)

        @Test
        fun `GIVEN unshared invite WHEN convert THEN slot reads Not invited and offers Invite`() {
            // Act
            val slot = convertSlot(invite = JointAccountInvite(id = INVITE_A, expiresAtMillis = now.plusDays(3).millis))

            // Assert
            assertThat(slot.status).isEqualTo(JointAccountMembersUM.SlotStatusUM.NotInvited)
            assertThat(slot.action).isInstanceOf(JointAccountMembersUM.SlotActionUM.Invite::class.java)
        }

        @Test
        fun `GIVEN expired invite WHEN convert THEN slot reads Expired and offers Re-invite`() {
            // Act
            val slot = convertSlot(
                invite = JointAccountInvite(id = INVITE_A, expiresAtMillis = now.minusMinutes(1).millis),
                shared = false,
            )

            // Assert
            assertThat(slot.status).isEqualTo(JointAccountMembersUM.SlotStatusUM.Expired)
            assertThat(slot.action).isInstanceOf(JointAccountMembersUM.SlotActionUM.ReInvite::class.java)
        }

        @Test
        fun `GIVEN invite expiring later today WHEN convert THEN label is Until today with the time`() {
            // Arrange
            val expiresAt = now.plusHours(1)

            // Act
            val slot = convertSlot(invite = JointAccountInvite(id = INVITE_A, expiresAtMillis = expiresAt.millis), shared = true)

            // Assert
            assertThat(slot.status).isEqualTo(
                JointAccountMembersUM.SlotStatusUM.Invited(
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
            val expiresAt = now.plusDays(1).withHourOfDay(9)

            // Act
            val slot = convertSlot(invite = JointAccountInvite(id = INVITE_A, expiresAtMillis = expiresAt.millis), shared = true)

            // Assert
            assertThat(slot.status).isEqualTo(
                JointAccountMembersUM.SlotStatusUM.Invited(
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
            val expiresAt = now.plusDays(3)

            // Act
            val slot = convertSlot(invite = JointAccountInvite(id = INVITE_A, expiresAtMillis = expiresAt.millis), shared = true)

            // Assert
            assertThat(slot.status).isEqualTo(
                JointAccountMembersUM.SlotStatusUM.Invited(
                    label = resourceReference(
                        id = CoreUiR.string.joint_account_invite_members_until,
                        formatArgs = wrappedList(FreeSlotUMFactory.dateTimeFormatter().print(expiresAt)),
                    ),
                ),
            )
        }

        private fun convertSlot(invite: JointAccountInvite, shared: Boolean = false): JointAccountMembersUM.MemberUM.FreeSlot {
            val input = JointAccountToMembersUMConverter.Input(
                account = pendingAccount(members = listOf(creator)).copy(membersCount = 2),
                invites = listOf(invite),
                sharedInvites = if (shared) setOf(invite.id) else emptySet(),
                ownAddress = creator.address,
                accountName = ACCOUNT_NAME,
                nowMillis = now.millis,
            )
            return converter.convert(input).freeSlots.single()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SlotNumbering {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN joined members WHEN convert THEN free slots are numbered after them`(model: NumberingModel) {
            // Arrange
            val account = pendingAccount(members = model.members).copy(membersCount = FIVE_SLOTS)

            // Act
            val titles = converter.convert(input(account = account, invites = emptyList())).freeSlots.map { it.title }

            // Assert
            assertThat(titles).containsExactlyElementsIn(
                model.expectedNumbers.map { number ->
                    resourceReference(CoreUiR.string.joint_account_invite_members_member_number, wrappedList(number))
                },
            ).inOrder()
        }

        private fun provideTestModels() = listOf(
            NumberingModel(members = listOf(creator), expectedNumbers = listOf(1, 2, 3, 4)),
            NumberingModel(
                members = listOf(creator, member, member.copy(address = "0xB0B")),
                expectedNumbers = listOf(3, 4),
            ),
        )
    }

    data class NumberingModel(val members: List<JointAccount.Member>, val expectedNumbers: List<Int>)

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SectionLabels {

        @Test
        fun `GIVEN joined members and free slots WHEN convert THEN both section labels shown`() {
            // Arrange
            val input = input(account = pendingAccount(members = listOf(creator)), invites = listOf(INVITE_A))

            // Act
            val state = converter.convert(input)

            // Assert
            assertThat(state.membersLabel).isEqualTo(resourceReference(CoreUiR.string.joint_account_members_title))
            assertThat(state.waitingLabel).isEqualTo(
                resourceReference(CoreUiR.string.joint_account_invite_members_waiting_to_join),
            )
        }

        @Test
        fun `GIVEN every slot filled WHEN convert THEN no waiting label`() {
            // Arrange
            val input = input(
                account = pendingAccount(members = listOf(creator, member, member.copy(address = "0xB0B"))),
                invites = listOf(INVITE_A),
            )

            // Act
            val state = converter.convert(input)

            // Assert
            assertThat(state.freeSlots).isEmpty()
            assertThat(state.waitingLabel).isNull()
            assertThat(state.membersLabel).isNotNull()
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
                invites = listOf(INVITE_A),
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
                invites = listOf(INVITE_A),
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
            val input = input(
                account = pendingAccount(members = listOf(creator)),
                invites = listOf(INVITE_A),
                accountName = null,
            )

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
        invites: List<String>,
        ownAddress: String? = null,
        sharedInvites: Set<String> = emptySet(),
        accountName: AccountName? = ACCOUNT_NAME,
        accountIcon: CryptoPortfolioIcon? = null,
    ) = JointAccountToMembersUMConverter.Input(
        account = account,
        invites = invites.map { inviteId -> JointAccountInvite(id = inviteId) },
        sharedInvites = sharedInvites,
        ownAddress = ownAddress,
        accountName = accountName,
        accountIcon = accountIcon,
    )

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
        const val FIVE_SLOTS = 5
        const val INVITE_A = "invite-a"
        const val INVITE_B = "invite-b"

        val INVITE_MORE_TWO = pluralReference(
            id = CoreUiR.plurals.joint_account_invite_members_invite_more,
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
        val ACCOUNT_NAME_REFERENCE = stringReference(ACCOUNT_NAME.value)
        val ACCOUNT_ICON = CryptoPortfolioIcon.ofCustomAccount(
            value = CryptoPortfolioIcon.Icon.Family,
            color = CryptoPortfolioIcon.Color.CandyGrapeFizz,
        )

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