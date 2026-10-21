package com.tangem.features.jointaccount.main.confirmaddress.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.account.supplier.SingleAccountSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.feedback.models.WalletMetaInfo
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointAccountActivationError
import com.tangem.domain.jointaccount.model.JointAccountFlowError
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.jointaccount.usecase.ActivateJointAccountUseCase
import com.tangem.domain.jointaccount.verifier.JointAccountCompositionVerifier
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.jointaccount.compromised.JointAccountCompromiseWatcher
import com.tangem.features.jointaccount.compromised.JointAccountCompromisedAlert
import com.tangem.features.jointaccount.main.confirmaddress.JointAccountConfirmAddressComponent
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import com.tangem.test.mock.MockAccounts
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import com.tangem.core.ui.R as CoreUiR

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JointAccountConfirmAddressModelTest {

    private val router: Router = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val shareManager: ShareManager = mockk(relaxed = true)
    private val supplier: SingleJointAccountListSupplier = mockk()
    private val singleAccountSupplier: SingleAccountSupplier = mockk()
    private val activateUseCase: ActivateJointAccountUseCase = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase = mockk()
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase = mockk(relaxed = true)
    private val compositionVerifier: JointAccountCompositionVerifier = mockk()
    private val singleAccountListSupplier: SingleAccountListSupplier = mockk()
    private val jointAccountCompromisedAlert: JointAccountCompromisedAlert = mockk(relaxed = true)

    private val jointAccount = MockAccounts.createJointAccount(userWalletId = USER_WALLET_ID)

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            router,
            uiMessageSender,
            shareManager,
            supplier,
            singleAccountSupplier,
            activateUseCase,
            userWalletsListRepository,
            getWalletMetaInfoUseCase,
            sendFeedbackEmailUseCase,
            compositionVerifier,
            singleAccountListSupplier,
            jointAccountCompromisedAlert,
        )
        val localAccount = mockk<Account.Joint> {
            every { accountId } returns AccountId.forJointAccount(USER_WALLET_ID, ACCOUNT_ID).getOrNull()!!
            every { ownerKeyIndex } returns jointAccount.ownerKeyIndex
        }
        every { singleAccountListSupplier(USER_WALLET_ID) } returns
            flowOf(mockk<AccountList> { every { accounts } returns listOf(localAccount) })
        coEvery { compositionVerifier.isVerified(userWalletId = any(), ownerKeyIndex = any(), jointAccount = any()) } returns
            true
        val coldWallet = mockk<UserWallet.Cold> {
            every { walletId } returns USER_WALLET_ID
        }
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(coldWallet))
        every { supplier.invoke(userWalletId = USER_WALLET_ID) } returns flowOf(listOf(confirmingAccount()))
        every { singleAccountSupplier.filterJointAccount(accountId = any()) } returns flowOf(jointAccount)
    }

    @Test
    fun `GIVEN confirming account WHEN model created THEN the safe address is shown`() = runTest {
        // Act
        val model = createModel()

        // Assert
        assertThat(model.uiState.value.address).isEqualTo(SAFE_ADDRESS)
    }

    @Test
    fun `GIVEN no safe address yet WHEN model created THEN no address and confirm does nothing`() = runTest {
        // Arrange
        every { supplier.invoke(userWalletId = USER_WALLET_ID) } returns
            flowOf(listOf(confirmingAccount().copy(safeAddress = null)))

        // Act
        val model = createModel()
        model.uiState.value.onShareClick()

        // Assert
        assertThat(model.uiState.value.address).isNull()
        verify(exactly = 0) { shareManager.shareText(any()) }
    }

    @Test
    fun `GIVEN cold wallet WHEN model created THEN the confirm button carries the card icon`() = runTest {
        // Act
        val model = createModel()

        // Assert
        assertThat(model.uiState.value.buttonIcon).isEqualTo(TangemIconUM.Icon(iconRes = CoreUiR.drawable.ic_tangem_24))
    }

    @Test
    fun `GIVEN hot wallet WHEN model created THEN the confirm button carries no icon`() = runTest {
        // Arrange
        val hotWallet = mockk<UserWallet.Hot> {
            every { walletId } returns USER_WALLET_ID
        }
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(hotWallet))

        // Act
        val model = createModel()

        // Assert
        assertThat(model.uiState.value.buttonIcon).isNull()
    }

    @Test
    fun `GIVEN address WHEN share clicked THEN the address is shared`() = runTest {
        // Arrange
        val model = createModel()

        // Act
        model.uiState.value.onShareClick()

        // Assert
        verify(exactly = 1) { shareManager.shareText(text = SAFE_ADDRESS) }
    }

    @Test
    fun `GIVEN wallet WHEN mismatch clicked THEN a support email is sent`() = runTest {
        // Arrange
        val metaInfo = WalletMetaInfo(userWalletId = USER_WALLET_ID)
        coEvery { getWalletMetaInfoUseCase(USER_WALLET_ID) } returns metaInfo.right()
        val model = createModel()

        // Act
        model.uiState.value.onMismatchClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) {
            sendFeedbackEmailUseCase(type = FeedbackEmailType.DirectUserRequest(walletMetaInfo = metaInfo))
        }
    }

    @Test
    fun `GIVEN signing succeeds WHEN confirm clicked THEN snackbar shown and the screen pops`() = runTest {
        // Arrange
        coEvery { activateUseCase(account = jointAccount) } returns Unit.right()
        val model = createModel()

        // Act
        model.uiState.value.onConfirmClick()
        advanceUntilIdle()

        // Assert
        val message = slot<SnackbarMessage>()
        verify(exactly = 1) { uiMessageSender.send(capture(message)) }
        assertThat(message.captured.message).isEqualTo(
            resourceReference(CoreUiR.string.joint_account_activated_snackbar),
        )
        verify(exactly = 1) { router.pop() }
        assertThat(model.uiState.value.isConfirming).isFalse()
    }

    @Test
    fun `GIVEN signing in flight WHEN confirm clicked again THEN the button loads and signs once`() = runTest {
        // Arrange
        val signing = CompletableDeferred<Unit>()
        coEvery { activateUseCase(account = jointAccount) } coAnswers {
            signing.await()
            Unit.right()
        }
        val model = createModel()

        // Act
        model.uiState.value.onConfirmClick()
        model.uiState.value.onConfirmClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.isConfirming).isTrue()
        coVerify(exactly = 1) { activateUseCase(account = jointAccount) }
        signing.complete(Unit)
    }

    @Test
    fun `GIVEN signing dismissed WHEN confirm clicked THEN no dialog and the loader stops`() = runTest {
        // Arrange
        coEvery { activateUseCase(account = jointAccount) } returns JointAccountFlowError.UserCancelled.left()
        val model = createModel()

        // Act
        model.uiState.value.onConfirmClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { uiMessageSender.send(any()) }
        verify(exactly = 0) { router.pop() }
        assertThat(model.uiState.value.isConfirming).isFalse()
    }

    @Test
    fun `GIVEN activation fails WHEN confirm clicked THEN error dialog is shown`() = runTest {
        // Arrange
        coEvery { activateUseCase(account = jointAccount) } returns JointAccountActivationError.NotCreator.left()
        val model = createModel()

        // Act
        model.uiState.value.onConfirmClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { uiMessageSender.send(any<DialogMessage>()) }
        verify(exactly = 0) { router.pop() }
    }

    @Test
    fun `GIVEN account not in the store WHEN confirm clicked THEN error dialog without signing`() = runTest {
        // Arrange
        every { singleAccountSupplier.filterJointAccount(accountId = any()) } returns emptyFlow()
        val model = createModel()

        // Act
        model.uiState.value.onConfirmClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { uiMessageSender.send(any<DialogMessage>()) }
        coVerify(exactly = 0) { activateUseCase(account = any()) }
    }

    @Test
    fun `GIVEN the composition does not verify WHEN the screen opens THEN it closes with the alert shown`() = runTest {
        // Arrange
        coEvery {
            compositionVerifier.isVerified(userWalletId = any(), ownerKeyIndex = any(), jointAccount = any())
        } returns false

        // Act
        val model = createModel()

        // Assert
        verify(exactly = 1) { router.pop() }
        verify(exactly = 1) { jointAccountCompromisedAlert.show(USER_WALLET_ID) }
        assertThat(model.uiState.value.address).isNull()
    }

    private fun confirmingAccount() = JointAccount(
        cryptoAccountId = ACCOUNT_ID,
        membersCount = 2,
        threshold = 2,
        safeAddress = SAFE_ADDRESS,
        status = JointAccount.Status.CONFIRMING,
        members = listOf(
            JointAccount.Member(name = "Ivan", address = "0xC0FFEE", role = JointAccount.Role.CREATOR),
            JointAccount.Member(name = "Dasha", address = "0xDA5HA", role = JointAccount.Role.MEMBER),
        ),
        source = StatusSource.ACTUAL,
    )

    private fun TestScope.createModel(): JointAccountConfirmAddressModel {
        val model = JointAccountConfirmAddressModel(
            paramsContainer = MutableParamsContainer(
                JointAccountConfirmAddressComponent.Params(userWalletId = USER_WALLET_ID, cryptoAccountId = ACCOUNT_ID),
            ),
            dispatchers = createTestingCoroutineDispatcherProvider(),
            router = router,
            uiMessageSender = uiMessageSender,
            shareManager = shareManager,
            jointAccountListSupplier = supplier,
            singleAccountSupplier = singleAccountSupplier,
            activateJointAccountUseCase = activateUseCase,
            userWalletsListRepository = userWalletsListRepository,
            getWalletMetaInfoUseCase = getWalletMetaInfoUseCase,
            sendFeedbackEmailUseCase = sendFeedbackEmailUseCase,
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
        const val SAFE_ADDRESS = "0x5afe000000000000000000000000000000000001"
    }
}