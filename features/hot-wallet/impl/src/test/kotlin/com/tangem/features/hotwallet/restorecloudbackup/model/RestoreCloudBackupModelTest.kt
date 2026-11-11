package com.tangem.features.hotwallet.restorecloudbackup.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.event.OnboardingAnalyticsEvent
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.dialog.Dialogs
import com.tangem.crypto.bip39.Mnemonic
import com.tangem.domain.cloudbackup.usecase.GetBackupPasswordUseCase
import com.tangem.domain.cloudbackup.models.CloudBackupError
import com.tangem.domain.cloudbackup.models.CloudBackupInfo
import com.tangem.domain.cloudbackup.models.CloudBackupSecretData
import com.tangem.domain.cloudbackup.models.RestoredCloudBackup
import com.tangem.domain.cloudbackup.usecase.RestoreCloudBackupUseCase
import com.tangem.domain.cloudbackup.usecase.SetCloudBackupStateUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.analytics.Settings.NoticeMobileWalletLimit
import com.tangem.features.hotwallet.HotWalletFeatureToggles
import com.tangem.features.hotwallet.MnemonicRepository
import com.tangem.features.hotwallet.addexistingwallet.im.port.model.HotWalletImportError
import com.tangem.features.hotwallet.addexistingwallet.im.port.model.HotWalletImporter
import com.tangem.domain.cloudbackup.restore.CloudRestoreResult
import com.tangem.domain.cloudbackup.restore.CloudRestoreResultHolder
import com.tangem.features.hotwallet.restorecloudbackup.RestoreCloudBackupComponent
import com.tangem.features.hotwallet.restorecloudbackup.entity.RestoreCloudBackupUM
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@OptIn(ExperimentalCoroutinesApi::class)
internal class RestoreCloudBackupModelTest {

