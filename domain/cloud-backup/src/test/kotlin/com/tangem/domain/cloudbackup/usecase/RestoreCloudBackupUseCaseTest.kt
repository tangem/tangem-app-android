package com.tangem.domain.cloudbackup.usecase

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.cloudbackup.models.CloudBackupSecretData
import com.tangem.domain.cloudbackup.models.RestoredCloudBackup
import com.tangem.domain.cloudbackup.repository.CloudBackupRepository
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class RestoreCloudBackupUseCaseTest {

    private val cloudBackupRepository: CloudBackupRepository = mockk()
    private val useCase = RestoreCloudBackupUseCase(cloudBackupRepository)

    private var accountInUse: String? = null

    @BeforeEach
    fun resetMocks() {
        clearMocks(cloudBackupRepository)
        accountInUse = null
        coEvery { cloudBackupRepository.withAccount<Any?>(any(), any()) } coAnswers {
            accountInUse = firstArg()
            secondArg<suspend () -> Any?>().invoke().also { accountInUse = null }
        }
    }

    @Test
    fun `GIVEN the account the backup was listed in WHEN invoke THEN it is read in that account`() = runTest {
        // Arrange
        var readIn: String? = null
        coEvery { cloudBackupRepository.readBackup(FILE_ID, any()) } answers {
            readIn = accountInUse
            RESTORED.right()
        }

        // Act
        val actual = useCase(fileId = FILE_ID, password = PASSWORD.toCharArray(), accountName = ACCOUNT_NAME)

        // Assert
        assertThat(actual).isEqualTo(RESTORED.right())
        assertThat(readIn).isEqualTo(ACCOUNT_NAME)
    }

    @Test
    fun `GIVEN no account WHEN invoke THEN it is read in the current account`() = runTest {
        // Arrange
        coEvery { cloudBackupRepository.readBackup(FILE_ID, any()) } returns RESTORED.right()

        // Act
        val actual = useCase(fileId = FILE_ID, password = PASSWORD.toCharArray())

        // Assert
        assertThat(actual).isEqualTo(RESTORED.right())
        coVerify(exactly = 0) { cloudBackupRepository.withAccount<Any?>(any(), any()) }
    }

    private companion object {
        const val FILE_ID = "file-1"
        const val PASSWORD = "Str0ng!Pass"
        const val ACCOUNT_NAME = "user@gmail.com"
        val RESTORED = RestoredCloudBackup(
            walletName = "My Wallet",
            secret = CloudBackupSecretData(mnemonic = "word1 word2".toCharArray(), isPassphraseRequired = false),
        )
    }
}