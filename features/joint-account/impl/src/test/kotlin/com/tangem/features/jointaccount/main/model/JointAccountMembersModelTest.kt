package com.tangem.features.jointaccount.main.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.jointaccount.usecase.GetJointAccountInvitesUseCase
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.jointaccount.main.JointAccountMembersComponent
import com.tangem.features.jointaccount.main.JointAccountMembersUM
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * The members screen reads the account from the domain and takes its right to invite and activate from the
 * invites this device holds — only the creating device ever gets them.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JointAccountMembersModelTest {

    private val router: Router = mockk(relaxed = true)
    private val clipboardManager: ClipboardManager = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val shareManager: ShareManager = mockk(relaxed = true)
    private val supplier: SingleJointAccountListSupplier = mockk()
    private val getInvitesUseCase: GetJointAccountInvitesUseCase = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(router, clipboardManager, uiMessageSender, shareManager, supplier, getInvitesUseCase)
    }

    @Test
    fun `GIVEN creator with two free slots WHEN model created THEN own role and progress of the account`() = runTest {
        // Arrange
        stub(account = pendingAccount(members = listOf(creator)), invites = listOf(INVITE_A, INVITE_B))

        // Act
        val state = createModel().uiState.value

        // Assert
        assertThat(state.members).hasSize(THREE_SLOTS)
        val own = state.members.first() as JointAccountMembersUM.MemberUM.Joined
        assertThat(own.name).isEqualTo(stringReference(CREATOR_NAME))
        assertThat(own.avatar.monogram).isEqualTo("I")
        assertThat(state.otherMembersLabel).isNotNull()
    }

    @Test
    fun `GIVEN invites on this device WHEN model created THEN every free slot offers its own invite`() = runTest {
        // Arrange
        stub(account = pendingAccount(members = listOf(creator)), invites = listOf(INVITE_A, INVITE_B))

        // Act
        val slots = createModel().uiState.value.members
            .filterIsInstance<JointAccountMembersUM.MemberUM.FreeSlot>()

        // Assert
        assertThat(slots.map { it.canInvite }).containsExactly(true, true)

        // Act
        slots.forEach { it.onInviteClick() }

        // Assert
        verify(exactly = 1) { shareManager.shareText("$INVITE_LINK_PREFIX$INVITE_A") }
        verify(exactly = 1) { shareManager.shareText("$INVITE_LINK_PREFIX$INVITE_B") }
    }

    @Test
    fun `GIVEN no invites on this device WHEN model created THEN no slot can be invited`() = runTest {
        // Arrange
        stub(account = pendingAccount(members = listOf(creator)), invites = emptyList())

        // Act
        val slots = createModel().uiState.value.members
            .filterIsInstance<JointAccountMembersUM.MemberUM.FreeSlot>()

        // Assert
        assertThat(slots.map { it.canInvite }).containsExactly(false, false)

        // Act
        slots.forEach { it.onInviteClick() }

        // Assert
        verify(exactly = 0) { shareManager.shareText(any()) }
    }

    @Test
    fun `GIVEN pending account of the creator WHEN model created THEN activation is not offered yet`() = runTest {
        // Arrange
        stub(account = pendingAccount(members = listOf(creator)), invites = listOf(INVITE_A))

        // Act
        val state = createModel().uiState.value

        // Assert
        assertThat(state.activation).isNull()
    }

    @Test
    fun `GIVEN confirming account of the creator WHEN model created THEN activation is offered`() = runTest {
        // Arrange
        stub(
            account = pendingAccount(members = listOf(creator, member)).copy(
                status = JointAccount.Status.CONFIRMING,
            ),
            invites = listOf(INVITE_A),
        )

        // Act
        val state = createModel().uiState.value

        // Assert
        assertThat(state.activation).isNotNull()
    }

    @Test
    fun `GIVEN confirming account without invites WHEN model created THEN activation is not offered`() = runTest {
        // Arrange
        stub(
            account = pendingAccount(members = listOf(creator, member)).copy(
                status = JointAccount.Status.CONFIRMING,
            ),
            invites = emptyList(),
        )

        // Act
        val state = createModel().uiState.value

        // Assert
        assertThat(state.activation).isNull()
    }

    @Test
    fun `GIVEN reference mode WHEN model created THEN neither inviting nor activation nor archiving`() = runTest {
        // Arrange
        stub(
            account = pendingAccount(members = listOf(creator, member)).copy(
                status = JointAccount.Status.CONFIRMING,
            ),
            invites = listOf(INVITE_A),
        )

        // Act
        val state = createModel(mode = JointAccountMembersComponent.Mode.Reference).uiState.value

        // Assert
        assertThat(state.activation).isNull()
        assertThat(state.canArchive).isFalse()
        val slots = state.members.filterIsInstance<JointAccountMembersUM.MemberUM.FreeSlot>()
        assertThat(slots.map { it.canInvite }).doesNotContain(true)
    }

    @Test
    fun `GIVEN another wallet's account in the list WHEN model created THEN it is ignored`() = runTest {
        // Arrange
        val other = pendingAccount(members = listOf(member)).copy(cryptoAccountId = OTHER_ACCOUNT_ID)
        every { supplier.invoke(userWalletId = USER_WALLET_ID) } returns flowOf(listOf(other))
        coEvery { getInvitesUseCase(any(), any()) } returns emptyList()

        // Act
        val state = createModel().uiState.value

        // Assert
        assertThat(state.members).isEmpty()
    }

    private fun stub(account: JointAccount, invites: List<String>) {
        every { supplier.invoke(userWalletId = USER_WALLET_ID) } returns flowOf(listOf(account))
        coEvery { getInvitesUseCase(userWalletId = USER_WALLET_ID, cryptoAccountId = ACCOUNT_ID) } returns invites
    }

    private fun pendingAccount(members: List<JointAccount.Member>) = JointAccount(
        cryptoAccountId = ACCOUNT_ID,
        membersCount = THREE_SLOTS,
        threshold = 2,
        safeAddress = null,
        status = JointAccount.Status.PENDING,
        members = members,
        source = StatusSource.ACTUAL,
    )

    private fun TestScope.createModel(
        mode: JointAccountMembersComponent.Mode = JointAccountMembersComponent.Mode.Invite,
    ): JointAccountMembersModel {
        val model = JointAccountMembersModel(
            paramsContainer = MutableParamsContainer(
                JointAccountMembersComponent.Params(
                    userWalletId = USER_WALLET_ID,
                    cryptoAccountId = ACCOUNT_ID,
                    mode = mode,
                ),
            ),
            dispatchers = createTestingCoroutineDispatcherProvider(),
            router = router,
            clipboardManager = clipboardManager,
            uiMessageSender = uiMessageSender,
            shareManager = shareManager,
            jointAccountListSupplier = supplier,
            getJointAccountInvitesUseCase = getInvitesUseCase,
        )
        advanceUntilIdle()

        return model
    }

    private fun TestScope.createTestingCoroutineDispatcherProvider(): TestingCoroutineDispatcherProvider {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        return TestingCoroutineDispatcherProvider(
            main = testDispatcher,
            mainImmediate = testDispatcher,
            io = testDispatcher,
            default = testDispatcher,
            single = testDispatcher,
        )
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        const val ACCOUNT_ID = "a1"
        const val OTHER_ACCOUNT_ID = "b2"
        const val CREATOR_NAME = "Ivan"
        const val THREE_SLOTS = 3
        const val INVITE_A = "invite-a"
        const val INVITE_B = "invite-b"
        const val INVITE_LINK_PREFIX = "https://tangem.com/joint-account/join/"

        val creator = JointAccount.Member(
            name = CREATOR_NAME,
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