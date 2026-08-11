package com.tangem.features.hotwallet.restorecloudbackup.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.crypto.bip39.Mnemonic
import com.tangem.domain.cloudbackup.models.CloudBackupError
import com.tangem.domain.cloudbackup.models.CloudBackupInfo
import com.tangem.domain.cloudbackup.models.CloudBackupSecretData
import com.tangem.domain.cloudbackup.usecase.RestoreCloudBackupUseCase
import com.tangem.domain.cloudbackup.usecase.SetCloudBackupStateUseCase
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.hotwallet.MnemonicRepository
import com.tangem.features.hotwallet.addexistingwallet.im.port.model.HotWalletImporter
import com.tangem.features.hotwallet.restorecloudbackup.CloudRestoreResult
import com.tangem.features.hotwallet.restorecloudbackup.CloudRestoreResultHolder
import com.tangem.features.hotwallet.restorecloudbackup.RestoreCloudBackupComponent
import com.tangem.features.hotwallet.restorecloudbackup.entity.RestoreCloudBackupUM
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class RestoreCloudBackupModelTest {

    private val restoreCloudBackupUseCase: RestoreCloudBackupUseCase = mockk()
    private val setCloudBackupStateUseCase: SetCloudBackupStateUseCase = mockk(relaxed = true)
    private val mnemonicRepository: MnemonicRepository = mockk()
    private val hotWalletImporter: HotWalletImporter = mockk()
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val callbacks: RestoreCloudBackupComponent.ModelCallbacks = mockk(relaxed = true)
    private val paramsContainer: ParamsContainer = mockk()

    private val walletId = UserWalletId("011")
    private val mnemonic: Mnemonic = mockk()

    private val backupInfo = CloudBackupInfo(
        fileId = "file-1",
        walletName = "My Wallet",
        createdAtMillis = 0L,
        walletId = "011",
    )
    private val secondBackupInfo = CloudBackupInfo(
        fileId = "file-2",
        walletName = "Savings",
        createdAtMillis = 1_000L,
        walletId = "012",
    )

    @BeforeEach
    fun setUp() {
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
        coEvery { restoreCloudBackupUseCase(backupInfo.fileId, any()) } returns
            CloudBackupSecretData(mnemonic = words.toCharArray(), isPassphraseRequired = false).right()
        val capturedMnemonicString = slot<String>()
        every { mnemonicRepository.generateMnemonic(capture(capturedMnemonicString)) } returns mnemonic
        coEvery {
            hotWalletImporter.import(any(), mnemonic, null, "My Wallet")
        } returns HotWalletImporter.Result.Success(walletId)

        val model = createModel(this, holderOf(backupInfo))
        advanceUntilIdle()

        // Act
        (model.uiState.value as RestoreCloudBackupUM.EnterPassword).onPasswordChange(PASSWORD)
        (model.uiState.value as RestoreCloudBackupUM.EnterPassword).onRestoreClick()
        advanceUntilIdle()

        // Assert
        assertThat(capturedMnemonicString.captured).isEqualTo(words)
        coVerify(exactly = 1) { hotWalletImporter.import(any(), mnemonic, null, "My Wallet") }
        coVerify(exactly = 1) { setCloudBackupStateUseCase("011", isBackedUp = true) }
        verify(exactly = 1) { callbacks.onWalletImported(walletId) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN passphraseRequired backup WHEN password entered THEN passphrase screen shown AND import waits`() =
        runTest {
            // Arrange
            coEvery { restoreCloudBackupUseCase(backupInfo.fileId, any()) } returns
                CloudBackupSecretData(mnemonic = "word1 word2".toCharArray(), isPassphraseRequired = true).right()
            every { mnemonicRepository.generateMnemonic(any<String>()) } returns mnemonic

            val model = createModel(this, holderOf(backupInfo))
            advanceUntilIdle()

            // Act
            (model.uiState.value as RestoreCloudBackupUM.EnterPassword).onPasswordChange(PASSWORD)
            (model.uiState.value as RestoreCloudBackupUM.EnterPassword).onRestoreClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value).isInstanceOf(RestoreCloudBackupUM.EnterPassphrase::class.java)
            coVerify(exactly = 0) { hotWalletImporter.import(any(), any(), any(), any()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN passphrase screen WHEN back THEN the restored mnemonic is wiped`() = runTest {
        // Arrange
        val mnemonicChars = "word1 word2".toCharArray()
        coEvery { restoreCloudBackupUseCase(backupInfo.fileId, any()) } returns
            CloudBackupSecretData(mnemonic = mnemonicChars, isPassphraseRequired = true).right()

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
        coEvery { restoreCloudBackupUseCase(backupInfo.fileId, any()) } returns
            CloudBackupSecretData(mnemonic = "word1 word2".toCharArray(), isPassphraseRequired = true).right()
        every { mnemonicRepository.generateMnemonic(any<String>()) } returns mnemonic
        val capturedPassphrase = slot<CharArray>()
        coEvery {
            hotWalletImporter.import(any(), mnemonic, capture(capturedPassphrase), "My Wallet")
        } returns HotWalletImporter.Result.Success(walletId)

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
            coEvery { restoreCloudBackupUseCase(backupInfo.fileId, any()) } returns
                CloudBackupSecretData(mnemonic = "word1 word2".toCharArray(), isPassphraseRequired = false).right()
            every { mnemonicRepository.generateMnemonic(any<String>()) } returns mnemonic
            coEvery {
                hotWalletImporter.import(any(), mnemonic, null, "My Wallet")
            } returns HotWalletImporter.Result.AlreadySaved

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
            coVerify(exactly = 0) { setCloudBackupStateUseCase(any(), any()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN wrong password WHEN restore clicked THEN state stays EnterPassword with error AND no import`() =
        runTest {
            // Arrange
            coEvery { restoreCloudBackupUseCase(backupInfo.fileId, any()) } returns CloudBackupError.WrongPassword.left()

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
            coVerify(exactly = 0) { hotWalletImporter.import(any(), any(), any(), any()) }
            verify(exactly = 0) { callbacks.onWalletImported(any()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN download fails WHEN restore clicked THEN error dialog shown AND stays on EnterPassword`() = runTest {
        // Arrange
        coEvery { restoreCloudBackupUseCase(backupInfo.fileId, any()) } returns CloudBackupError.ReadError().left()

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
    fun `GIVEN malformed backup WHEN restore clicked THEN error dialog shown AND stays on EnterPassword`() = runTest {
        // Arrange
        coEvery { restoreCloudBackupUseCase(backupInfo.fileId, any()) } returns CloudBackupError.InvalidBackupFile.left()

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
            mnemonicRepository = mnemonicRepository,
            hotWalletImporter = hotWalletImporter,
            uiMessageSender = uiMessageSender,
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
        const val PASSWORD = "Str0ng!Pass"
        const val MNEMONIC_WORDS = 12
        const val ACCOUNT_EMAIL = "user@gmail.com"
    }
}