package com.tangem.features.jointaccount.main.model

import arrow.core.left
import arrow.core.right
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.dismiss
import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.SingleAccountListSupplier
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
import com.tangem.domain.jointaccount.usecase.GetSharedJointAccountInvitesUseCase
import com.tangem.domain.jointaccount.usecase.MarkJointAccountInviteSharedUseCase
import com.tangem.domain.jointaccount.verifier.JointAccountCompositionVerifier
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.jointaccount.compromised.JointAccountCompromiseWatcher
import com.tangem.features.jointaccount.compromised.JointAccountCompromisedAlert
import com.tangem.features.jointaccount.main.JointAccountMembersComponent
import com.tangem.features.jointaccount.main.JointAccountMembersUM
import com.tangem.features.jointaccount.main.entity.JointAccountMembersSheetConfig
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
import org.junit.jupiter.api.Disabled
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
    private val getSharedInvitesUseCase: GetSharedJointAccountInvitesUseCase = mockk()
    private val markInviteSharedUseCase: MarkJointAccountInviteSharedUseCase = mockk()
    private val sharedInvites = MutableStateFlow<Set<String>>(emptySet())
    /** Fresh per test: with one instance for the class, models of earlier tests would stay subscribed to it */
    private var sharedTexts = MutableSharedFlow<String>(extraBufferCapacity = 1)
    private val ownerAddressResolver: JointOwnerAddressResolver = mockk()
    private val singleAccountSupplier: SingleAccountSupplier = mockk()
    private val activateUseCase: ActivateJointAccountUseCase = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val compositionVerifier: JointAccountCompositionVerifier = mockk()
    private val singleAccountListSupplier: SingleAccountListSupplier = mockk()
    private val jointAccountCompromisedAlert: JointAccountCompromisedAlert = mockk(relaxed = true)

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
            getSharedInvitesUseCase,
            markInviteSharedUseCase,
            ownerAddressResolver,
            singleAccountSupplier,
            activateUseCase,
            userWalletsListRepository,
            compositionVerifier,
            jointAccountCompromisedAlert,
            singleAccountListSupplier,
        )
        val localAccount = mockk<Account.Joint> {
            every { accountId } returns AccountId.forJointAccount(USER_WALLET_ID, ACCOUNT_ID).getOrNull()!!
            every { ownerKeyIndex } returns MockAccounts.createJointAccount(userWalletId = USER_WALLET_ID).ownerKeyIndex
        }
        every { singleAccountListSupplier(USER_WALLET_ID) } returns
            flowOf(mockk<AccountList> { every { accounts } returns listOf(localAccount) })
        coEvery { compositionVerifier.isVerified(userWalletId = any(), ownerKeyIndex = any(), jointAccount = any()) } returns
            true
        val coldWallet = mockk<UserWallet.Cold> {
            every { walletId } returns USER_WALLET_ID
        }
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(coldWallet))
        every { pollingCoordinator.poll(walletId = any()) } returns emptyFlow()
        every { singleAccountSupplier.filterJointAccount(accountId = any()) } returns emptyFlow()
        coEvery { ownerAddressResolver.resolve(userWalletId = any(), ownerKeyIndex = any()) } returns null.right()
        sharedInvites.value = emptySet()
        sharedTexts = MutableSharedFlow(extraBufferCapacity = 1)
        every { shareManager.sharedTexts } returns sharedTexts
        every { getSharedInvitesUseCase(userWalletId = any(), cryptoAccountId = any()) } returns sharedInvites
        coEvery { markInviteSharedUseCase(userWalletId = any(), cryptoAccountId = any(), inviteId = any()) } answers {
            sharedInvites.value += thirdArg<String>()
        }
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
        assertThat(state.joined.map { it.isYou }).containsExactly(false, true).inOrder()
    }

    @Test
    fun `GIVEN accounts record WHEN model created THEN summary card shows its name and icon`() = runTest {
        // Arrange
        stub(account = pendingAccount(members = listOf(creator)), invites = listOf(INVITE_A))
        val jointAccount = MockAccounts.createJointAccount(name = "Family savings", userWalletId = USER_WALLET_ID)
        every { singleAccountSupplier.filterJointAccount(accountId = any()) } returns flowOf(jointAccount)

        // Act
        val card = createModel().uiState.value.summaryCard

        // Assert
        assertThat(card?.name).isEqualTo(stringReference("Family savings"))
        assertThat(card?.icon).isEqualTo(
            AccountIconUM.CryptoPortfolio(value = jointAccount.icon.value, color = jointAccount.icon.color),
        )
    }

    @Test
    fun `GIVEN summary card WHEN approvals info clicked THEN about signatures sheet opens with the rule`() = runTest {
        // Arrange
        stub(account = pendingAccount(members = listOf(creator)), invites = listOf(INVITE_A))
        every { singleAccountSupplier.filterJointAccount(accountId = any()) } returns
            flowOf(MockAccounts.createJointAccount(userWalletId = USER_WALLET_ID))
        val model = createModel()
        val openedSheets = model.bottomSheetNavigation.trackSlot()

        // Act
        model.uiState.value.summaryCard!!.onApprovalsInfoClick()

        // Assert
        assertThat(openedSheets.last()).isEqualTo(JointAccountMembersSheetConfig.AboutSignatures(threshold = 2, total = THREE_SLOTS))
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
        val before = model.uiState.value.joined.map { it.isYou }

        // Act
        accounts.emit(jointAccount)
        advanceUntilIdle()

        // Assert
        assertThat(before).containsExactly(false, false).inOrder()
        assertThat(model.uiState.value.joined.map { it.isYou }).containsExactly(false, true).inOrder()
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
    fun `GIVEN free slot with an invite WHEN slot invite clicked THEN the invite sheet opens without sharing`() =
        runTest {
            // Arrange
            stub(account = pendingAccount(members = listOf(creator)), invites = listOf(INVITE_A))
            stubAccountRecord()
            val model = createModel()
            val openedSheets = model.bottomSheetNavigation.trackSlot()
            val expected = JointAccountMembersSheetConfig.InviteMember(
                message = resourceReference(
                    id = CoreUiR.string.joint_account_invite_member_message,
                    formatArgs = wrappedList(stringReference(ACCOUNT_NAME), "$INVITE_LINK_PREFIX$INVITE_A"),
                ),
            )

            // Act
            model.uiState.value.freeSlots.first().action!!.onClick()

            // Assert
            assertThat(openedSheets.last()).isEqualTo(expected)
            verify(exactly = 0) { shareManager.shareTrackedText(text = any()) }
        }

    @Test
    fun `GIVEN account record not arrived yet WHEN model created THEN no slot can be invited`() = runTest {
        // Arrange
        stub(account = pendingAccount(members = listOf(creator)), invites = listOf(INVITE_A))

        // Act
        val slots = createModel().uiState.value.freeSlots

        // Assert
        assertThat(slots.map { it.action }).containsExactly(null, null)
    }

    @Test
    fun `GIVEN invite sheet WHEN share clicked THEN the message is shared and the sheet stays open`() = runTest {
        // Arrange
        stub(account = pendingAccount(members = listOf(creator)), invites = listOf(INVITE_A))
        stubAccountRecord()
        val model = createModel()
        val openedSheets = model.bottomSheetNavigation.trackSlot()
        model.uiState.value.freeSlots.first().action!!.onClick()

        // Act
        model.onShareInviteClick(message = INVITE_A_MESSAGE)

        // Assert
        verify(exactly = 1) { shareManager.shareTrackedText(text = INVITE_A_MESSAGE) }
        assertThat(openedSheets.last()).isNotNull()
    }

    @Test
    fun `GIVEN a not invited slot WHEN a target is picked THEN the slot reads Invited`() = runTest {
        // Arrange
        stub(account = pendingAccount(members = listOf(creator)), invites = listOf(INVITE_A))
        val model = createModel()

        // Act
        model.onShareInviteClick(message = INVITE_A_MESSAGE)
        sharedTexts.emit(INVITE_A_MESSAGE)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.freeSlots.first().status)
            .isEqualTo(resourceReference(CoreUiR.string.joint_account_invite_members_invited))
        coVerify(exactly = 1) {
            markInviteSharedUseCase(userWalletId = USER_WALLET_ID, cryptoAccountId = ACCOUNT_ID, inviteId = INVITE_A)
        }
    }

    @Test
    fun `GIVEN the account updates while a pick is recorded WHEN the write finishes THEN the slot reads Invited`() =
        runTest {
            // Arrange
            val account = pendingAccount(members = listOf(creator))
            val accounts = MutableStateFlow(listOf(account))
            every { supplier.invoke(userWalletId = USER_WALLET_ID) } returns accounts
            coEvery { getInvitesUseCase(userWalletId = USER_WALLET_ID, cryptoAccountId = ACCOUNT_ID) } returns
                listOf(INVITE_A)
            coEvery {
                markInviteSharedUseCase(userWalletId = any(), cryptoAccountId = any(), inviteId = any())
            } coAnswers {
                delay(timeMillis = 1_000)
                sharedInvites.value += thirdArg<String>()
            }
            val model = createModel()

            // Act
            sharedTexts.emit(INVITE_A_MESSAGE)
            runCurrent()
            accounts.value = listOf(account.copy(source = StatusSource.CACHE))
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.freeSlots.first().status)
                .isEqualTo(resourceReference(CoreUiR.string.joint_account_invite_members_invited))
        }

    @Test
    fun `GIVEN another text is shared WHEN it is reported THEN no invite is recorded`() = runTest {
        // Arrange
        stub(account = pendingAccount(members = listOf(creator)), invites = listOf(INVITE_A))
        createModel()

        // Act
        sharedTexts.emit("https://tangem.com/somewhere-else")
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { markInviteSharedUseCase(any(), any(), any()) }
    }

    @Test
    fun `GIVEN a link that only starts with the invite's WHEN it is reported THEN no invite is recorded`() = runTest {
        // Arrange
        stub(account = pendingAccount(members = listOf(creator)), invites = listOf(INVITE_A))
        createModel()

        // Act
        sharedTexts.emit("Join my joint account\n$INVITE_LINK_PREFIX${INVITE_A}-other")
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { markInviteSharedUseCase(any(), any(), any()) }
    }

    @Test
    fun `GIVEN the share sheet is dismissed WHEN no target is picked THEN the slot stays not invited`() = runTest {
        // Arrange
        stub(account = pendingAccount(members = listOf(creator)), invites = listOf(INVITE_A))
        val model = createModel()

        // Act
        model.onShareInviteClick(message = INVITE_A_MESSAGE)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.freeSlots.first().status)
            .isEqualTo(resourceReference(CoreUiR.string.joint_account_invite_members_not_invited))
        coVerify(exactly = 0) { markInviteSharedUseCase(any(), any(), any()) }
    }

    @Test
    fun `GIVEN an invited slot WHEN shared again THEN the same message is shared`() = runTest {
        // Arrange
        stub(account = pendingAccount(members = listOf(creator)), invites = listOf(INVITE_A))
        sharedInvites.value = setOf(INVITE_A)
        val model = createModel()

        // Act
        model.onShareInviteClick(message = INVITE_A_MESSAGE)

        // Assert
        verify(exactly = 1) { shareManager.shareTrackedText(text = INVITE_A_MESSAGE) }
        assertThat(model.uiState.value.freeSlots.first().status)
            .isEqualTo(resourceReference(CoreUiR.string.joint_account_invite_members_invited))
    }

    @Test
    fun `GIVEN an invite shared before WHEN model created THEN its slot reads Invited`() = runTest {
        // Arrange
        stub(account = pendingAccount(members = listOf(creator)), invites = listOf(INVITE_A))
        sharedInvites.value = setOf(INVITE_A)

        // Act
        val model = createModel()

        // Assert
        assertThat(model.uiState.value.freeSlots.map { it.status }).containsExactly(
            resourceReference(CoreUiR.string.joint_account_invite_members_invited),
            resourceReference(CoreUiR.string.joint_account_invite_members_not_invited),
        ).inOrder()
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
    fun `GIVEN confirming account WHEN activate tapped THEN use case gets the account without a sheet`() = runTest {
        // Arrange
        stubConfirmingAccount()
        val jointAccount = MockAccounts.createJointAccount(userWalletId = USER_WALLET_ID)
        every { singleAccountSupplier.filterJointAccount(accountId = any()) } returns flowOf(jointAccount)
        coEvery { activateUseCase(account = jointAccount) } returns Unit.right()

        // Act
        val model = createModel()
        val openedConfirmations = model.bottomSheetNavigation.trackSlot()
        model.uiState.value.footer!!.button.onClick()
        advanceUntilIdle()

        // Assert
        assertThat(openedConfirmations.filterNotNull()).isEmpty()
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
    fun `GIVEN cold wallet WHEN activation offered THEN the activate button carries the card icon`() = runTest {
        // Arrange
        stubConfirmingAccount()

        // Act
        val model = createModel()

        // Assert
        assertThat(model.uiState.value.footer!!.button.walletIconRes).isEqualTo(CoreUiR.drawable.ic_tangem_24)
    }

    @Test
    fun `GIVEN hot wallet WHEN activation offered THEN the activate button carries no wallet icon`() = runTest {
        // Arrange
        stubConfirmingAccount()
        val hotWallet = mockk<UserWallet.Hot> {
            every { walletId } returns USER_WALLET_ID
        }
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(hotWallet))

        // Act
        val model = createModel()

        // Assert
        assertThat(model.uiState.value.footer!!.button.walletIconRes).isNull()
    }

    @Test
    fun `GIVEN activation fails WHEN activate tapped THEN the activate button stops loading`() = runTest {
        // Arrange
        stubConfirmingAccount()
        val jointAccount = MockAccounts.createJointAccount(userWalletId = USER_WALLET_ID)
        every { singleAccountSupplier.filterJointAccount(accountId = any()) } returns flowOf(jointAccount)
        coEvery {
            activateUseCase(account = jointAccount)
        } returns JointAccountFlowError.Failed(cause = IllegalStateException("boom")).left()
        val model = createModel()

        // Act
        model.uiState.value.footer!!.button.onClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.footer!!.button.isLoading).isFalse()
    }

    @Test
    fun `GIVEN signing dismissed WHEN activate tapped THEN the activate button stops loading`() = runTest {
        // Arrange
        stubConfirmingAccount()
        val jointAccount = MockAccounts.createJointAccount(userWalletId = USER_WALLET_ID)
        every { singleAccountSupplier.filterJointAccount(accountId = any()) } returns flowOf(jointAccount)
        coEvery { activateUseCase(account = jointAccount) } returns JointAccountFlowError.UserCancelled.left()
        val model = createModel()

        // Act
        model.uiState.value.footer!!.button.onClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.footer!!.button.isLoading).isFalse()
    }

    @Disabled("The activation sheet is off, see IS_ACTIVATION_CONFIRMATION_SHOWN")
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
        val openedConfirmations = model.bottomSheetNavigation.trackSlot()
        model.uiState.value.footer!!.button.onClick()

        // Assert
        assertThat((openedConfirmations.last() as JointAccountMembersSheetConfig.ActivationConfirmation).walletInteractionIconRes).isNull()
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
        val openedConfirmations = model.bottomSheetNavigation.trackSlot()
        model.uiState.value.footer!!.button.onClick()
        model.onConfirmActivationClick(cryptoAccountId = ACCOUNT_ID)
        runCurrent()

        // Assert
        assertThat(model.isConfirmingActivation.value).isTrue()
        assertThat(model.uiState.value.footer!!.button.isLoading).isTrue()

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
            val openedConfirmations = model.bottomSheetNavigation.trackSlot()
            model.uiState.value.footer!!.button.onClick()
            model.onConfirmActivationClick(cryptoAccountId = ACCOUNT_ID)
            runCurrent()
            model.bottomSheetNavigation.dismiss()
            model.uiState.value.footer!!.button.onClick()
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
        model.uiState.value.footer!!.button.onClick()
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
        model.uiState.value.footer!!.button.onClick()
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
        model.uiState.value.footer!!.button.onClick()
        model.onConfirmActivationClick(cryptoAccountId = ACCOUNT_ID)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { activateUseCase(account = any()) }
        verify(exactly = 1) { uiMessageSender.send(any()) }
        assertThat(model.uiState.value.footer!!.button.isLoading).isFalse()
    }

    @Disabled("The activation sheet is off, see IS_ACTIVATION_CONFIRMATION_SHOWN")
    @Test
    fun `GIVEN confirmation open WHEN the resolved account changes identity THEN the sheet is dropped`() = runTest {
        // Arrange
        val accountsFlow = MutableStateFlow(
            listOf(pendingAccount(members = listOf(creator, member)).copy(status = JointAccount.Status.CONFIRMING)),
        )
        every { supplier.invoke(userWalletId = USER_WALLET_ID) } returns accountsFlow
        coEvery { getInvitesUseCase(any(), any()) } returns listOf(INVITE_A)

        val model = createModel(cryptoAccountId = null)
        val openedConfirmations = model.bottomSheetNavigation.trackSlot()
        model.uiState.value.footer!!.button.onClick()
        assertThat(openedConfirmations.last()).isNotNull()

        // Act — a different account now resolves first
        accountsFlow.value = listOf(pendingAccount(members = listOf(creator)).copy(cryptoAccountId = OTHER_ACCOUNT_ID))
        advanceUntilIdle()

        // Assert
        assertThat(openedConfirmations.last()).isNull()
    }

    /** Mirrors what a real `childSlot` would track, since bare [SlotNavigation] only relays events. */
    private fun stubAccountRecord() {
        every { singleAccountSupplier.filterJointAccount(accountId = any()) } returns
            flowOf(MockAccounts.createJointAccount(name = ACCOUNT_NAME, userWalletId = USER_WALLET_ID))
    }

    private fun <C : Any> SlotNavigation<C>.trackSlot(): List<C?> {
        val tracked = mutableListOf<C?>()
        subscribe { event -> tracked.add(event.transformer(tracked.lastOrNull())) }
        return tracked
    }

    @Test
    fun `GIVEN the composition does not verify WHEN the screen opens THEN it closes with the alert shown`() = runTest {
        // Arrange
        stub(account = pendingAccount(members = listOf(creator)), invites = listOf(INVITE_A))
        coEvery {
            compositionVerifier.isVerified(userWalletId = any(), ownerKeyIndex = any(), jointAccount = any())
        } returns false

        // Act
        val model = createModel()

        // Assert
        verify(exactly = 1) { router.pop() }
        verify(exactly = 1) { jointAccountCompromisedAlert.show(USER_WALLET_ID) }
        assertThat(model.uiState.value.joined).isEmpty()
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
            jointAccountListSupplier = supplier,
            jointAccountListFetcher = jointAccountListFetcher,
            pollingCoordinator = pollingCoordinator,
            getJointAccountInvitesUseCase = getInvitesUseCase,
            getSharedJointAccountInvitesUseCase = getSharedInvitesUseCase,
            ownerAddressResolver = ownerAddressResolver,
            singleAccountSupplier = singleAccountSupplier,
            activateJointAccountUseCase = activateUseCase,
            userWalletsListRepository = userWalletsListRepository,
            stateController = JointAccountMembersStateController(),
            inviteSharing = JointAccountInviteSharing(
                shareManager = shareManager,
                getJointAccountInvitesUseCase = getInvitesUseCase,
                markJointAccountInviteSharedUseCase = markInviteSharedUseCase,
            ),
            compromiseWatcher = JointAccountCompromiseWatcher(
                singleAccountListSupplier = singleAccountListSupplier,
                compositionVerifier = compositionVerifier,
                jointAccountCompromisedAlert = jointAccountCompromisedAlert,
            ),
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
        const val INVITE_A_MESSAGE = "Join my joint account “Family savings” in Tangem\n$INVITE_LINK_PREFIX$INVITE_A"
        const val ACCOUNT_NAME = "Family savings"
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