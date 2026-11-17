package com.tangem.features.jointaccount.join.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.account.status.utils.MainExpandedAccountsHolder
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointAccountConfig
import com.tangem.domain.jointaccount.model.JointAccountFlowError
import com.tangem.domain.jointaccount.model.JointAccountJoinError
import com.tangem.domain.jointaccount.usecase.JoinJointAccountUseCase
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.models.errors.GetUserWalletError
import com.tangem.domain.wallets.usecase.EnsureWalletLinkedUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.wallets.usecase.SelectWalletUseCase
import com.tangem.features.jointaccount.analytics.JointAccountAnalyticsEvent
import com.tangem.features.jointaccount.analytics.JointAccountAnalyticsEvent.ErrorSource
import com.tangem.features.jointaccount.analytics.JointAccountAnalyticsEvent.JointAccountError
import com.tangem.features.jointaccount.analytics.JointAccountAnalyticsEvent.LimitSource
import com.tangem.test.mock.MockAccounts
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import com.tangem.core.ui.R as CoreUiR

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JointAccountJoinModelTest {

    private val joinUseCase: JoinJointAccountUseCase = mockk()
    private val singleAccountListSupplier: SingleAccountListSupplier = mockk()
    private val selectWalletUseCase: SelectWalletUseCase = mockk()
    private val expandedAccountsHolder: MainExpandedAccountsHolder = mockk(relaxed = true)
    private val getUserWalletUseCase: GetUserWalletUseCase = mockk()
    private val ensureWalletLinkedUseCase: EnsureWalletLinkedUseCase = mockk()
    private val userWallet: UserWallet = mockk()
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            joinUseCase,
            singleAccountListSupplier,
            selectWalletUseCase,
            expandedAccountsHolder,
            getUserWalletUseCase,
            ensureWalletLinkedUseCase,
            uiMessageSender,
            analyticsEventHandler,
        )
        coEvery { singleAccountListSupplier.getSyncOrNull(USER_WALLET_ID) } returns MockAccounts.onlyMainAccount
        coEvery { selectWalletUseCase(USER_WALLET_ID) } returns userWallet.right()
        every { getUserWalletUseCase(USER_WALLET_ID) } returns userWallet.right()
        coEvery { ensureWalletLinkedUseCase(userWallet) } returns true
    }

    @Test
    fun `GIVEN complete draft WHEN join THEN the main screen opens with the account added snackbar`() = runTest {
        // Arrange
        val model = createModelWithDraft()
        coEvery {
            joinUseCase(
                userWalletId = USER_WALLET_ID,
                inviteId = INVITE_ID,
                config = CONFIG,
                creatorAddress = CREATOR_ADDRESS,
                memberName = NAME,
            )
        } returns joinedAccount.right()

        // Act
        val action = model.join(inviteId = INVITE_ID, memberName = NAME)

        // Assert
        assertThat(action).isEqualTo(JoinResultAction.OpenMain(userWalletId = USER_WALLET_ID))
        assertThat(model.draftHolder.draft.value.displayName).isEqualTo(NAME)
        val message = slot<SnackbarMessage>()
        verify(exactly = 1) { uiMessageSender.send(capture(message)) }
        assertThat(message.captured.message)
            .isEqualTo(resourceReference(CoreUiR.string.joint_account_join_success_message))
        assertThat(message.captured.startIconId).isEqualTo(CoreUiR.drawable.ic_success_20)
        verify(exactly = 1) {
            analyticsEventHandler.send(JointAccountAnalyticsEvent.AccountJoined(membersCount = 3, threshold = 2))
        }
    }

    @Test
    fun `GIVEN complete draft WHEN join THEN the joining wallet is selected and the joint account expanded`() = runTest {
        // Arrange
        val model = createModelWithDraft()
        coEvery {
            joinUseCase(
                userWalletId = USER_WALLET_ID,
                inviteId = INVITE_ID,
                config = CONFIG,
                creatorAddress = CREATOR_ADDRESS,
                memberName = NAME,
            )
        } returns joinedAccount.right()
        val jointAccountId = AccountId.forJointAccount(userWalletId = USER_WALLET_ID, value = ACCOUNT_ID).getOrNull()!!

        // Act
        model.join(inviteId = INVITE_ID, memberName = NAME)

        // Assert
        coVerify(exactly = 1) { selectWalletUseCase(USER_WALLET_ID) }
        verify(exactly = 1) { expandedAccountsHolder.expandAccount(jointAccountId) }
    }

    @Test
    fun `GIVEN 20 active joint accounts WHEN join THEN the limit dialog is shown and nothing is signed`() = runTest {
        // Arrange
        val model = createModelWithDraft()
        coEvery {
            singleAccountListSupplier.getSyncOrNull(USER_WALLET_ID)
        } returns MockAccounts.fullJointAccountList

        // Act
        val action = model.join(inviteId = INVITE_ID, memberName = NAME)

        // Assert
        assertThat(action).isEqualTo(JoinResultAction.None)
        coVerify(exactly = 0) { joinUseCase(any(), any(), any(), any(), any()) }
        val message = slot<DialogMessage>()
        verify(exactly = 1) { uiMessageSender.send(capture(message)) }
        assertThat(message.captured.title).isEqualTo(resourceReference(CoreUiR.string.account_add_limit_dialog_title))
        verify(exactly = 1) {
            analyticsEventHandler.send(JointAccountAnalyticsEvent.NoticeAccountsLimitReached(LimitSource.Join))
        }
    }

    @Test
    fun `GIVEN no cached account list WHEN join THEN the limit does not block the join`() = runTest {
        // Arrange
        val model = createModelWithDraft()
        coEvery { singleAccountListSupplier.getSyncOrNull(USER_WALLET_ID) } returns null
        coEvery {
            joinUseCase(any(), any(), any(), any(), any())
        } returns joinedAccount.right()

        // Act
        val action = model.join(inviteId = INVITE_ID, memberName = NAME)

        // Assert
        assertThat(action).isEqualTo(JoinResultAction.OpenMain(userWalletId = USER_WALLET_ID))
        coVerify(exactly = 1) { joinUseCase(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN signing dismissed WHEN join THEN the form stays and no dialog is shown`() = runTest {
        // Arrange
        val model = createModelWithDraft()
        coEvery {
            joinUseCase(any(), any(), any(), any(), any())
        } returns JointAccountFlowError.UserCancelled.left()

        // Act
        val action = model.join(inviteId = INVITE_ID, memberName = NAME)

        // Assert
        assertThat(action).isEqualTo(JoinResultAction.None)
        verify(exactly = 0) { uiMessageSender.send(any()) }
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN spent invite WHEN join THEN the dialog is shown and the flow closes`() = runTest {
        // Arrange
        val model = createModelWithDraft()
        coEvery {
            joinUseCase(any(), any(), any(), any(), any())
        } returns JointAccountJoinError.InvalidInvite(cause = failure).left()

        // Act
        val action = model.join(inviteId = INVITE_ID, memberName = NAME)

        // Assert
        assertThat(action).isEqualTo(JoinResultAction.CloseFlow)
        verify(exactly = 1) { uiMessageSender.send(any()) }
        verify(exactly = 1) {
            analyticsEventHandler.send(JointAccountError(source = ErrorSource.Join, cause = failure))
        }
    }

    @Test
    fun `GIVEN join fails WHEN join THEN the dialog is shown and the form stays`() = runTest {
        // Arrange
        val model = createModelWithDraft()
        coEvery {
            joinUseCase(any(), any(), any(), any(), any())
        } returns JointAccountFlowError.Failed(cause = failure).left()

        // Act
        val action = model.join(inviteId = INVITE_ID, memberName = NAME)

        // Assert
        assertThat(action).isEqualTo(JoinResultAction.None)
        verify(exactly = 1) { uiMessageSender.send(any()) }
        verify(exactly = 1) {
            analyticsEventHandler.send(JointAccountError(source = ErrorSource.Join, cause = failure))
        }
    }

    @Test
    fun `GIVEN draft without config WHEN join THEN nothing is signed and the dialog is shown`() = runTest {
        // Arrange
        val model = createModel()
        model.draftHolder.setSelectedWallet(walletId = USER_WALLET_ID)

        // Act
        val action = model.join(inviteId = INVITE_ID, memberName = NAME)

        // Assert
        assertThat(action).isEqualTo(JoinResultAction.None)
        coVerify(exactly = 0) { joinUseCase(any(), any(), any(), any(), any()) }
        verify(exactly = 1) { uiMessageSender.send(any()) }
        verify(exactly = 1) {
            analyticsEventHandler.send(JointAccountError(source = ErrorSource.Join, errorMessage = "Incomplete draft"))
        }
    }

    @Test
    fun `GIVEN linked wallet WHEN join THEN the link is ensured before joining`() = runTest {
        // Arrange
        val model = createModelWithDraft()
        coEvery { joinUseCase(any(), any(), any(), any(), any()) } returns joinedAccount.right()

        // Act
        model.join(inviteId = INVITE_ID, memberName = NAME)

        // Assert
        coVerifyOrder {
            ensureWalletLinkedUseCase(userWallet)
            joinUseCase(USER_WALLET_ID, INVITE_ID, CONFIG, CREATOR_ADDRESS, NAME)
        }
    }

    @Test
    fun `GIVEN wallet link refused WHEN join THEN nothing is joined and nothing is shown`() = runTest {
        // Arrange
        val model = createModelWithDraft()
        coEvery { ensureWalletLinkedUseCase(userWallet) } returns false

        // Act
        val action = model.join(inviteId = INVITE_ID, memberName = NAME)

        // Assert
        assertThat(action).isEqualTo(JoinResultAction.None)
        coVerify(exactly = 0) { joinUseCase(any(), any(), any(), any(), any()) }
        verify(exactly = 0) { uiMessageSender.send(any()) }
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN wallet not found WHEN join THEN the join goes on without the link check`() = runTest {
        // Arrange
        val model = createModelWithDraft()
        every { getUserWalletUseCase(USER_WALLET_ID) } returns GetUserWalletError.UserWalletNotFound.left()
        coEvery { joinUseCase(any(), any(), any(), any(), any()) } returns joinedAccount.right()

        // Act
        val action = model.join(inviteId = INVITE_ID, memberName = NAME)

        // Assert
        assertThat(action).isEqualTo(JoinResultAction.OpenMain(userWalletId = USER_WALLET_ID))
        coVerify(exactly = 0) { ensureWalletLinkedUseCase(any()) }
    }

    private fun createModelWithDraft(): JointAccountJoinModel {
        return createModel().apply {
            draftHolder.setSelectedWallet(walletId = USER_WALLET_ID)
            draftHolder.setInvite(config = CONFIG, creatorAddress = CREATOR_ADDRESS)
        }
    }

    private fun createModel(): JointAccountJoinModel = JointAccountJoinModel(
        dispatchers = TestingCoroutineDispatcherProvider(),
        joinJointAccountUseCase = joinUseCase,
        singleAccountListSupplier = singleAccountListSupplier,
        selectWalletUseCase = selectWalletUseCase,
        expandedAccountsHolder = expandedAccountsHolder,
        getUserWalletUseCase = getUserWalletUseCase,
        ensureWalletLinkedUseCase = ensureWalletLinkedUseCase,
        uiMessageSender = uiMessageSender,
        analyticsEventHandler = analyticsEventHandler,
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        const val INVITE_ID = "invite-a"
        const val NAME = "Bob"
        const val CREATOR_ADDRESS = "0x2B5AD5c4795c026514f8317c7a215E218DcCD6cF"
        val failure = IllegalStateException("boom")
        val ACCOUNT_ID = "a1".repeat(n = 32)

        val CONFIG = JointAccountConfig(
            name = "Family",
            icon = "Family",
            iconColor = "Azure",
            membersCount = 3,
            threshold = 2,
        )

        val joinedAccount = JointAccount(
            cryptoAccountId = ACCOUNT_ID,
            membersCount = 3,
            threshold = 2,
            safeAddress = null,
            status = JointAccount.Status.PENDING,
            members = emptyList(),
            source = StatusSource.ACTUAL,
        )
    }
}