package com.tangem.features.jointaccount.main.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessage
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.core.ui.message.ToastMessage
import com.tangem.domain.account.status.usecase.ArchiveCryptoPortfolioUseCase
import com.tangem.domain.account.supplier.SingleAccountSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.jointaccount.address.JointOwnerAddressResolver
import com.tangem.domain.jointaccount.fetcher.SingleJointAccountListFetcher
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointAccountFlowError
import com.tangem.domain.jointaccount.polling.JointAccountPollingCoordinator
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.jointaccount.usecase.ActivateJointAccountUseCase
import com.tangem.domain.jointaccount.usecase.GetJointAccountInvitesUseCase
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.Account
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.jointaccount.main.JointAccountMembersComponent
import com.tangem.features.jointaccount.main.JointAccountMembersUM
import com.tangem.features.jointaccount.main.entity.ActivationConfirmationConfig
import com.tangem.features.jointaccount.main.state.JointAccountMembersStateController
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import com.tangem.test.mock.MockAccounts
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import com.tangem.core.ui.R as CoreUiR

/**
 * Mapping the account onto the screen (title/progress/members/activation offered or not) is covered by
 * [com.tangem.features.jointaccount.main.converter.JointAccountToMembersUMConverterTest]. This suite only
 * covers what the Model itself does: switching between the account-present and account-cleared state,
 * wiring callbacks into their side effects, activation signing, and archiving.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JointAccountMembersModelTest {

    private val router: Router = mockk(relaxed = true)
    private val clipboardManager: ClipboardManager = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val shareManager: ShareManager = mockk(relaxed = true)
    private val supplier: SingleJointAccountListSupplier = mockk()
    private val jointAccountListFetcher: SingleJointAccountListFetcher = mockk(relaxed = true)
    private val pollingCoordinator: JointAccountPollingCoordinator = mockk()
    private val getInvitesUseCase: GetJointAccountInvitesUseCase = mockk()
    private val ownerAddressResolver: JointOwnerAddressResolver = mockk()
    private val singleAccountSupplier: SingleAccountSupplier = mockk()
    private val activateUseCase: ActivateJointAccountUseCase = mockk()
    private val archiveCryptoPortfolioUseCase: ArchiveCryptoPortfolioUseCase = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            router,
            clipboardManager,
            uiMessageSender,
            shareManager,
            supplier,
            jointAccountListFetcher,
            pollingCoordinator,
            getInvitesUseCase,
            ownerAddressResolver,
            singleAccountSupplier,
            activateUseCase,
            archiveCryptoPortfolioUseCase,
            userWalletsListRepository,
        )
        val coldWallet = mockk<UserWallet.Cold> {
            every { walletId } returns USER_WALLET_ID
        }
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(coldWallet))
        every { pollingCoordinator.poll(walletId = any()) } returns emptyFlow()
        every { singleAccountSupplier.filterJointAccount(accountId = any()) } returns emptyFlow()
        coEvery { ownerAddressResolver.resolve(userWalletId = any(), ownerKeyIndex = any()) } returns null.right()
    }

    @Test
    fun `GIVEN account for this wallet WHEN model created THEN state is built from it`() = runTest {
        // Arrange
        stub(account = pendingAccount(members = listOf(creator)), invites = listOf(INVITE_A))

        // Act
        val state = createModel().uiState.value

        // Assert
        assertThat(state.header).isNotNull()
        assertThat(state.joined).isNotEmpty()
    }

    /**
     * The supplier only reads the store, so the composition on screen is as old as the last fetch. The screen
     * asks for one on open and renders what arrives — a member who joined meanwhile shows up without a restart.
     */
    @Test
    fun `GIVEN the store fills after the refresh WHEN model created THEN it ran for this wallet and the account shows`() =
        runTest {
            // Arrange
            val stored = MutableStateFlow(emptyList<JointAccount>())
            every { supplier.invoke(userWalletId = USER_WALLET_ID) } returns stored
            coEvery { getInvitesUseCase(any(), any()) } returns emptyList()
            coEvery { jointAccountListFetcher.invoke(params = any()) } coAnswers {
                stored.value = listOf(pendingAccount(members = listOf(creator)))
                Unit.right()
            }

            // Act
            val model = createModel()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) {
                jointAccountListFetcher.invoke(params = SingleJointAccountListFetcher.Params(USER_WALLET_ID))
            }
            assertThat(model.uiState.value.header).isNotNull()
            assertThat(model.uiState.value.joined).isNotEmpty()
        }

    @Test
    fun `GIVEN own owner address of a joined member WHEN model created THEN own row is labelled You`() = runTest {
        // Arrange
        stub(account = pendingAccount(members = listOf(creator, member)), invites = emptyList())
        val jointAccount = MockAccounts.createJointAccount(userWalletId = USER_WALLET_ID)
        every { singleAccountSupplier.filterJointAccount(accountId = any()) } returns flowOf(jointAccount)
        coEvery {
            ownerAddressResolver.resolve(userWalletId = USER_WALLET_ID, ownerKeyIndex = jointAccount.ownerKeyIndex)
        } returns member.address.right()

        // Act
        val state = createModel().uiState.value

        // Assert
        assertThat(state.joined.map { it.role }).containsExactly(
            resourceReference(CoreUiR.string.joint_account_invite_members_creator),
            resourceReference(CoreUiR.string.joint_account_members_role_you),
        ).inOrder()
    }

    @Test
    fun `GIVEN accounts record arrives late WHEN model created THEN own row turns into You once it does`() = runTest {
        // Arrange
        stub(account = pendingAccount(members = listOf(creator, member)), invites = emptyList())
        val jointAccount = MockAccounts.createJointAccount(userWalletId = USER_WALLET_ID)
        val accounts = MutableSharedFlow<Account.Joint>()
        every { singleAccountSupplier.filterJointAccount(accountId = any()) } returns accounts
        coEvery {
            ownerAddressResolver.resolve(userWalletId = USER_WALLET_ID, ownerKeyIndex = jointAccount.ownerKeyIndex)
        } returns member.address.right()
        val model = createModel()
        val before = model.uiState.value.joined.map { it.role }

        // Act
        accounts.emit(jointAccount)
        advanceUntilIdle()

        // Assert
        assertThat(before).containsExactly(
            resourceReference(CoreUiR.string.joint_account_invite_members_creator),
            resourceReference(CoreUiR.string.joint_account_members_role_joined),
        ).inOrder()
        assertThat(model.uiState.value.joined.map { it.role }).containsExactly(
            resourceReference(CoreUiR.string.joint_account_invite_members_creator),
            resourceReference(CoreUiR.string.joint_account_members_role_you),
        ).inOrder()
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
        assertThat(state.header).isNull()
        assertThat(state.joined).isEmpty()
    }

    @Test
    fun `GIVEN free slot with an invite WHEN slot invite clicked THEN the link is shared`() = runTest {
        // Arrange
        stub(account = pendingAccount(members = listOf(creator)), invites = listOf(INVITE_A))

        // Act
        val slot = createModel().uiState.value.freeSlots.first()
        slot.onInviteClick()

        // Assert
        verify(exactly = 1) { shareManager.shareText("$INVITE_LINK_PREFIX$INVITE_A") }
    }

    @Test
    fun `GIVEN account WHEN model created but not resumed THEN polling does not start`() = runTest {
        // Arrange
        stub(account = pendingAccount(members = listOf(creator)), invites = listOf(INVITE_A))

        // Act
        createModel()

        // Assert
        verify(exactly = 0) { pollingCoordinator.poll(walletId = any()) }
    }

    @Test
    fun `GIVEN account WHEN resumed THEN polling starts for the wallet`() = runTest {
        // Arrange
        stub(account = pendingAccount(members = listOf(creator)), invites = listOf(INVITE_A))
        val model = createModel()

        // Act
        model.onResume()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { pollingCoordinator.poll(walletId = USER_WALLET_ID) }
    }

    /**
     * The whole point of polling is to discover state the client doesn't know about yet — an active account with
     * no in-flight operations locally cached can still gain one on the backend (another member creates an
     * operation) at any moment, so polling must not be gated on the currently-known status.
     */
    @Test
    fun `GIVEN active account with no known pending operations WHEN resumed THEN polling still starts`() = runTest {
        // Arrange
        val active = pendingAccount(members = listOf(creator, member)).copy(
            status = JointAccount.Status.ACTIVE,
            safeAddress = SAFE_ADDRESS,
        )
        stub(account = active, invites = emptyList())
        val model = createModel()

        // Act
        model.onResume()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { pollingCoordinator.poll(walletId = USER_WALLET_ID) }
    }

    /**
     * An empty store isn't proof there's nothing to find — offline, a cold start, or a failed [refreshAccounts]
     * all look the same locally. Gating polling on a resolved account would make the screen unable to recover
     * from any of them on its own, so [subscribePolling] doesn't wait for one.
     */
    @Test
    fun `GIVEN no account resolved yet WHEN resumed THEN polling still starts`() = runTest {
        // Arrange
        every { supplier.invoke(userWalletId = USER_WALLET_ID) } returns flowOf(emptyList())
        coEvery { getInvitesUseCase(any(), any()) } returns emptyList()
        val model = createModel()

        // Act
        model.onResume()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { pollingCoordinator.poll(walletId = USER_WALLET_ID) }
    }

    @Test
    fun `GIVEN resumed with an account WHEN paused THEN polling stops`() = runTest {
        // Arrange
        stub(account = pendingAccount(members = listOf(creator)), invites = listOf(INVITE_A))
        val pollCancelled = MutableStateFlow(false)
        every { pollingCoordinator.poll(walletId = USER_WALLET_ID) } returns flow {
            try {
                awaitCancellation()
            } finally {
                pollCancelled.value = true
            }
        }
        val model = createModel()
        model.onResume()
        advanceUntilIdle()

        // Act
        model.onPause()
        advanceUntilIdle()

        // Assert
        assertThat(pollCancelled.value).isTrue()
    }

    private fun stubConfirmingAccount() {
        stub(
            account = pendingAccount(members = listOf(creator, member)).copy(
                status = JointAccount.Status.CONFIRMING,
            ),
            invites = listOf(INVITE_A),
        )
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
        val openedConfirmations = model.activationConfirmationNavigation.trackSlot()
        model.uiState.value.onActivateClick!!()
        model.onConfirmActivationClick(cryptoAccountId = ACCOUNT_ID)
        advanceUntilIdle()

        // Assert
        assertThat(openedConfirmations.first()!!.walletInteractionIconRes).isEqualTo(CoreUiR.drawable.ic_tangem_24)
        coVerify(exactly = 1) { activateUseCase(account = jointAccount) }
        val message = slot<SnackbarMessage>()
        verify(exactly = 1) { uiMessageSender.send(capture(message)) }
        assertThat(message.captured.message)
            .isEqualTo(resourceReference(CoreUiR.string.joint_account_activated_snackbar))
        assertThat(message.captured.startIconId).isEqualTo(CoreUiR.drawable.ic_success_20)
        verify(exactly = 1) { router.pop() }
        assertThat(openedConfirmations.last()).isNull()
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
        val openedConfirmations = model.activationConfirmationNavigation.trackSlot()
        model.uiState.value.onActivateClick!!()

        // Assert
        assertThat(openedConfirmations.last()!!.walletInteractionIconRes).isNull()
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
        val openedConfirmations = model.activationConfirmationNavigation.trackSlot()
        model.uiState.value.onActivateClick!!()
        model.onConfirmActivationClick(cryptoAccountId = ACCOUNT_ID)
        runCurrent()

        // Assert
        assertThat(model.isConfirmingActivation.value).isTrue()

        // Act
        advanceUntilIdle()

        // Assert
        assertThat(openedConfirmations.last()).isNull()
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
            val openedConfirmations = model.activationConfirmationNavigation.trackSlot()
            model.uiState.value.onActivateClick!!()
            model.onConfirmActivationClick(cryptoAccountId = ACCOUNT_ID)
            runCurrent()
            model.activationConfirmationNavigation.dismiss()
            model.uiState.value.onActivateClick!!()
            model.onConfirmActivationClick(cryptoAccountId = ACCOUNT_ID)
            runCurrent()

            // Assert
            assertThat(model.isConfirmingActivation.value).isTrue()

            // Act
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { activateUseCase(account = jointAccount) }
            assertThat(openedConfirmations.last()).isNull()
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
        model.uiState.value.onActivateClick!!()
        model.onConfirmActivationClick(cryptoAccountId = ACCOUNT_ID)
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
        model.uiState.value.onActivateClick!!()
        model.onConfirmActivationClick(cryptoAccountId = ACCOUNT_ID)
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
        model.uiState.value.onActivateClick!!()
        model.onConfirmActivationClick(cryptoAccountId = ACCOUNT_ID)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { activateUseCase(account = any()) }
        verify(exactly = 1) { uiMessageSender.send(any()) }
    }

    @Test
    fun `GIVEN confirmation open WHEN the resolved account changes identity THEN the sheet is dropped`() = runTest {
        // Arrange
        val accountsFlow = MutableStateFlow(
            listOf(pendingAccount(members = listOf(creator, member)).copy(status = JointAccount.Status.CONFIRMING)),
        )
        every { supplier.invoke(userWalletId = USER_WALLET_ID) } returns accountsFlow
        coEvery { getInvitesUseCase(any(), any()) } returns listOf(INVITE_A)

        val model = createModel(cryptoAccountId = null)
        val openedConfirmations = model.activationConfirmationNavigation.trackSlot()
        model.uiState.value.onActivateClick!!()
        assertThat(openedConfirmations.last()).isNotNull()

        // Act — a different account now resolves first
        accountsFlow.value = listOf(pendingAccount(members = listOf(creator)).copy(cryptoAccountId = OTHER_ACCOUNT_ID))
        advanceUntilIdle()

        // Assert
        assertThat(openedConfirmations.last()).isNull()
    }

    /** Mirrors what a real `childSlot` would track, since bare [SlotNavigation] only relays events. */
    private fun SlotNavigation<ActivationConfirmationConfig>.trackSlot(): List<ActivationConfirmationConfig?> {
        val tracked = mutableListOf<ActivationConfirmationConfig?>()
        subscribe { event -> tracked.add(event.transformer(tracked.lastOrNull())) }
        return tracked
    }

    @Test
    fun `GIVEN creator confirms archive WHEN it succeeds THEN account is archived and screen closes`() = runTest {
        // Arrange
        val expectedAccountId = stubForArchive()
        coEvery { archiveCryptoPortfolioUseCase(expectedAccountId) } returns Unit.right()
        val model = createModel()

        // Act
        model.dismissibleTopBar().onArchiveClick()
        confirmArchiveDialog().firstAction.onClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { archiveCryptoPortfolioUseCase(expectedAccountId) }
        val sentMessages = mutableListOf<UiMessage>()
        verify { uiMessageSender.send(capture(sentMessages)) }
        assertThat(sentMessages.filterIsInstance<ToastMessage>()).isNotEmpty()
        verify(exactly = 1) { router.pop() }
    }

    @Test
    fun `GIVEN archive fails with active referral status WHEN archived THEN referral dialog is shown`() = runTest {
        // Arrange
        stubForArchive()
        coEvery { archiveCryptoPortfolioUseCase(any()) } returns
            ArchiveCryptoPortfolioUseCase.Error.ActiveReferralStatus.left()
        val model = createModel()

        // Act
        model.dismissibleTopBar().onArchiveClick()
        confirmArchiveDialog().firstAction.onClick()
        advanceUntilIdle()

        // Assert
        val sentMessages = mutableListOf<UiMessage>()
        verify { uiMessageSender.send(capture(sentMessages)) }
        val errorDialog = sentMessages.filterIsInstance<DialogMessage>().last()
        assertThat(errorDialog.title).isEqualTo(
            resourceReference(CoreUiR.string.account_could_not_archive_referral_program_title),
        )
    }

    @Test
    fun `GIVEN archive fails otherwise WHEN archived THEN generic error dialog is shown`() = runTest {
        // Arrange
        stubForArchive()
        coEvery { archiveCryptoPortfolioUseCase(any()) } returns
            ArchiveCryptoPortfolioUseCase.Error.DataOperationFailed(cause = RuntimeException("boom")).left()
        val model = createModel()

        // Act
        model.dismissibleTopBar().onArchiveClick()
        confirmArchiveDialog().firstAction.onClick()
        advanceUntilIdle()

        // Assert
        val sentMessages = mutableListOf<UiMessage>()
        verify { uiMessageSender.send(capture(sentMessages)) }
        val errorDialog = sentMessages.filterIsInstance<DialogMessage>().last()
        assertThat(errorDialog.title).isEqualTo(resourceReference(CoreUiR.string.common_something_went_wrong))
    }

    private fun JointAccountMembersModel.dismissibleTopBar() =
        uiState.value.topBar as JointAccountMembersUM.TopBarUM.Dismissible

    /** Captures the confirmation [DialogMessage] sent so far by [JointAccountMembersModel]'s archive click. */
    private fun confirmArchiveDialog(): DialogMessage {
        val slot = slot<UiMessage>()
        verify { uiMessageSender.send(capture(slot)) }
        return slot.captured as DialogMessage
    }

    /** Stubs a creator-owned pending account; returns its resolved [AccountId] for the archive use case. */
    private fun stubForArchive(): AccountId {
        stub(account = pendingAccount(members = listOf(creator)), invites = listOf(INVITE_A))

        return AccountId.forJointAccount(userWalletId = USER_WALLET_ID, value = ACCOUNT_ID)
            .getOrNull()!!
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

    private fun TestScope.createModel(cryptoAccountId: String? = ACCOUNT_ID): JointAccountMembersModel {
        val model = JointAccountMembersModel(
            paramsContainer = MutableParamsContainer(
                JointAccountMembersComponent.Params(
                    userWalletId = USER_WALLET_ID,
                    cryptoAccountId = cryptoAccountId,
                ),
            ),
            dispatchers = createTestingCoroutineDispatcherProvider(),
            router = router,
            clipboardManager = clipboardManager,
            uiMessageSender = uiMessageSender,
            shareManager = shareManager,
            jointAccountListSupplier = supplier,
            jointAccountListFetcher = jointAccountListFetcher,
            pollingCoordinator = pollingCoordinator,
            getJointAccountInvitesUseCase = getInvitesUseCase,
            ownerAddressResolver = ownerAddressResolver,
            singleAccountSupplier = singleAccountSupplier,
            activateJointAccountUseCase = activateUseCase,
            archiveCryptoPortfolioUseCase = archiveCryptoPortfolioUseCase,
            userWalletsListRepository = userWalletsListRepository,
            stateController = JointAccountMembersStateController(),
        )
        advanceUntilIdle()

        return model
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        val ACCOUNT_ID = "a1".repeat(n = 32)
        val OTHER_ACCOUNT_ID = "b2".repeat(n = 32)
        const val THREE_SLOTS = 3
        const val INVITE_A = "invite-a"
        const val INVITE_LINK_PREFIX = "https://tangem.com/joint-account/join/"
        const val SAFE_ADDRESS = "0x5afe000000000000000000000000000000000001"

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