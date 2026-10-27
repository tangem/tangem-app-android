package com.tangem.features.jointaccount.main.model

import arrow.core.right
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.stringReference
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.account.supplier.SingleAccountSupplier
import com.tangem.domain.jointaccount.address.JointOwnerAddressResolver
import com.tangem.domain.jointaccount.fetcher.SingleJointAccountListFetcher
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointAccountInvite
import com.tangem.domain.jointaccount.polling.JointAccountPollingCoordinator
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.jointaccount.verifier.JointAccountCompositionVerifier
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.jointaccount.compromised.JointAccountCompromiseWatcher
import com.tangem.features.jointaccount.compromised.JointAccountCompromisedAlert
import com.tangem.features.jointaccount.main.JointAccountMembersUM
import com.tangem.features.jointaccount.main.entity.JointAccountMembersSheetConfig
import com.tangem.features.jointaccount.main.navigation.JointAccountMembersRoute
import com.tangem.features.jointaccount.main.state.JointAccountMembersStateController
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import com.tangem.utils.time.CurrentTimeProvider
import com.tangem.test.mock.MockAccounts
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

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
    private val closeFlow: () -> Unit = mockk(relaxed = true)
    private val clipboardManager: ClipboardManager = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val shareManager: ShareManager = mockk(relaxed = true)
    private val supplier: SingleJointAccountListSupplier = mockk()
    private val jointAccountListFetcher: SingleJointAccountListFetcher = mockk(relaxed = true)
    private val pollingCoordinator: JointAccountPollingCoordinator = mockk()
    private val ownerAddressResolver: JointOwnerAddressResolver = mockk()
    private val clock = FakeCurrentTimeProvider()
    private val singleAccountSupplier: SingleAccountSupplier = mockk()
    private val compositionVerifier: JointAccountCompositionVerifier = mockk()
    private val singleAccountListSupplier: SingleAccountListSupplier = mockk()
    private val jointAccountCompromisedAlert: JointAccountCompromisedAlert = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clock.nowMillis = NOW
        clearMocks(
            router,
            closeFlow,
            clipboardManager,
            uiMessageSender,
            shareManager,
            supplier,
            jointAccountListFetcher,
            pollingCoordinator,
            ownerAddressResolver,
            singleAccountSupplier,
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
        coEvery { compositionVerifier.isVerified(account = any(), jointAccount = any()) } returns
            true
        every { pollingCoordinator.poll(walletId = any()) } returns emptyFlow()
        every { singleAccountSupplier.filterJointAccount(accountId = any()) } returns emptyFlow()
        coEvery { ownerAddressResolver.resolve(userWalletId = any(), ownerKeyIndex = any()) } returns null.right()
    }

    @Test
    fun `GIVEN account for this wallet WHEN model created THEN state is built from it`() = runTest {
        // Arrange
        stub(account = pendingAccount(members = listOf(creator)))

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
        stub(account = pendingAccount(members = listOf(creator, member)))
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
        stub(account = pendingAccount(members = listOf(creator)))
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
        stub(account = pendingAccount(members = listOf(creator)))
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
        stub(account = pendingAccount(members = listOf(creator, member)))
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

        // Act
        val state = createModel().uiState.value

        // Assert
        assertThat(state.header).isNull()
        assertThat(state.joined).isEmpty()
    }

    @Test
    fun `GIVEN account record not arrived yet WHEN model created THEN no slot can be invited`() = runTest {
        // Arrange
        stub(account = pendingAccount(members = listOf(creator)))

        // Act
        val slots = createModel().uiState.value.freeSlots

        // Assert
        assertThat(slots.map { it.action }).containsExactly(null, null)
    }

    @Test
    fun `GIVEN invite sheet WHEN share clicked THEN the message is shared`() = runTest {
        // Arrange
        stub(account = pendingAccount(members = listOf(creator)))
        val model = createModel()

        // Act
        model.onShareInviteClick(message = INVITE_MESSAGE)

        // Assert
        verify(exactly = 1) { shareManager.shareTrackedText(text = INVITE_MESSAGE) }
    }

    @Test
    fun `GIVEN account WHEN model created but not resumed THEN polling does not start`() = runTest {
        // Arrange
        stub(account = pendingAccount(members = listOf(creator)))

        // Act
        createModel()

        // Assert
        verify(exactly = 0) { pollingCoordinator.poll(walletId = any()) }
    }

    @Test
    fun `GIVEN account WHEN resumed THEN polling starts for the wallet`() = runTest {
        // Arrange
        stub(account = pendingAccount(members = listOf(creator)))
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
        stub(account = active)
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
        stub(account = pendingAccount(members = listOf(creator)))
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

    @Test
    fun `GIVEN confirming account WHEN confirm tapped THEN the confirm address route is pushed`() = runTest {
        // Arrange
        stub(account = pendingAccount(members = listOf(creator, member)).copy(status = JointAccount.Status.CONFIRMING))
        val model = createModel()

        // Act
        model.uiState.value.footer!!.button!!.onClick()

        // Assert
        verify(exactly = 1) { router.push(JointAccountMembersRoute.ConfirmAddress(cryptoAccountId = ACCOUNT_ID)) }
    }

    @Test
    fun `GIVEN two live invites WHEN their deadlines pass while the screen is on top THEN each slot reads Expired in turn`() =
        runTest {
            // Arrange
            val invites = listOf(
                JointAccountInvite(slot = 1, expiresAtMillis = NOW + FIRST_DEADLINE_IN_MILLIS),
                JointAccountInvite(slot = 2, expiresAtMillis = NOW + SECOND_DEADLINE_IN_MILLIS),
            )
            stub(account = pendingAccount(members = listOf(creator)).copy(invites = invites))
            val model = createModel()
            model.onResume()
            runCurrent()
            val before = model.uiState.value.freeSlots.map { it.status }

            // Act
            advanceClock(byMillis = FIRST_DEADLINE_IN_MILLIS)
            val afterFirst = model.uiState.value.freeSlots.map { it.status }
            advanceClock(byMillis = SECOND_DEADLINE_IN_MILLIS - FIRST_DEADLINE_IN_MILLIS)
            val afterSecond = model.uiState.value.freeSlots.map { it.status }

            // Assert
            assertThat(before.map { it.javaClass }).containsExactly(
                JointAccountMembersUM.SlotStatusUM.Invited::class.java,
                JointAccountMembersUM.SlotStatusUM.Invited::class.java,
            ).inOrder()
            assertThat(afterFirst.map { it.javaClass }).containsExactly(
                JointAccountMembersUM.SlotStatusUM.Expired::class.java,
                JointAccountMembersUM.SlotStatusUM.Invited::class.java,
            ).inOrder()
            assertThat(afterSecond).containsExactly(
                JointAccountMembersUM.SlotStatusUM.Expired,
                JointAccountMembersUM.SlotStatusUM.Expired,
            ).inOrder()
        }

    @Test
    fun `GIVEN an invite expired while the screen was paused WHEN resumed THEN the slot reads Expired`() = runTest {
        // Arrange
        val invite = JointAccountInvite(slot = 1, expiresAtMillis = NOW + FIRST_DEADLINE_IN_MILLIS)
        stub(account = pendingAccount(members = listOf(creator)).copy(invites = listOf(invite)))
        val model = createModel()
        model.onResume()
        runCurrent()
        model.onPause()
        runCurrent()
        clock.nowMillis = NOW + SECOND_DEADLINE_IN_MILLIS
        val whilePaused = model.uiState.value.freeSlots.first().status

        // Act
        model.onResume()
        runCurrent()

        // Assert
        assertThat(whilePaused).isInstanceOf(JointAccountMembersUM.SlotStatusUM.Invited::class.java)
        assertThat(model.uiState.value.freeSlots.first().status).isEqualTo(JointAccountMembersUM.SlotStatusUM.Expired)
    }

    /** The fake clock and the test scheduler move together, as the wall clock and `delay` do in production */
    private fun TestScope.advanceClock(byMillis: Long) {
        clock.nowMillis += byMillis
        advanceTimeBy(byMillis)
        runCurrent()
    }

    private fun <C : Any> SlotNavigation<C>.trackSlot(): List<C?> {
        val tracked = mutableListOf<C?>()
        subscribe { event -> tracked.add(event.transformer(tracked.lastOrNull())) }
        return tracked
    }

    @Test
    fun `GIVEN a member joins after the first load WHEN the account updates THEN a status change is emitted`() =
        runTest {
            // Arrange
            val accounts = MutableStateFlow(listOf(pendingAccount(members = listOf(creator))))
            every { supplier.invoke(userWalletId = USER_WALLET_ID) } returns accounts
            val model = createModel()
            model.onResume()
            val changes = mutableListOf<Unit>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.memberStatusChanges.collect { changes += it } }
            runCurrent()

            // Act
            accounts.value = listOf(pendingAccount(members = listOf(creator, member)))
            advanceUntilIdle()

            // Assert
            assertThat(changes).hasSize(1)
        }

    @Test
    fun `GIVEN a member confirms WHEN the account updates THEN a status change is emitted`() = runTest {
        // Arrange
        val confirming = pendingAccount(members = listOf(creator, member)).copy(status = JointAccount.Status.CONFIRMING)
        val accounts = MutableStateFlow(listOf(confirming))
        every { supplier.invoke(userWalletId = USER_WALLET_ID) } returns accounts
        val model = createModel()
        model.onResume()
        val changes = mutableListOf<Unit>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.memberStatusChanges.collect { changes += it } }
        runCurrent()

        // Act
        accounts.value = listOf(confirming.copy(members = listOf(creator, member.copy(signature = "0xsigned"))))
        advanceUntilIdle()

        // Assert
        assertThat(changes).hasSize(1)
    }

    @Test
    fun `GIVEN paused screen WHEN a member joins THEN no status change is emitted`() = runTest {
        // Arrange
        val accounts = MutableStateFlow(listOf(pendingAccount(members = listOf(creator))))
        every { supplier.invoke(userWalletId = USER_WALLET_ID) } returns accounts
        val model = createModel()
        model.onResume()
        model.onPause()
        val changes = mutableListOf<Unit>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.memberStatusChanges.collect { changes += it } }
        runCurrent()

        // Act
        accounts.value = listOf(pendingAccount(members = listOf(creator, member)))
        advanceUntilIdle()

        // Assert
        assertThat(changes).isEmpty()
    }

    @Test
    fun `GIVEN first load and a refresh without changes WHEN the account arrives THEN no status change is emitted`() =
        runTest {
            // Arrange
            val accounts = MutableStateFlow(listOf(pendingAccount(members = listOf(creator, member))))
            every { supplier.invoke(userWalletId = USER_WALLET_ID) } returns accounts
            val changes = mutableListOf<Unit>()

            // Act
            val model = createModel()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.memberStatusChanges.collect { changes += it } }
            runCurrent()
            accounts.value = listOf(pendingAccount(members = listOf(creator, member)).copy(source = StatusSource.CACHE))
            advanceUntilIdle()

            // Assert
            assertThat(changes).isEmpty()
        }

    @Test
    fun `GIVEN the composition does not verify WHEN the screen opens THEN it closes with the alert shown`() = runTest {
        // Arrange
        stub(account = pendingAccount(members = listOf(creator)))
        coEvery {
            compositionVerifier.isVerified(account = any(), jointAccount = any())
        } returns false

        // Act
        val model = createModel()

        // Assert
        verify(exactly = 1) { closeFlow() }
        verify(exactly = 0) { router.pop() }
        verify(exactly = 1) { jointAccountCompromisedAlert.show(USER_WALLET_ID) }
        assertThat(model.uiState.value.joined).isEmpty()
    }

    private fun stub(account: JointAccount) {
        every { supplier.invoke(userWalletId = USER_WALLET_ID) } returns flowOf(listOf(account))
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
                JointAccountMembersListParams(
                    userWalletId = USER_WALLET_ID,
                    cryptoAccountId = cryptoAccountId,
                    onCloseFlow = closeFlow,
                ),
            ),
            dispatchers = createTestingCoroutineDispatcherProvider(),
            router = router,
            clipboardManager = clipboardManager,
            uiMessageSender = uiMessageSender,
            jointAccountListSupplier = supplier,
            jointAccountListFetcher = jointAccountListFetcher,
            pollingCoordinator = pollingCoordinator,
            ownerAddressResolver = ownerAddressResolver,
            singleAccountSupplier = singleAccountSupplier,
            stateController = JointAccountMembersStateController(),
            inviteSharing = JointAccountInviteSharing(shareManager = shareManager),
            currentTimeProvider = clock,
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
        const val NOW = 1_700_000_000_000L
        const val FIRST_DEADLINE_IN_MILLIS = 60_000L
        const val SECOND_DEADLINE_IN_MILLIS = 120_000L
        const val INVITE_MESSAGE = "Join my joint account “Family savings” in Tangem\n" +
            "https://tangem.com/joint-account/join/invite-a"
        const val SAFE_ADDRESS = "0x5afe000000000000000000000000000000000001"

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
    }
}

private class FakeCurrentTimeProvider(var nowMillis: Long = 1_700_000_000_000L) : CurrentTimeProvider {
    override fun nowMillis(): Long = nowMillis
}