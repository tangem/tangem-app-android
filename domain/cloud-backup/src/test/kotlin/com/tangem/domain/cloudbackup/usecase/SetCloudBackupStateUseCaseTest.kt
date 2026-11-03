package com.tangem.domain.cloudbackup.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.cloudbackup.repository.CloudBackupRepository
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SetCloudBackupStateUseCaseTest {

    private val cloudBackupRepository: CloudBackupRepository = mockk()
    private val useCase = SetCloudBackupStateUseCase(cloudBackupRepository)

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
    fun `GIVEN the account of the backup WHEN invoke THEN the state is set in that account`() = runTest {
        // Arrange
        var setIn: String? = null
        coEvery { cloudBackupRepository.setBackedUp(WALLET_ID, backedUp = true) } answers { setIn = accountInUse }

        // Act
        useCase(walletId = WALLET_ID, isBackedUp = true, accountName = ACCOUNT_NAME)

        // Assert
        assertThat(setIn).isEqualTo(ACCOUNT_NAME)
    }

    @Test
    fun `GIVEN no account WHEN invoke THEN the state is set in the current account`() = runTest {
        // Arrange
        coEvery { cloudBackupRepository.setBackedUp(WALLET_ID, backedUp = false) } just Runs

        // Act
        useCase(walletId = WALLET_ID, isBackedUp = false)

        // Assert
        coVerify(exactly = 1) { cloudBackupRepository.setBackedUp(WALLET_ID, backedUp = false) }
        coVerify(exactly = 0) { cloudBackupRepository.withAccount<Any?>(any(), any()) }
    }

    private companion object {
        const val WALLET_ID = "011"
        const val ACCOUNT_NAME = "user@gmail.com"
    }
}