package com.tangem.features.hotwallet.createmobilewallet.importoptions.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.cloudbackup.models.CloudBackupAccount
import com.tangem.domain.cloudbackup.models.CloudBackupError
import com.tangem.domain.cloudbackup.models.CloudBackupInfo
import com.tangem.domain.cloudbackup.repository.CloudBackupRepository
import com.tangem.features.hotwallet.createmobilewallet.importoptions.ImportOptionsBottomSheetComponent
import com.tangem.features.hotwallet.createmobilewallet.importoptions.entity.ImportOptionsBottomSheetUM
import com.tangem.features.hotwallet.restorecloudbackup.CloudRestoreResult
import com.tangem.features.hotwallet.restorecloudbackup.CloudRestoreResultHolder
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ImportOptionsBottomSheetModelTest {

    private val cloudBackupRepository: CloudBackupRepository = mockk()
    private val onRecoveryPhrase: () -> Unit = mockk(relaxed = true)
    private val onCloudBackupsResolved: (CloudRestoreResult) -> Unit = mockk(relaxed = true)
    private val onDismiss: () -> Unit = mockk(relaxed = true)
    private val paramsContainer: ParamsContainer = mockk()

    private val backupInfo = CloudBackupInfo(
        fileId = "file-1",
        walletName = "My Wallet",
        createdAtMillis = 0L,
        walletId = "011",
    )

    @BeforeEach
    fun setUp() {
        clearMocks(cloudBackupRepository, onRecoveryPhrase, onCloudBackupsResolved, onDismiss)
        every { paramsContainer.require<ImportOptionsBottomSheetComponent.Params>() } returns
            ImportOptionsBottomSheetComponent.Params(
                onRecoveryPhrase = onRecoveryPhrase,
                onCloudBackupsResolved = onCloudBackupsResolved,
                onDismiss = onDismiss,
            )
        coEvery { cloudBackupRepository.signOut() } returns Unit
        coEvery { cloudBackupRepository.getAccountInfo(any()) } returns account.right()
    }

    @Test
    fun `GIVEN recovery phrase option WHEN clicked THEN callback invoked`() = runTest {
        // Arrange
        val holder = CloudRestoreResultHolder()
        val model = createModel(this, holder)

        // Act
        model.options().onRecoveryPhraseClick()

        // Assert
        verify(exactly = 1) { onRecoveryPhrase() }
        coVerify(exactly = 0) { cloudBackupRepository.findBackups(any()) }
    }

    @Test
    fun `GIVEN backups found WHEN cloud backup clicked THEN result is held and reported`() = runTest {
        // Arrange
        coEvery { cloudBackupRepository.findBackups(interactive = true) } returns listOf(backupInfo).right()
        val holder = CloudRestoreResultHolder()
        val model = createModel(this, holder)

        // Act
        model.options().onCloudBackupClick()
        advanceUntilIdle()

        // Assert
        val expected = CloudRestoreResult(backups = listOf(backupInfo), accountEmail = ACCOUNT_EMAIL)
        assertThat(holder.result.value).isEqualTo(expected)
        verify(exactly = 1) { onCloudBackupsResolved(expected) }
    }

    @Test
    fun `GIVEN authorized account WHEN cloud backup clicked THEN signs out before listing backups`() = runTest {
        // Arrange
        coEvery { cloudBackupRepository.findBackups(interactive = true) } returns listOf(backupInfo).right()
        val model = createModel(this, CloudRestoreResultHolder())

        // Act
        model.options().onCloudBackupClick()
        advanceUntilIdle()

        // Assert
        coVerifyOrder {
            cloudBackupRepository.signOut()
            cloudBackupRepository.findBackups(interactive = true)
        }
    }

    @Test
    fun `GIVEN account info unavailable WHEN cloud backup clicked THEN result has no email`() = runTest {
        // Arrange
        coEvery { cloudBackupRepository.findBackups(interactive = true) } returns listOf(backupInfo).right()
        coEvery { cloudBackupRepository.getAccountInfo(any()) } returns CloudBackupError.AuthRequired.left()
        val holder = CloudRestoreResultHolder()
        val model = createModel(this, holder)

        // Act
        model.options().onCloudBackupClick()
        advanceUntilIdle()

        // Assert
        assertThat(holder.result.value)
            .isEqualTo(CloudRestoreResult(backups = listOf(backupInfo), accountEmail = null))
    }

    @Test
    fun `GIVEN no backups WHEN cloud backup clicked THEN warning error shown`() = runTest {
        // Arrange
        coEvery { cloudBackupRepository.findBackups(interactive = true) } returns emptyList<CloudBackupInfo>().right()
        val model = createModel(this, CloudRestoreResultHolder())

        // Act
        model.options().onCloudBackupClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.error().isWarning).isTrue()
        verify(exactly = 0) { onCloudBackupsResolved(any()) }
    }

    @Test
    fun `GIVEN missing permissions WHEN cloud backup clicked THEN non-warning error shown`() = runTest {
        // Arrange
        coEvery { cloudBackupRepository.findBackups(interactive = true) } returns
            CloudBackupError.AuthPermissionsMissing.left()
        val model = createModel(this, CloudRestoreResultHolder())

        // Act
        model.options().onCloudBackupClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.error().isWarning).isFalse()
    }

    @Test
    fun `GIVEN read error WHEN cloud backup clicked THEN non-warning error shown`() = runTest {
        // Arrange
        coEvery { cloudBackupRepository.findBackups(interactive = true) } returns CloudBackupError.ReadError().left()
        val model = createModel(this, CloudRestoreResultHolder())

        // Act
        model.options().onCloudBackupClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.error().isWarning).isFalse()
    }

    @Test
    fun `GIVEN authorization cancelled WHEN cloud backup clicked THEN options shown without loading`() = runTest {
        // Arrange
        coEvery { cloudBackupRepository.findBackups(interactive = true) } returns CloudBackupError.AuthCanceled.left()
        val model = createModel(this, CloudRestoreResultHolder())

        // Act
        model.options().onCloudBackupClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.options().isCloudLoading).isFalse()
        verify(exactly = 0) { onDismiss() }
    }

    @Test
    fun `GIVEN backups are loading WHEN options clicked again THEN clicks are ignored`() = runTest {
        // Arrange
        coEvery { cloudBackupRepository.findBackups(interactive = true) } returns listOf(backupInfo).right()
        val model = createModel(this, CloudRestoreResultHolder())
        model.options().onCloudBackupClick()

        // Act
        model.options().onRecoveryPhraseClick()
        model.options().onCloudBackupClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.options().isCloudLoading).isTrue()
        verify(exactly = 0) { onRecoveryPhrase() }
        coVerify(exactly = 1) { cloudBackupRepository.findBackups(interactive = true) }
    }

    private fun ImportOptionsBottomSheetModel.options(): ImportOptionsBottomSheetUM.Content.Options =
        uiState.value.content as ImportOptionsBottomSheetUM.Content.Options

    private fun ImportOptionsBottomSheetModel.error(): ImportOptionsBottomSheetUM.Content.Error =
        uiState.value.content as ImportOptionsBottomSheetUM.Content.Error

    private fun createModel(testScope: TestScope, holder: CloudRestoreResultHolder) = ImportOptionsBottomSheetModel(
        paramsContainer = paramsContainer,
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        cloudBackupRepository = cloudBackupRepository,
        cloudRestoreResultHolder = holder,
    )

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
        const val ACCOUNT_EMAIL = "user@gmail.com"
        val account = CloudBackupAccount(email = ACCOUNT_EMAIL, displayName = null, photoUrl = null)
    }
}