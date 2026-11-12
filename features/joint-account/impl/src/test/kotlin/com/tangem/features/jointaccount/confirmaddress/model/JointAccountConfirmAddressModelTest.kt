package com.tangem.features.jointaccount.confirmaddress.model

import arrow.core.left
import arrow.core.right
import com.tangem.common.routing.AppRoute
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.account.supplier.SingleAccountSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.models.WalletMetaInfo
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointAccountConfirmationError
import com.tangem.domain.jointaccount.model.JointAccountFlowError
import com.tangem.domain.jointaccount.polling.JointAccountPollingCoordinator
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.jointaccount.usecase.ConfirmJointAccountUseCase
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.jointaccount.confirmaddress.JointAccountConfirmAddressComponent
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import com.tangem.test.mock.MockAccounts
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
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
    private val onActivated: () -> Unit = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val shareManager: ShareManager = mockk(relaxed = true)
    private val supplier: SingleJointAccountListSupplier = mockk()
    private val singleAccountSupplier: SingleAccountSupplier = mockk()
    private val confirmUseCase: ConfirmJointAccountUseCase = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase = mockk()
    private val pollingCoordinator: JointAccountPollingCoordinator = mockk()

    private val jointAccount = MockAccounts.createJointAccount(userWalletId = USER_WALLET_ID)

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            router,
            onActivated,
            uiMessageSender,
            shareManager,
            supplier,
            singleAccountSupplier,
            confirmUseCase,
            userWalletsListRepository,
            getWalletMetaInfoUseCase,
            pollingCoordinator,
        )
        every { pollingCoordinator.poll(walletId = any()) } returns emptyFlow()
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
    fun `GIVEN wallet WHEN mismatch clicked THEN the support chat opens`() = runTest {
        // Arrange
        val metaInfo = WalletMetaInfo(userWalletId = USER_WALLET_ID)
        coEvery { getWalletMetaInfoUseCase(USER_WALLET_ID) } returns metaInfo.right()
        val model = createModel()

        // Act
        model.uiState.value.onMismatchClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { router.push(AppRoute.Usedesk(walletMetaInfo = metaInfo)) }
    }

    @Test
    fun `GIVEN signing in flight WHEN mismatch clicked THEN the support chat does not open`() = runTest {
        // Arrange
        val signing = CompletableDeferred<Unit>()
        coEvery { confirmUseCase(account = jointAccount) } coAnswers {
            signing.await()
            confirmingAccount().right()
        }
        coEvery { getWalletMetaInfoUseCase(USER_WALLET_ID) } returns WalletMetaInfo(USER_WALLET_ID).right()
        val model = createModel()
        model.uiState.value.onConfirmClick()
        advanceUntilIdle()

        // Act
        model.uiState.value.onMismatchClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { router.push(any()) }
        signing.complete(Unit)
    }

    @Test
    fun `GIVEN chat lookup pending WHEN confirm clicked before it resolves THEN the support chat does not open`() =
        runTest {
            // Arrange
            val lookup = CompletableDeferred<Unit>()
            val signing = CompletableDeferred<Unit>()
            coEvery { getWalletMetaInfoUseCase(USER_WALLET_ID) } coAnswers {
                lookup.await()
                WalletMetaInfo(USER_WALLET_ID).right()
            }
            coEvery { confirmUseCase(account = jointAccount) } coAnswers {
                signing.await()
                confirmingAccount().right()
            }
            val model = createModel()
            model.uiState.value.onMismatchClick()
            advanceUntilIdle()
            model.uiState.value.onConfirmClick()
            advanceUntilIdle()

            // Act
            lookup.complete(Unit)
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { router.push(any()) }
            signing.complete(Unit)
        }

    @Test
    fun `GIVEN the last signature activates the account WHEN confirm clicked THEN the flow is left as activated`() =
        runTest {
            // Arrange
            coEvery { confirmUseCase(account = jointAccount) } returns
                confirmingAccount().copy(status = JointAccount.Status.ACTIVE).right()
            val model = createModel()

            // Act
            model.uiState.value.onConfirmClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { onActivated() }
            verify(exactly = 0) { router.pop() }
            verify(exactly = 0) { uiMessageSender.send(any()) }
            assertThat(model.uiState.value.isConfirming).isFalse()
        }

    @Test
    fun `GIVEN others are still to confirm WHEN confirm clicked THEN the screen pops without a snackbar`() = runTest {
        // Arrange — the members screen behind shows who is still due, a toast would only duplicate it
        coEvery { confirmUseCase(account = jointAccount) } returns confirmingAccount().right()
        val model = createModel()

        // Act
        model.uiState.value.onConfirmClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { uiMessageSender.send(any()) }
        verify(exactly = 1) { router.pop() }
        verify(exactly = 0) { onActivated() }
        assertThat(model.uiState.value.isConfirming).isFalse()
    }

    @Test
    fun `GIVEN signing in flight WHEN confirm clicked again THEN the button loads and signs once`() = runTest {
        // Arrange
        val signing = CompletableDeferred<Unit>()
        coEvery { confirmUseCase(account = jointAccount) } coAnswers {
            signing.await()
            confirmingAccount().right()
        }
        val model = createModel()

        // Act
        model.uiState.value.onConfirmClick()
        model.uiState.value.onConfirmClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.isConfirming).isTrue()
        coVerify(exactly = 1) { confirmUseCase(account = jointAccount) }
        signing.complete(Unit)
    }

    @Test
    fun `GIVEN signing dismissed WHEN confirm clicked THEN no dialog and the loader stops`() = runTest {
        // Arrange
        coEvery { confirmUseCase(account = jointAccount) } returns JointAccountFlowError.UserCancelled.left()
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
    fun `GIVEN confirmation fails WHEN confirm clicked THEN error dialog is shown`() = runTest {
        // Arrange
        coEvery { confirmUseCase(account = jointAccount) } returns JointAccountConfirmationError.NotMember.left()
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
        coVerify(exactly = 0) { confirmUseCase(account = any()) }
    }

    @Test
    fun `GIVEN model created WHEN resumed THEN the joint accounts are polled for the wallet`() = runTest {
        // Arrange
        val model = createModel()
        verify(exactly = 0) { pollingCoordinator.poll(walletId = any()) }

        // Act
        model.onResume()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { pollingCoordinator.poll(walletId = USER_WALLET_ID) }
    }

    @Test
    fun `GIVEN resumed model WHEN paused THEN polling stops`() = runTest {
        // Arrange
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
                JointAccountConfirmAddressComponent.Params(
                    userWalletId = USER_WALLET_ID,
                    cryptoAccountId = ACCOUNT_ID,
                    onActivated = onActivated,
                ),
            ),
            dispatchers = createTestingCoroutineDispatcherProvider(),
            router = router,
            uiMessageSender = uiMessageSender,
            shareManager = shareManager,
            jointAccountListSupplier = supplier,
            singleAccountSupplier = singleAccountSupplier,
            confirmJointAccountUseCase = confirmUseCase,
            userWalletsListRepository = userWalletsListRepository,
            getWalletMetaInfoUseCase = getWalletMetaInfoUseCase,
            pollingCoordinator = pollingCoordinator,
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