    private val restoreCloudBackupUseCase: RestoreCloudBackupUseCase = mockk()
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxUnitFun = true)
    private val getBackupPasswordUseCase: GetBackupPasswordUseCase = mockk {
        coEvery { this@mockk.invoke() } returns null
    }
    private val setCloudBackupStateUseCase: SetCloudBackupStateUseCase = mockk(relaxed = true)
    private val mnemonicRepository: MnemonicRepository = mockk()
    private val hotWalletImporter: HotWalletImporter = mockk()
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val callbacks: RestoreCloudBackupComponent.ModelCallbacks = mockk(relaxed = true)
    private var model: RestoreCloudBackupModel? = null
    private val paramsContainer: ParamsContainer = mockk()
    private val userWallets = MutableStateFlow<List<UserWallet>?>(emptyList())
    private val userWalletsListRepository: UserWalletsListRepository = mockk {
        every { this@mockk.userWallets } returns this@RestoreCloudBackupModelTest.userWallets
    }
    private val hotWalletFeatureToggles: HotWalletFeatureToggles = mockk()

    private val walletId = UserWalletId("011")
    private val mnemonic: Mnemonic = mockk()

    private val backupInfo = CloudBackupInfo(
        fileId = "file-1",
        backupName = "My Wallet",
        walletName = "My Wallet",
        createdAtMillis = 0L,
        walletId = "011",
    )
    private val secondBackupInfo = CloudBackupInfo(
        fileId = "file-2",
        backupName = "Savings",
        walletName = "Savings",
        createdAtMillis = 1_000L,
        walletId = "012",
    )

    @BeforeEach
    fun setUp() {
        userWallets.value = emptyList()
        every { hotWalletFeatureToggles.isMultipleHotWalletsEnabled } returns true
        every { paramsContainer.require<RestoreCloudBackupComponent.Params>() } returns
            RestoreCloudBackupComponent.Params(callbacks)
    }

    @Test
    fun `GIVEN empty holder WHEN model created THEN onBack invoked AND state is empty BackupList`() = runTest {
        // Arrange
        val holder = holderOf()

        // Act
        val model = createModel(this, holder)
        advanceUntilIdle()

        // Assert
        val state = model.uiState.value
        assertThat(state).isInstanceOf(RestoreCloudBackupUM.BackupList::class.java)
        assertThat((state as RestoreCloudBackupUM.BackupList).items).isEmpty()
        verify(exactly = 1) { callbacks.onBack() }
        model.onDestroy()
    }

    @Test
    fun `GIVEN single backup WHEN model created THEN state is EnterPassword for that backup`() = runTest {
        // Arrange
        val holder = holderOf(backupInfo)

        // Act
        val model = createModel(this, holder)
        advanceUntilIdle()

        // Assert
        val state = model.uiState.value
        assertThat(state).isInstanceOf(RestoreCloudBackupUM.EnterPassword::class.java)
        assertThat((state as RestoreCloudBackupUM.EnterPassword).walletName).isEqualTo("My Wallet")
        model.onDestroy()
    }

    @Test
    fun `GIVEN a stored password WHEN EnterPassword opens THEN it is filled in from the manager`() = runTest {
        // Arrange
        coEvery { getBackupPasswordUseCase() } returns "Str0ng!Pass".toCharArray()
        val holder = holderOf(backupInfo)

        // Act
        val model = createModel(this, holder)
        advanceUntilIdle()

        // Assert
        val state = model.uiState.value as RestoreCloudBackupUM.EnterPassword
        assertThat(state.password).isEqualTo("Str0ng!Pass")
        assertThat(state.isRestoreEnabled).isTrue()
        model.onDestroy()
    }

    @Test
    fun `GIVEN the user typed while the picker was up WHEN it answers THEN the typed password stays`() = runTest {
        // Arrange
        val holder = holderOf(backupInfo)
        coEvery { getBackupPasswordUseCase() } coAnswers {
            (model!!.uiState.value as RestoreCloudBackupUM.EnterPassword).onPasswordChange("typed-by-hand")
            "Str0ng!Pass".toCharArray()
        }

        // Act
        model = createModel(this, holder)
        advanceUntilIdle()

        // Assert
        assertThat((model!!.uiState.value as RestoreCloudBackupUM.EnterPassword).password)
            .isEqualTo("typed-by-hand")
        model!!.onDestroy()
    }

    @Test
    fun `GIVEN multiple backups WHEN row selected THEN state is EnterPassword for selected backup`() = runTest {
        // Arrange
        val holder = holderOf(backupInfo, secondBackupInfo)

        // Act
        val model = createModel(this, holder)
        advanceUntilIdle()
        val list = model.uiState.value
        assertThat(list).isInstanceOf(RestoreCloudBackupUM.BackupList::class.java)
        assertThat((list as RestoreCloudBackupUM.BackupList).items).hasSize(2)
        list.items[0].onClick()

        // Assert
        val state = model.uiState.value
        assertThat(state).isInstanceOf(RestoreCloudBackupUM.EnterPassword::class.java)
        assertThat((state as RestoreCloudBackupUM.EnterPassword).walletName).isEqualTo("Savings")
        model.onDestroy()
    }

    @Test
    fun `GIVEN backups WHEN model created THEN account email exposed`() = runTest {
        // Arrange
        val holder = holderOf(backupInfo, secondBackupInfo)

        // Act
        val model = createModel(this, holder)
        advanceUntilIdle()

        // Assert
        val list = model.uiState.value as RestoreCloudBackupUM.BackupList
        assertThat(list.accountEmail).isEqualTo(ACCOUNT_EMAIL)
        model.onDestroy()
    }

    @Test
    fun `GIVEN correct password WHEN restore clicked THEN import invoked with backed-up words`() = runTest {
        // Arrange
        val words = (1..MNEMONIC_WORDS).joinToString(separator = " ") { "word$it" }
        coEvery { restoreCloudBackupUseCase(backupInfo.fileId, any(), any()) } returns
            restoredBackup(mnemonic = words).right()
        val capturedMnemonicString = slot<String>()
        every { mnemonicRepository.generateMnemonic(capture(capturedMnemonicString)) } returns mnemonic
        coEvery {
            hotWalletImporter.import(any(), mnemonic, null, "My Wallet", isSeedPhraseBackedUp = false)
        } returns walletId.right()

        val model = createModel(this, holderOf(backupInfo))
        advanceUntilIdle()

        // Act
        (model.uiState.value as RestoreCloudBackupUM.EnterPassword).onPasswordChange(PASSWORD)
        (model.uiState.value as RestoreCloudBackupUM.EnterPassword).onRestoreClick()
        advanceUntilIdle()

        // Assert
        assertThat(capturedMnemonicString.captured).isEqualTo(words)
        coVerify(exactly = 1) {
            hotWalletImporter.import(any(), mnemonic, null, "My Wallet", isSeedPhraseBackedUp = false)
        }
        coVerify(exactly = 1) { setCloudBackupStateUseCase("011", isBackedUp = true, accountName = ACCOUNT_EMAIL) }
        verify(exactly = 1) { callbacks.onWalletImported(walletId) }
        verify(exactly = 0) { analyticsEventHandler.send(ofType<NoticeMobileWalletLimit>()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN backups listed in an account WHEN restore clicked THEN the backup is read in that account`() = runTest {
        // Arrange
        coEvery { restoreCloudBackupUseCase(backupInfo.fileId, any(), any()) } returns
            CloudBackupError.WrongPassword.left()
        val model = createModel(this, holderOf(backupInfo, accountEmail = OTHER_ACCOUNT_EMAIL))
        advanceUntilIdle()

        // Act
        (model.uiState.value as RestoreCloudBackupUM.EnterPassword).onPasswordChange(PASSWORD)
        (model.uiState.value as RestoreCloudBackupUM.EnterPassword).onRestoreClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) {
            restoreCloudBackupUseCase(fileId = backupInfo.fileId, password = any(), accountName = OTHER_ACCOUNT_EMAIL)
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN multiple hot wallets disabled WHEN restored THEN no account is passed to the read and the state`() =
        runTest {
            // Arrange
            every { hotWalletFeatureToggles.isMultipleHotWalletsEnabled } returns false
            coEvery { restoreCloudBackupUseCase(backupInfo.fileId, any(), any()) } returns
                restoredBackup(mnemonic = "word1 word2").right()
            every { mnemonicRepository.generateMnemonic(any<String>()) } returns mnemonic
            coEvery {
                hotWalletImporter.import(any(), mnemonic, null, "My Wallet", isSeedPhraseBackedUp = false)
            } returns walletId.right()
            val model = createModel(this, holderOf(backupInfo, accountEmail = OTHER_ACCOUNT_EMAIL))
            advanceUntilIdle()

            // Act
            (model.uiState.value as RestoreCloudBackupUM.EnterPassword).onPasswordChange(PASSWORD)
            (model.uiState.value as RestoreCloudBackupUM.EnterPassword).onRestoreClick()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) {
                restoreCloudBackupUseCase(fileId = backupInfo.fileId, password = any(), accountName = null)
            }
            coVerify(exactly = 1) { setCloudBackupStateUseCase("011", isBackedUp = true, accountName = null) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN cloud restore WHEN wallet imported THEN seed phrase is not marked as backed up`() = runTest {
        // Arrange
        val capturedBackedUp = slot<Boolean>()
        coEvery { restoreCloudBackupUseCase(backupInfo.fileId, any(), any()) } returns
            restoredBackup(mnemonic = "word1 word2").right()
        every { mnemonicRepository.generateMnemonic(any<String>()) } returns mnemonic
        coEvery {
            hotWalletImporter.import(any(), mnemonic, null, "My Wallet", capture(capturedBackedUp))
        } returns walletId.right()

        val model = createModel(this, holderOf(backupInfo))
        advanceUntilIdle()

        // Act
        (model.uiState.value as RestoreCloudBackupUM.EnterPassword).onPasswordChange(PASSWORD)
        (model.uiState.value as RestoreCloudBackupUM.EnterPassword).onRestoreClick()
        advanceUntilIdle()

        // Assert
        assertThat(capturedBackedUp.captured).isFalse()
        model.onDestroy()
    }

    @Test
    fun `GIVEN name truncated in the list WHEN restore clicked THEN import uses the name from the backup file`() =
        runTest {
            // Arrange
            val fullName = "Wallet with a very long name"
            coEvery { restoreCloudBackupUseCase(backupInfo.fileId, any(), any()) } returns
                restoredBackup(mnemonic = "word1 word2", walletName = fullName).right()
            every { mnemonicRepository.generateMnemonic(any<String>()) } returns mnemonic
            coEvery {
                hotWalletImporter.import(any(), mnemonic, null, fullName, isSeedPhraseBackedUp = false)
            } returns walletId.right()

            val model = createModel(this, holderOf(backupInfo))
            advanceUntilIdle()

            // Act
            (model.uiState.value as RestoreCloudBackupUM.EnterPassword).onPasswordChange(PASSWORD)
            (model.uiState.value as RestoreCloudBackupUM.EnterPassword).onRestoreClick()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) {
                hotWalletImporter.import(any(), mnemonic, null, fullName, isSeedPhraseBackedUp = false)
            }
            model.onDestroy()
        }

    @Test
    fun `GIVEN passphraseRequired backup WHEN password entered THEN passphrase screen shown AND import waits`() =
        runTest {
            // Arrange
            coEvery { restoreCloudBackupUseCase(backupInfo.fileId, any(), any()) } returns
                restoredBackup(mnemonic = "word1 word2", isPassphraseRequired = true).right()
            every { mnemonicRepository.generateMnemonic(any<String>()) } returns mnemonic

            val model = createModel(this, holderOf(backupInfo))
            advanceUntilIdle()

            // Act
            (model.uiState.value as RestoreCloudBackupUM.EnterPassword).onPasswordChange(PASSWORD)
            (model.uiState.value as RestoreCloudBackupUM.EnterPassword).onRestoreClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value).isInstanceOf(RestoreCloudBackupUM.EnterPassphrase::class.java)
            coVerify(exactly = 0) { hotWalletImporter.import(any(), any(), any(), any(), any(), any()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN passphrase screen WHEN back THEN the restored mnemonic is wiped`() = runTest {
        // Arrange
        val mnemonicChars = "word1 word2".toCharArray()
        coEvery { restoreCloudBackupUseCase(backupInfo.fileId, any(), any()) } returns
            RestoredCloudBackup(
                walletName = "My Wallet",
                secret = CloudBackupSecretData(mnemonic = mnemonicChars, isPassphraseRequired = true),
            ).right()

        val model = createModel(this, holderOf(backupInfo))
        advanceUntilIdle()
        (model.uiState.value as RestoreCloudBackupUM.EnterPassword).onPasswordChange(PASSWORD)
        (model.uiState.value as RestoreCloudBackupUM.EnterPassword).onRestoreClick()
        advanceUntilIdle()

        // Act
        (model.uiState.value as RestoreCloudBackupUM.EnterPassphrase).onBack()
        advanceUntilIdle()

        // Assert
        assertThat(mnemonicChars).isEqualTo(CharArray(mnemonicChars.size) { ' ' })
        model.onDestroy()
    }

    @Test
    fun `GIVEN passphrase screen WHEN passphrase entered THEN import invoked with that passphrase`() = runTest {
        // Arrange
        coEvery { restoreCloudBackupUseCase(backupInfo.fileId, any(), any()) } returns
            restoredBackup(mnemonic = "word1 word2", isPassphraseRequired = true).right()
        every { mnemonicRepository.generateMnemonic(any<String>()) } returns mnemonic
        val capturedPassphrase = slot<CharArray>()
        coEvery {
            hotWalletImporter.import(
                any(),
                mnemonic,
                capture(capturedPassphrase),
                "My Wallet",
                isSeedPhraseBackedUp = false,
            )
        } returns walletId.right()

        val model = createModel(this, holderOf(backupInfo))
        advanceUntilIdle()
        (model.uiState.value as RestoreCloudBackupUM.EnterPassword).onPasswordChange(PASSWORD)
        (model.uiState.value as RestoreCloudBackupUM.EnterPassword).onRestoreClick()
        advanceUntilIdle()

        // Act
        (model.uiState.value as RestoreCloudBackupUM.EnterPassphrase).onPassphraseChange("extra")
        (model.uiState.value as RestoreCloudBackupUM.EnterPassphrase).onContinueClick()
        advanceUntilIdle()

        // Assert
        assertThat(capturedPassphrase.captured).isEqualTo("extra".toCharArray())
        verify(exactly = 1) { callbacks.onWalletImported(walletId) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN wallet already added WHEN restore clicked THEN already-added dialog shown AND no import callback`() =
        runTest {
            // Arrange
            coEvery { restoreCloudBackupUseCase(backupInfo.fileId, any(), any()) } returns
                restoredBackup(mnemonic = "word1 word2").right()
            every { mnemonicRepository.generateMnemonic(any<String>()) } returns mnemonic
            coEvery {
                hotWalletImporter.import(any(), mnemonic, null, "My Wallet", isSeedPhraseBackedUp = false)
            } returns HotWalletImportError.AlreadySaved.left()

            val model = createModel(this, holderOf(backupInfo))
            advanceUntilIdle()

            // Act
            (model.uiState.value as RestoreCloudBackupUM.EnterPassword).onPasswordChange(PASSWORD)
            (model.uiState.value as RestoreCloudBackupUM.EnterPassword).onRestoreClick()
            advanceUntilIdle()

            // Assert
            val dialog = slot<DialogMessage>()
            verify(exactly = 1) { uiMessageSender.send(capture(dialog)) }
            assertThat(dialog.captured.message)
                .isEqualTo(resourceReference(R.string.user_wallet_list_error_wallet_already_saved))
            assertThat(model.uiState.value).isInstanceOf(RestoreCloudBackupUM.EnterPassword::class.java)
            verify(exactly = 0) { callbacks.onWalletImported(any()) }
            verify(exactly = 0) { analyticsEventHandler.send(ofType<NoticeMobileWalletLimit>()) }
            coVerify(exactly = 0) { setCloudBackupStateUseCase(any(), any()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN hot wallets limit reached WHEN restore clicked THEN limit reported AND dialog shown AND no import`() =
        runTest {
            // Arrange
            coEvery { restoreCloudBackupUseCase(backupInfo.fileId, any(), any()) } returns
                restoredBackup(mnemonic = "word1 word2").right()
            every { mnemonicRepository.generateMnemonic(any<String>()) } returns mnemonic
            coEvery {
                hotWalletImporter.import(any(), mnemonic, null, "My Wallet", isSeedPhraseBackedUp = false)
            } returns HotWalletImportError.LimitReached.left()

            val model = createModel(this, holderOf(backupInfo))
            advanceUntilIdle()

            // Act
            (model.uiState.value as RestoreCloudBackupUM.EnterPassword).onPasswordChange(PASSWORD)
            (model.uiState.value as RestoreCloudBackupUM.EnterPassword).onRestoreClick()
            advanceUntilIdle()

            // Assert
            val dialog = slot<DialogMessage>()
            verify(exactly = 1) { uiMessageSender.send(capture(dialog)) }
            assertThat(dialog.captured.message).isEqualTo(Dialogs.hotWalletsLimitReachedDialog().message)
            verify(exactly = 1) {
                analyticsEventHandler.send(
                    NoticeMobileWalletLimit(
                        type = NoticeMobileWalletLimit.Type.Import,
                        limit = NoticeMobileWalletLimit.Limit.MobileWallets,
                    ),
                )
            }
            assertThat(model.uiState.value).isInstanceOf(RestoreCloudBackupUM.EnterPassword::class.java)
            verify(exactly = 0) { callbacks.onWalletImported(any()) }
            coVerify(exactly = 0) { setCloudBackupStateUseCase(any(), any()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN wrong password WHEN restore clicked THEN state stays EnterPassword with error AND no import`() =
        runTest {
            // Arrange
            coEvery { restoreCloudBackupUseCase(backupInfo.fileId, any(), any()) } returns CloudBackupError.WrongPassword.left()

            val model = createModel(this, holderOf(backupInfo))
            advanceUntilIdle()

            // Act
            (model.uiState.value as RestoreCloudBackupUM.EnterPassword).onPasswordChange("wrong-password")
            (model.uiState.value as RestoreCloudBackupUM.EnterPassword).onRestoreClick()
            advanceUntilIdle()

            // Assert
            val state = model.uiState.value
            assertThat(state).isInstanceOf(RestoreCloudBackupUM.EnterPassword::class.java)
            assertThat((state as RestoreCloudBackupUM.EnterPassword).isError).isTrue()
            coVerify(exactly = 0) { hotWalletImporter.import(any(), any(), any(), any(), any(), any()) }
            verify(exactly = 0) { callbacks.onWalletImported(any()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN download fails WHEN restore clicked THEN error dialog shown AND stays on EnterPassword`() = runTest {
        // Arrange
        coEvery { restoreCloudBackupUseCase(backupInfo.fileId, any(), any()) } returns CloudBackupError.ReadError().left()

        val model = createModel(this, holderOf(backupInfo))
        advanceUntilIdle()

        // Act
        (model.uiState.value as RestoreCloudBackupUM.EnterPassword).onPasswordChange(PASSWORD)
        (model.uiState.value as RestoreCloudBackupUM.EnterPassword).onRestoreClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { uiMessageSender.send(any<DialogMessage>()) }
        assertThat(model.uiState.value).isInstanceOf(RestoreCloudBackupUM.EnterPassword::class.java)
        model.onDestroy()
    }

    @Test
    fun `GIVEN no network WHEN restore clicked THEN dialog explains the connection AND stays on EnterPassword`() =
        runTest {
            // Arrange
            coEvery { restoreCloudBackupUseCase(backupInfo.fileId, any(), any()) } returns CloudBackupError.NetworkError.left()

            val model = createModel(this, holderOf(backupInfo))
            advanceUntilIdle()

            // Act
            (model.uiState.value as RestoreCloudBackupUM.EnterPassword).onPasswordChange(PASSWORD)
            (model.uiState.value as RestoreCloudBackupUM.EnterPassword).onRestoreClick()
            advanceUntilIdle()

            // Assert
            val dialog = slot<DialogMessage>()
            verify(exactly = 1) { uiMessageSender.send(capture(dialog)) }
            assertThat(dialog.captured.message)
                .isEqualTo(resourceReference(R.string.hw_cloud_backup_error_network))
            assertThat(model.uiState.value).isInstanceOf(RestoreCloudBackupUM.EnterPassword::class.java)
            model.onDestroy()
        }

    @Test
    fun `GIVEN malformed backup WHEN restore clicked THEN error dialog shown AND stays on EnterPassword`() = runTest {
        // Arrange
        coEvery { restoreCloudBackupUseCase(backupInfo.fileId, any(), any()) } returns CloudBackupError.InvalidBackupFile.left()

        val model = createModel(this, holderOf(backupInfo))
        advanceUntilIdle()

        // Act
        (model.uiState.value as RestoreCloudBackupUM.EnterPassword).onPasswordChange(PASSWORD)
        (model.uiState.value as RestoreCloudBackupUM.EnterPassword).onRestoreClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { uiMessageSender.send(any<DialogMessage>()) }
        assertThat(model.uiState.value).isInstanceOf(RestoreCloudBackupUM.EnterPassword::class.java)
        model.onDestroy()
    }

    @Nested
    inner class AlreadyAddedBackups {

        @Test
        fun `GIVEN one of two backups is in the app WHEN model created THEN list marks only that one`() = runTest {
            // Arrange
            userWallets.value = listOf(walletInApp(id = "012"))

            // Act
            val model = createModel(this, holderOf(backupInfo, secondBackupInfo))
            advanceUntilIdle()

            // Assert
            val list = model.uiState.value as RestoreCloudBackupUM.BackupList
            assertThat(list.items.map { it.walletName to it.isAlreadyAdded })
                .containsExactly("Savings" to true, "My Wallet" to false)
                .inOrder()
            assertThat(list.areAllAdded).isFalse()
            model.onDestroy()
        }

        @Test
        fun `GIVEN added backup row WHEN clicked THEN password step is not opened`() = runTest {
            // Arrange
            userWallets.value = listOf(walletInApp(id = "012"))
            val model = createModel(this, holderOf(backupInfo, secondBackupInfo))
            advanceUntilIdle()
            val list = model.uiState.value as RestoreCloudBackupUM.BackupList

            // Act
            list.items.first { it.isAlreadyAdded }.onClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value).isEqualTo(list)
            verify(exactly = 0) {
                analyticsEventHandler.send(match<OnboardingAnalyticsEvent.Backup.EnterCloudBackupPasswordScreen> { true })
            }
            model.onDestroy()
        }

        @Test
        fun `GIVEN the only backup is in the app WHEN model created THEN list with the mark is shown`() = runTest {
            // Arrange
            userWallets.value = listOf(walletInApp(id = "011"))

            // Act
            val model = createModel(this, holderOf(backupInfo))
            advanceUntilIdle()

            // Assert
            val list = model.uiState.value as RestoreCloudBackupUM.BackupList
            assertThat(list.items.map { it.isAlreadyAdded }).containsExactly(true)
            verify(exactly = 1) {
                analyticsEventHandler.send(
                    OnboardingAnalyticsEvent.Backup.SelectCloudBackupScreen(backupCount = 1),
                )
            }
            verify(exactly = 0) { callbacks.onBack() }
            model.onDestroy()
        }

        @Test
        fun `GIVEN all backups are in the app WHEN model created THEN every row is marked and count is total`() =
            runTest {
                // Arrange
                userWallets.value = listOf(walletInApp(id = "011"), walletInApp(id = "012"))

                // Act
                val model = createModel(this, holderOf(backupInfo, secondBackupInfo))
                advanceUntilIdle()

                // Assert
                val list = model.uiState.value as RestoreCloudBackupUM.BackupList
                assertThat(list.items.map { it.isAlreadyAdded }).containsExactly(true, true)
                assertThat(list.areAllAdded).isTrue()
                verify(exactly = 1) {
                    analyticsEventHandler.send(
                        OnboardingAnalyticsEvent.Backup.SelectCloudBackupScreen(backupCount = 2),
                    )
                }
                model.onDestroy()
            }

        @Test
        fun `GIVEN single backup not in the app WHEN model created THEN password step opens directly`() = runTest {
            // Arrange
            userWallets.value = listOf(walletInApp(id = "999"))

            // Act
            val model = createModel(this, holderOf(backupInfo))
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value).isInstanceOf(RestoreCloudBackupUM.EnterPassword::class.java)
            model.onDestroy()
        }

        @Test
        fun `GIVEN legacy backup without walletId WHEN model created THEN it is not treated as added`() = runTest {
            // Arrange
            userWallets.value = listOf(walletInApp(id = "011"))
            val legacyBackup = backupInfo.copy(walletId = null)

            // Act
            val model = createModel(this, holderOf(legacyBackup))
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value).isInstanceOf(RestoreCloudBackupUM.EnterPassword::class.java)
            model.onDestroy()
        }

        @Test
        fun `GIVEN toggle off AND the only backup is in the app WHEN model created THEN password step opens`() =
            runTest {
                // Arrange
                every { hotWalletFeatureToggles.isMultipleHotWalletsEnabled } returns false
                userWallets.value = listOf(walletInApp(id = "011"))

                // Act
                val model = createModel(this, holderOf(backupInfo))
                advanceUntilIdle()

                // Assert
                assertThat(model.uiState.value).isInstanceOf(RestoreCloudBackupUM.EnterPassword::class.java)
                model.onDestroy()
            }

        @Test
        fun `GIVEN toggle off AND backups are in the app WHEN model created THEN no row is marked`() = runTest {
            // Arrange
            every { hotWalletFeatureToggles.isMultipleHotWalletsEnabled } returns false
            userWallets.value = listOf(walletInApp(id = "011"), walletInApp(id = "012"))

            // Act
            val model = createModel(this, holderOf(backupInfo, secondBackupInfo))
            advanceUntilIdle()

            // Assert
            val list = model.uiState.value as RestoreCloudBackupUM.BackupList
            assertThat(list.items.map { it.isAlreadyAdded }).containsExactly(false, false)
            model.onDestroy()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AnalyticsSource {

        @BeforeEach
        fun resetMocks() {
            clearMocks(hotWalletImporter, restoreCloudBackupUseCase, mnemonicRepository, callbacks)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN params with a source WHEN restore clicked THEN the import reports that source`(
            model: SourceModel,
        ) = runTest {
            // Arrange
            every { paramsContainer.require<RestoreCloudBackupComponent.Params>() } returns model.params(callbacks)
            coEvery { restoreCloudBackupUseCase(backupInfo.fileId, any(), any()) } returns
                restoredBackup(mnemonic = "word1 word2").right()
            every { mnemonicRepository.generateMnemonic(any<String>()) } returns mnemonic
            coEvery {
                hotWalletImporter.import(any(), any(), any(), any(), any(), any(), any())
            } returns walletId.right()
            val restoreModel = createModel(this, holderOf(backupInfo))
            advanceUntilIdle()

            // Act
            (restoreModel.uiState.value as RestoreCloudBackupUM.EnterPassword).onPasswordChange(PASSWORD)
            (restoreModel.uiState.value as RestoreCloudBackupUM.EnterPassword).onRestoreClick()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) {
                hotWalletImporter.import(
                    scope = any(),
                    mnemonic = mnemonic,
                    passphrase = null,
                    name = "My Wallet",
                    isSeedPhraseBackedUp = false,
                    creationType = any(),
                    analyticsSource = model.expectedSource,
                )
            }
            restoreModel.onDestroy()
        }

        private fun provideTestModels() = listOf(
            SourceModel(
                name = "default source",
                params = { RestoreCloudBackupComponent.Params(it) },
                expectedSource = AnalyticsParam.ScreensSources.ImportWallet,
            ),
            SourceModel(
                name = "add new wallet source",
                params = { RestoreCloudBackupComponent.Params(it, AnalyticsParam.ScreensSources.AddNewWallet) },
                expectedSource = AnalyticsParam.ScreensSources.AddNewWallet,
            ),
        )
    }

    internal data class SourceModel(
        val name: String,
        val params: (RestoreCloudBackupComponent.ModelCallbacks) -> RestoreCloudBackupComponent.Params,
        val expectedSource: AnalyticsParam.ScreensSources,
    ) {
        override fun toString(): String = name
    }

    private fun restoredBackup(
        mnemonic: String,
        isPassphraseRequired: Boolean = false,
        walletName: String = "My Wallet",
    ): RestoredCloudBackup = RestoredCloudBackup(
        walletName = walletName,
        secret = CloudBackupSecretData(mnemonic = mnemonic.toCharArray(), isPassphraseRequired = isPassphraseRequired),
    )

    private fun holderOf(vararg backups: CloudBackupInfo, accountEmail: String? = ACCOUNT_EMAIL): CloudRestoreResultHolder {
        val holder = CloudRestoreResultHolder()
        if (backups.isNotEmpty()) {
            holder.set(CloudRestoreResult(backups = backups.toList(), accountEmail = accountEmail))
        }
        return holder
    }

    private fun createModel(testScope: TestScope, holder: CloudRestoreResultHolder): RestoreCloudBackupModel {
        return RestoreCloudBackupModel(
            paramsContainer = paramsContainer,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            cloudRestoreResultHolder = holder,
            restoreCloudBackupUseCase = restoreCloudBackupUseCase,
            setCloudBackupStateUseCase = setCloudBackupStateUseCase,
            hotWalletFeatureToggles = hotWalletFeatureToggles,
            mnemonicRepository = mnemonicRepository,
            hotWalletImporter = hotWalletImporter,
            uiMessageSender = uiMessageSender,
            analyticsEventHandler = analyticsEventHandler,
            getBackupPasswordUseCase = getBackupPasswordUseCase,
            userWalletsListRepository = userWalletsListRepository,
        )
    }

    private fun walletInApp(id: String): UserWallet = mockk { every { walletId } returns UserWalletId(id) }

    @Test
    fun `GIVEN wrong password WHEN restore clicked THEN wrong password event sent`() = runTest {
        // Arrange
        coEvery { restoreCloudBackupUseCase(backupInfo.fileId, any(), any()) } returns CloudBackupError.WrongPassword.left()
        val model = createModel(this, holderOf(backupInfo))
        advanceUntilIdle()

        // Act
        (model.uiState.value as RestoreCloudBackupUM.EnterPassword).onPasswordChange("wrong-password")
        (model.uiState.value as RestoreCloudBackupUM.EnterPassword).onRestoreClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            analyticsEventHandler.send(match<OnboardingAnalyticsEvent.Backup.WrongCloudBackupPassword> { true })
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN download fails WHEN restore clicked THEN import error event sent with message`() = runTest {
        // Arrange
        coEvery { restoreCloudBackupUseCase(backupInfo.fileId, any(), any()) } returns CloudBackupError.ReadError().left()
        val model = createModel(this, holderOf(backupInfo))
        advanceUntilIdle()

        // Act
        (model.uiState.value as RestoreCloudBackupUM.EnterPassword).onPasswordChange(PASSWORD)
        (model.uiState.value as RestoreCloudBackupUM.EnterPassword).onRestoreClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            analyticsEventHandler.send(
                match<OnboardingAnalyticsEvent.Backup.ImportCloudBackupError> { it.params["Error Message"] == "Read error" },
            )
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN single backup WHEN model created THEN enter password screen event sent`() = runTest {
        // Act
        val model = createModel(this, holderOf(backupInfo))
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            analyticsEventHandler.send(match<OnboardingAnalyticsEvent.Backup.EnterCloudBackupPasswordScreen> { true })
        }
        model.onDestroy()
    }

    private companion object {
        const val PASSWORD = "Str0ng!Pass"
        const val MNEMONIC_WORDS = 12
        const val ACCOUNT_EMAIL = "user@gmail.com"
        const val OTHER_ACCOUNT_EMAIL = "other@gmail.com"
    }
}