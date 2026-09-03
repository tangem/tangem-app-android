package com.tangem.features.jointaccount.main.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.account.supplier.SingleAccountSupplier
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointAccountFlowError
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.jointaccount.usecase.ActivateJointAccountUseCase
import com.tangem.domain.jointaccount.usecase.GetJointAccountInvitesUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.jointaccount.main.JointAccountMembersComponent
import com.tangem.features.jointaccount.main.JointAccountMembersUM
import com.tangem.features.jointaccount.main.state.JointAccountMembersStateController
import com.tangem.test.mock.MockAccounts
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import com.tangem.core.ui.R as CoreUiR

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
    private val singleAccountSupplier: SingleAccountSupplier = mockk()
    private val activateUseCase: ActivateJointAccountUseCase = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            router,
            clipboardManager,
            uiMessageSender,
            shareManager,
            supplier,
            getInvitesUseCase,
            singleAccountSupplier,
            activateUseCase,
            userWalletsListRepository,
        )
        val coldWallet = mockk<UserWallet.Cold> {
            every { walletId } returns USER_WALLET_ID
        }
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(coldWallet))
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
    fun `GIVEN member redeemed a later invite WHEN model created THEN free slot offers the unredeemed one`() =
        runTest {
            // Arrange
            stub(
                account = pendingAccount(members = listOf(creator, member.copy(inviteId = INVITE_B))),
                invites = listOf(INVITE_A, INVITE_B),
            )

            // Act
            val slot = createModel().uiState.value.members
                .filterIsInstance<JointAccountMembersUM.MemberUM.FreeSlot>()
                .single()
            slot.onInviteClick()

            // Assert
            verify(exactly = 1) { shareManager.shareText("$INVITE_LINK_PREFIX$INVITE_A") }
            verify(exactly = 0) { shareManager.shareText("$INVITE_LINK_PREFIX$INVITE_B") }
        }

    @Test
    fun `GIVEN redeemed invite differs in case WHEN model created THEN it is still excluded`() = runTest {
        // Arrange
        stub(
            account = pendingAccount(members = listOf(creator, member.copy(inviteId = INVITE_B.uppercase()))),
            invites = listOf(INVITE_A, INVITE_B),
        )

        // Act
        val slot = createModel().uiState.value.members
            .filterIsInstance<JointAccountMembersUM.MemberUM.FreeSlot>()
            .single()
        slot.onInviteClick()

        // Assert
        verify(exactly = 1) { shareManager.shareText("$INVITE_LINK_PREFIX$INVITE_A") }
        verify(exactly = 0) { shareManager.shareText("$INVITE_LINK_PREFIX$INVITE_B") }
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
    fun `GIVEN confirming account WHEN activation confirmed THEN use case gets the account`() = runTest {
        // Arrange
        stubConfirmingAccount()
        val jointAccount = MockAccounts.createJointAccount(userWalletId = USER_WALLET_ID)
        every { singleAccountSupplier.filterJointAccount(accountId = any()) } returns flowOf(jointAccount)
        coEvery { activateUseCase(account = jointAccount) } returns Unit.right()

        // Act
        val model = createModel()
        model.uiState.value.activation!!.onActivateClick()
        val confirmation = model.uiState.value.activation!!.confirmation!!
        confirmation.onConfirmClick()
        advanceUntilIdle()

        // Assert
        assertThat(confirmation.walletInteractionIconRes).isEqualTo(CoreUiR.drawable.ic_tangem_24)
        coVerify(exactly = 1) { activateUseCase(account = jointAccount) }
        verify(exactly = 0) { uiMessageSender.send(any()) }
        assertThat(model.uiState.value.activation?.confirmation).isNull()
    }

    @Test
    fun `GIVEN hot wallet WHEN activation offered THEN the confirm button carries no wallet icon`() = runTest {
        // Arrange
        stubConfirmingAccount()
        val hotWallet = mockk<UserWallet.Hot> {
            every { walletId } returns USER_WALLET_ID
        }
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(hotWallet))

        // Act
        val model = createModel()
        model.uiState.value.activation!!.onActivateClick()

        // Assert
        assertThat(model.uiState.value.activation!!.confirmation!!.walletInteractionIconRes).isNull()
    }

    @Test
    fun `GIVEN signing in flight WHEN activation confirmed THEN the confirm button shows a loader`() = runTest {
        // Arrange
        stubConfirmingAccount()
        val jointAccount = MockAccounts.createJointAccount(userWalletId = USER_WALLET_ID)
        every { singleAccountSupplier.filterJointAccount(accountId = any()) } returns flowOf(jointAccount)
        coEvery { activateUseCase(account = jointAccount) } coAnswers {
            delay(timeMillis = 500)
            Unit.right()
        }

        // Act
        val model = createModel()
        model.uiState.value.activation!!.onActivateClick()
        model.uiState.value.activation!!.confirmation!!.onConfirmClick()
        runCurrent()

        // Assert
        assertThat(model.uiState.value.activation!!.confirmation!!.isConfirming).isTrue()

        // Act
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.activation?.confirmation).isNull()
    }

    @Test
    fun `GIVEN signing in flight WHEN sheet reopened and confirmed again THEN loader shows without second signing`() =
        runTest {
            // Arrange
            stubConfirmingAccount()
            val jointAccount = MockAccounts.createJointAccount(userWalletId = USER_WALLET_ID)
            every { singleAccountSupplier.filterJointAccount(accountId = any()) } returns flowOf(jointAccount)
            coEvery { activateUseCase(account = jointAccount) } coAnswers {
                delay(timeMillis = 500)
                Unit.right()
            }

            // Act — confirm, dismiss the sheet mid-flight, reopen it and confirm again
            val model = createModel()
            model.uiState.value.activation!!.onActivateClick()
            model.uiState.value.activation!!.confirmation!!.onConfirmClick()
            runCurrent()
            model.uiState.value.activation!!.confirmation!!.onCancelClick()
            model.uiState.value.activation!!.onActivateClick()
            model.uiState.value.activation!!.confirmation!!.onConfirmClick()
            runCurrent()

            // Assert
            assertThat(model.uiState.value.activation!!.confirmation!!.isConfirming).isTrue()

            // Act
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { activateUseCase(account = jointAccount) }
            assertThat(model.uiState.value.activation?.confirmation).isNull()
        }

    @Test
    fun `GIVEN signing dismissed WHEN activation confirmed THEN no dialog is shown`() = runTest {
        // Arrange
        stubConfirmingAccount()
        val jointAccount = MockAccounts.createJointAccount(userWalletId = USER_WALLET_ID)
        every { singleAccountSupplier.filterJointAccount(accountId = any()) } returns flowOf(jointAccount)
        coEvery { activateUseCase(account = jointAccount) } returns JointAccountFlowError.UserCancelled.left()

        // Act
        val model = createModel()
        model.uiState.value.activation!!.onActivateClick()
        model.uiState.value.activation!!.confirmation!!.onConfirmClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { uiMessageSender.send(any()) }
    }

    @Test
    fun `GIVEN activation fails WHEN activation confirmed THEN error dialog is shown`() = runTest {
        // Arrange
        stubConfirmingAccount()
        val jointAccount = MockAccounts.createJointAccount(userWalletId = USER_WALLET_ID)
        every { singleAccountSupplier.filterJointAccount(accountId = any()) } returns flowOf(jointAccount)
        coEvery {
            activateUseCase(account = jointAccount)
        } returns JointAccountFlowError.Failed(cause = IllegalStateException("boom")).left()

        // Act
        val model = createModel()
        model.uiState.value.activation!!.onActivateClick()
        model.uiState.value.activation!!.confirmation!!.onConfirmClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { uiMessageSender.send(any()) }
    }

    @Test
    fun `GIVEN account not in the store WHEN activation confirmed THEN error dialog without signing`() = runTest {
        // Arrange
        stubConfirmingAccount()
        every { singleAccountSupplier.filterJointAccount(accountId = any()) } returns emptyFlow()

        // Act
        val model = createModel()
        model.uiState.value.activation!!.onActivateClick()
        model.uiState.value.activation!!.confirmation!!.onConfirmClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { activateUseCase(account = any()) }
        verify(exactly = 1) { uiMessageSender.send(any()) }
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

    private fun stubConfirmingAccount() {
        stub(
            account = pendingAccount(members = listOf(creator, member)).copy(
                status = JointAccount.Status.CONFIRMING,
            ),
            invites = listOf(INVITE_A),
        )
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
            singleAccountSupplier = singleAccountSupplier,
            activateJointAccountUseCase = activateUseCase,
            userWalletsListRepository = userWalletsListRepository,
            stateController = JointAccountMembersStateController(),
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
        val ACCOUNT_ID = "a1".repeat(n = 32)
        val OTHER_ACCOUNT_ID = "b2".repeat(n = 32)
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