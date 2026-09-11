package com.tangem.features.jointaccount.join.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointAccountConfig
import com.tangem.domain.jointaccount.model.JointAccountFlowError
import com.tangem.domain.jointaccount.model.JointAccountJoinError
import com.tangem.domain.jointaccount.usecase.JoinJointAccountUseCase
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.mock.MockAccounts
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
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
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(joinUseCase, singleAccountListSupplier, uiMessageSender)
        coEvery { singleAccountListSupplier.getSyncOrNull(USER_WALLET_ID) } returns MockAccounts.onlyMainAccount
    }

    @Test
    fun `GIVEN complete draft WHEN join THEN the flow closes with the account added snackbar`() = runTest {
        // Arrange
        val model = createModelWithDraft()
        coEvery {
            joinUseCase(userWalletId = USER_WALLET_ID, inviteId = INVITE_ID, config = CONFIG, memberName = NAME)
        } returns joinedAccount.right()

        // Act
        val action = model.join(inviteId = INVITE_ID, memberName = NAME)

        // Assert
        assertThat(action).isEqualTo(JoinResultAction.CloseFlow)
        assertThat(model.draftHolder.draft.value.displayName).isEqualTo(NAME)
        val message = slot<SnackbarMessage>()
        verify(exactly = 1) { uiMessageSender.send(capture(message)) }
        assertThat(message.captured.message)
            .isEqualTo(resourceReference(CoreUiR.string.joint_account_join_success_message))
        assertThat(message.captured.startIconId).isEqualTo(CoreUiR.drawable.ic_success_20)
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
        coVerify(exactly = 0) { joinUseCase(any(), any(), any(), any()) }
        val message = slot<DialogMessage>()
        verify(exactly = 1) { uiMessageSender.send(capture(message)) }
        assertThat(message.captured.title).isEqualTo(resourceReference(CoreUiR.string.account_add_limit_dialog_title))
    }

    @Test
    fun `GIVEN no cached account list WHEN join THEN the limit does not block the join`() = runTest {
        // Arrange
        val model = createModelWithDraft()
        coEvery { singleAccountListSupplier.getSyncOrNull(USER_WALLET_ID) } returns null
        coEvery {
            joinUseCase(userWalletId = any(), inviteId = any(), config = any(), memberName = any())
        } returns joinedAccount.right()

        // Act
        val action = model.join(inviteId = INVITE_ID, memberName = NAME)

        // Assert
        assertThat(action).isEqualTo(JoinResultAction.CloseFlow)
        coVerify(exactly = 1) { joinUseCase(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN signing dismissed WHEN join THEN the form stays and no dialog is shown`() = runTest {
        // Arrange
        val model = createModelWithDraft()
        coEvery {
            joinUseCase(userWalletId = any(), inviteId = any(), config = any(), memberName = any())
        } returns JointAccountFlowError.UserCancelled.left()

        // Act
        val action = model.join(inviteId = INVITE_ID, memberName = NAME)

        // Assert
        assertThat(action).isEqualTo(JoinResultAction.None)
        verify(exactly = 0) { uiMessageSender.send(any()) }
    }

    @Test
    fun `GIVEN spent invite WHEN join THEN the dialog is shown and the flow closes`() = runTest {
        // Arrange
        val model = createModelWithDraft()
        coEvery {
            joinUseCase(userWalletId = any(), inviteId = any(), config = any(), memberName = any())
        } returns JointAccountJoinError.InvalidInvite.left()

        // Act
        val action = model.join(inviteId = INVITE_ID, memberName = NAME)

        // Assert
        assertThat(action).isEqualTo(JoinResultAction.CloseFlow)
        verify(exactly = 1) { uiMessageSender.send(any()) }
    }

    @Test
    fun `GIVEN join fails WHEN join THEN the dialog is shown and the form stays`() = runTest {
        // Arrange
        val model = createModelWithDraft()
        coEvery {
            joinUseCase(userWalletId = any(), inviteId = any(), config = any(), memberName = any())
        } returns JointAccountFlowError.Failed(cause = IllegalStateException("boom")).left()

        // Act
        val action = model.join(inviteId = INVITE_ID, memberName = NAME)

        // Assert
        assertThat(action).isEqualTo(JoinResultAction.None)
        verify(exactly = 1) { uiMessageSender.send(any()) }
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
        coVerify(exactly = 0) { joinUseCase(any(), any(), any(), any()) }
        verify(exactly = 1) { uiMessageSender.send(any()) }
    }

    private fun createModelWithDraft(): JointAccountJoinModel {
        return createModel().apply {
            draftHolder.setSelectedWallet(walletId = USER_WALLET_ID)
            draftHolder.setConfig(config = CONFIG)
        }
    }

    private fun createModel(): JointAccountJoinModel = JointAccountJoinModel(
        dispatchers = TestingCoroutineDispatcherProvider(),
        joinJointAccountUseCase = joinUseCase,
        singleAccountListSupplier = singleAccountListSupplier,
        uiMessageSender = uiMessageSender,
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        const val INVITE_ID = "invite-a"
        const val NAME = "Bob"
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