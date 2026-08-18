package com.tangem.features.hotwallet.createcloudbackup.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.core.TangemSdkError
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessage
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetUM
import com.tangem.core.ui.message.BottomSheetMessage
import com.tangem.core.ui.message.DialogMessage
import com.tangem.crypto.bip39.Mnemonic
import com.tangem.domain.cloudbackup.models.CloudBackupError
import com.tangem.domain.cloudbackup.models.CloudBackupInfo
import com.tangem.domain.cloudbackup.models.CloudBackupSecretData
import com.tangem.domain.cloudbackup.usecase.CreateCloudBackupUseCase
import com.tangem.domain.cloudbackup.usecase.SetCloudBackupStateUseCase
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.ClearHotWalletContextualUnlockUseCase
import com.tangem.domain.wallets.usecase.ExportSeedPhraseUseCase
import com.tangem.domain.wallets.usecase.GetHotWalletContextualUnlockUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.wallets.usecase.UnlockHotWalletContextualUseCase
import com.tangem.features.hotwallet.CreateCloudBackupComponent
import com.tangem.features.hotwallet.createcloudbackup.entity.CreateCloudBackupUM
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.hot.sdk.model.SeedPhrasePrivateInfo
import com.tangem.hot.sdk.model.UnlockHotWallet
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class CreateCloudBackupModelTest {

    private val router: Router = mockk(relaxUnitFun = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val getUserWalletUseCase: GetUserWalletUseCase = mockk()
    private val exportSeedPhraseUseCase: ExportSeedPhraseUseCase = mockk()
    private val getHotWalletContextualUnlockUseCase: GetHotWalletContextualUnlockUseCase = mockk()
    private val unlockHotWalletContextualUseCase: UnlockHotWalletContextualUseCase = mockk()
    private val clearHotWalletContextualUnlockUseCase: ClearHotWalletContextualUnlockUseCase = mockk(relaxed = true)
    private val createCloudBackupUseCase: CreateCloudBackupUseCase = mockk()
    private val setCloudBackupStateUseCase: SetCloudBackupStateUseCase = mockk(relaxed = true)
    private val paramsContainer: ParamsContainer = mockk()

    private val walletId = UserWalletId("011")
    private val hotWalletId: HotWalletId = mockk()
    private val unlockHotWallet: UnlockHotWallet = mockk()
    private var heldUnlock: UnlockHotWallet? = null
    private val hotWallet: UserWallet.Hot = mockk {
        every { walletId } returns this@CreateCloudBackupModelTest.walletId
        every { hotWalletId } returns this@CreateCloudBackupModelTest.hotWalletId
        every { name } returns "My Wallet"
    }

    // Fixed fake word list — never a real secret.
    private val mnemonic: Mnemonic = mockk {
        every { mnemonicComponents } returns listOf("alpha", "bravo", "charlie")
    }
    private val privateInfo = SeedPhrasePrivateInfo(mnemonic = mnemonic, passphrase = null)

    private val backupInfo = CloudBackupInfo(
        fileId = "file-1",
        walletName = "My Wallet",
        createdAtMillis = 0L,
        walletId = "011",
    )

    @BeforeEach
    fun setUp() {
        every { paramsContainer.require<CreateCloudBackupComponent.Params>() } returns
            CreateCloudBackupComponent.Params(walletId)
        every { getUserWalletUseCase(walletId) } returns hotWallet.right()
        every { hotWalletId.authType } returns HotWalletId.AuthType.Password
        coEvery { exportSeedPhraseUseCase.invoke(hotWalletId) } returns privateInfo.right()

        // Mirrors the accessor: unlocking prompts the user and caches the result, which is then held
        coEvery { getHotWalletContextualUnlockUseCase(hotWalletId) } answers { heldUnlock.right() }
        coEvery { unlockHotWalletContextualUseCase(hotWalletId) } answers {
            heldUnlock = unlockHotWallet
            unlockHotWallet.right()
        }
    }

    @Test
    fun `GIVEN strong password AND upload succeeds WHEN confirm clicked THEN state Completed AND finish pops`() =
        runTest {
            // Arrange
            coEvery {
                createCloudBackupUseCase(any(), any(), any(), any())
            } returns backupInfo.right()
            val model = createModel(this)
            advanceUntilIdle()

            // Act
            driveToConfirmAndSubmit(model)
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) {
                createCloudBackupUseCase(
                    walletId = "011",
                    walletName = "My Wallet",
                    secret = any(),
                    password = any(),
                )
            }
            coVerify(exactly = 1) { setCloudBackupStateUseCase("011", isBackedUp = true) }
            val state = model.uiState.value
            assertThat(state).isInstanceOf(CreateCloudBackupUM.Completed::class.java)
            (state as CreateCloudBackupUM.Completed).onFinishClick()
            verify(exactly = 1) { router.pop() }
            model.onDestroy()
        }

    @Test
    fun `GIVEN strong password AND upload fails WHEN confirm clicked THEN error sheet sent AND stays ConfirmPassword`() =
        runTest {
            // Arrange
            coEvery {
                createCloudBackupUseCase(any(), any(), any(), any())
            } returns CloudBackupError.AuthPermissionsMissing.left()
            val model = createModel(this)
            advanceUntilIdle()

            // Act
            driveToConfirmAndSubmit(model)
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { createCloudBackupUseCase(any(), any(), any(), any()) }
            assertThat(model.uiState.value).isInstanceOf(CreateCloudBackupUM.ConfirmPassword::class.java)
            verify { uiMessageSender.send(any()) }
            verify(exactly = 0) { router.pop() }
            model.onDestroy()
        }

    @Test
    fun `GIVEN wallet unlock dismissed WHEN confirm clicked THEN stays ConfirmPassword AND no error sheet`() = runTest {
        // Arrange
        coEvery { exportSeedPhraseUseCase.invoke(hotWalletId) } returns TangemSdkError.UserCancelled().left()
        val model = createModel(this)
        advanceUntilIdle()

        // Act
        driveToConfirmAndSubmit(model)
        advanceUntilIdle()

        // Assert
        val state = model.uiState.value
        assertThat(state).isInstanceOf(CreateCloudBackupUM.ConfirmPassword::class.java)
        assertThat((state as CreateCloudBackupUM.ConfirmPassword).isLoading).isFalse()
        coVerify(exactly = 0) { createCloudBackupUseCase(any(), any(), any(), any()) }
        verify(exactly = 0) { uiMessageSender.send(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN google sign-in canceled WHEN confirm clicked THEN stays ConfirmPassword AND no error sheet`() = runTest {
        // Arrange
        coEvery { createCloudBackupUseCase(any(), any(), any(), any()) } returns CloudBackupError.AuthCanceled.left()
        val model = createModel(this)
        advanceUntilIdle()

        // Act
        driveToConfirmAndSubmit(model)
        advanceUntilIdle()

        // Assert
        val state = model.uiState.value
        assertThat(state).isInstanceOf(CreateCloudBackupUM.ConfirmPassword::class.java)
        assertThat((state as CreateCloudBackupUM.ConfirmPassword).isLoading).isFalse()
        verify(exactly = 0) { uiMessageSender.send(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN upload failed WHEN retry pressed on error sheet THEN upload is repeated AND state Completed`() = runTest {
        // Arrange
        coEvery { createCloudBackupUseCase(any(), any(), any(), any()) } returnsMany listOf(
            CloudBackupError.NetworkError.left(),
            backupInfo.right(),
        )
        val sentMessages = mutableListOf<UiMessage>()
        every { uiMessageSender.send(capture(sentMessages)) } just Runs
        val model = createModel(this)
        advanceUntilIdle()
        driveToConfirmAndSubmit(model)
        advanceUntilIdle()

        // Act
        val sheet = sentMessages.filterIsInstance<BottomSheetMessage>().last()
        val retryButton = sheet.messageBottomSheetUM.elements
            .filterIsInstance<MessageBottomSheetUM.Button>()
            .first()
        retryButton.onClick?.invoke(sheet.messageBottomSheetUM.closeScope)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 2) { createCloudBackupUseCase(any(), any(), any(), any()) }
        assertThat(model.uiState.value).isInstanceOf(CreateCloudBackupUM.Completed::class.java)
        model.onDestroy()
    }

    @Test
    fun `GIVEN wallet with passphrase AND upload failed WHEN retried THEN the passphrase flag survives`() =
        runTest {
            // Arrange
            coEvery { exportSeedPhraseUseCase.invoke(hotWalletId) } answers {
                SeedPhrasePrivateInfo(mnemonic = mnemonic, passphrase = PASSPHRASE.toCharArray()).right()
            }
            val secrets = mutableListOf<CloudBackupSecretData>()
            coEvery {
                createCloudBackupUseCase(any(), any(), capture(secrets), any())
            } returnsMany listOf(CloudBackupError.NetworkError.left(), backupInfo.right())
            val sentMessages = mutableListOf<UiMessage>()
            every { uiMessageSender.send(capture(sentMessages)) } just Runs
            val model = createModel(this)
            advanceUntilIdle()
            driveToConfirmAndSubmit(model)
            advanceUntilIdle()

            // Act
            val sheet = sentMessages.filterIsInstance<BottomSheetMessage>().last()
            sheet.messageBottomSheetUM.elements
                .filterIsInstance<MessageBottomSheetUM.Button>()
                .first()
                .onClick
                ?.invoke(sheet.messageBottomSheetUM.closeScope)
            advanceUntilIdle()

            // Assert
            assertThat(secrets.map { it.isPassphraseRequired }).containsExactly(true, true)
            model.onDestroy()
        }

    @Test
    fun `GIVEN failed upload WHEN retried THEN the wallet is unlocked once and reused`() = runTest {
        // Arrange
        coEvery { createCloudBackupUseCase(any(), any(), any(), any()) } returnsMany listOf(
            CloudBackupError.NetworkError.left(),
            backupInfo.right(),
        )
        val sentMessages = mutableListOf<UiMessage>()
        every { uiMessageSender.send(capture(sentMessages)) } just Runs
        val model = createModel(this)
        advanceUntilIdle()
        driveToConfirmAndSubmit(model)
        advanceUntilIdle()

        // Act
        val sheet = sentMessages.filterIsInstance<BottomSheetMessage>().last()
        sheet.messageBottomSheetUM.elements
            .filterIsInstance<MessageBottomSheetUM.Button>()
            .first()
            .onClick
            ?.invoke(sheet.messageBottomSheetUM.closeScope)
        advanceUntilIdle()

        // Assert
        coVerifyOrder {
            unlockHotWalletContextualUseCase(hotWalletId)
            exportSeedPhraseUseCase.invoke(hotWalletId)
        }
        coVerify(exactly = 1) { unlockHotWalletContextualUseCase(hotWalletId) }
        coVerify(exactly = 2) { exportSeedPhraseUseCase.invoke(hotWalletId) }
        model.onDestroy()
        verify(exactly = 1) { clearHotWalletContextualUnlockUseCase.invoke(walletId) }
    }

    @Test
    fun `GIVEN a wallet already unlocked WHEN confirm clicked THEN the user is not prompted to unlock`() = runTest {
        // Arrange
        heldUnlock = unlockHotWallet
        coEvery { createCloudBackupUseCase(any(), any(), any(), any()) } returns backupInfo.right()
        val model = createModel(this)
        advanceUntilIdle()

        // Act
        driveToConfirmAndSubmit(model)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { unlockHotWalletContextualUseCase(any()) }
        assertThat(model.uiState.value).isInstanceOf(CreateCloudBackupUM.Completed::class.java)
        model.onDestroy()
    }

    @Test
    fun `GIVEN mismatched confirmation WHEN confirm clicked THEN confirm disabled AND no upload`() = runTest {
        // Arrange
        val model = createModel(this)
        advanceUntilIdle()
        (model.uiState.value as CreateCloudBackupUM.SetPassword).onPasswordChange(STRONG_PASSWORD)
        (model.uiState.value as CreateCloudBackupUM.SetPassword).onContinueClick()
        (model.uiState.value as CreateCloudBackupUM.ConfirmPassword).onPasswordChange("Different1!")
        (model.uiState.value as CreateCloudBackupUM.ConfirmPassword).onConsentChange(true)
        val confirmState = model.uiState.value as CreateCloudBackupUM.ConfirmPassword

        // Act
        confirmState.onConfirmClick()
        advanceUntilIdle()

        // Assert
        assertThat(confirmState.isMismatch).isTrue()
        assertThat(confirmState.isConfirmEnabled).isFalse()
        coVerify(exactly = 0) { createCloudBackupUseCase(any(), any(), any(), any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN passwords of increasing strength WHEN typed THEN continue enabled only for strong`() = runTest {
        // Arrange
        val model = createModel(this)
        advanceUntilIdle()

        // Act
        (model.uiState.value as CreateCloudBackupUM.SetPassword).onPasswordChange(WEAK_PASSWORD)
        val afterWeak = (model.uiState.value as CreateCloudBackupUM.SetPassword).isContinueEnabled

        (model.uiState.value as CreateCloudBackupUM.SetPassword).onPasswordChange(MEDIUM_PASSWORD)
        val afterMedium = (model.uiState.value as CreateCloudBackupUM.SetPassword).isContinueEnabled

        (model.uiState.value as CreateCloudBackupUM.SetPassword).onPasswordChange(STRONG_PASSWORD)
        val afterStrong = (model.uiState.value as CreateCloudBackupUM.SetPassword).isContinueEnabled

        // Assert
        assertThat(afterWeak).isFalse()
        assertThat(afterMedium).isFalse()
        assertThat(afterStrong).isTrue()
        model.onDestroy()
    }

    @Test
    fun `GIVEN SetPassword step WHEN back clicked THEN cancel-setup dialog shown AND not popped`() = runTest {
        // Arrange
        val model = createModel(this)
        advanceUntilIdle()

        // Act
        (model.uiState.value as CreateCloudBackupUM.SetPassword).onBackClick()

        // Assert
        verify(exactly = 1) { uiMessageSender.send(any<DialogMessage>()) }
        verify(exactly = 0) { router.pop() }
        model.onDestroy()
    }

    @Test
    fun `GIVEN wallet without access code WHEN finish clicked THEN skippable access code flow opened`() = runTest {
        // Arrange
        every { hotWalletId.authType } returns HotWalletId.AuthType.NoPassword
        coEvery { createCloudBackupUseCase(any(), any(), any(), any()) } returns backupInfo.right()
        val model = createModel(this)
        advanceUntilIdle()
        driveToConfirmAndSubmit(model)
        advanceUntilIdle()

        // Act
        (model.uiState.value as CreateCloudBackupUM.Completed).onFinishClick()

        // Assert
        verify(exactly = 1) {
            router.replaceCurrent(
                AppRoute.UpdateAccessCode(
                    userWalletId = walletId,
                    source = AnalyticsParam.ScreensSources.WalletSettings.value,
                    canSkip = true,
                ),
                onComplete = any(),
            )
        }
        verify(exactly = 0) { router.pop(onComplete = any()) }
        model.onDestroy()
    }

    private fun driveToConfirmAndSubmit(model: CreateCloudBackupModel) {
        (model.uiState.value as CreateCloudBackupUM.SetPassword).onPasswordChange(STRONG_PASSWORD)
        (model.uiState.value as CreateCloudBackupUM.SetPassword).onContinueClick()
        (model.uiState.value as CreateCloudBackupUM.ConfirmPassword).onPasswordChange(STRONG_PASSWORD)
        (model.uiState.value as CreateCloudBackupUM.ConfirmPassword).onConsentChange(true)
        (model.uiState.value as CreateCloudBackupUM.ConfirmPassword).onConfirmClick()
    }

    private fun createModel(testScope: TestScope): CreateCloudBackupModel {
        return CreateCloudBackupModel(
            paramsContainer = paramsContainer,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            router = router,
            uiMessageSender = uiMessageSender,
            getUserWalletUseCase = getUserWalletUseCase,
            exportSeedPhraseUseCase = exportSeedPhraseUseCase,
            getHotWalletContextualUnlockUseCase = getHotWalletContextualUnlockUseCase,
            unlockHotWalletContextualUseCase = unlockHotWalletContextualUseCase,
            clearHotWalletContextualUnlockUseCase = clearHotWalletContextualUnlockUseCase,
            createCloudBackupUseCase = createCloudBackupUseCase,
            setCloudBackupStateUseCase = setCloudBackupStateUseCase,
        )
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
        const val PASSPHRASE = "delta echo"
        const val STRONG_PASSWORD = "Str0ng!Pass"
        const val MEDIUM_PASSWORD = "abcdefgH"
        const val WEAK_PASSWORD = "weak"
    }
}