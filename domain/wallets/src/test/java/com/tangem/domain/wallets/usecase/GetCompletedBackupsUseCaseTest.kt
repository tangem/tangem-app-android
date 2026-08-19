package com.tangem.domain.wallets.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.cloudbackup.repository.CloudBackupRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class GetCompletedBackupsUseCaseTest {

    private val cloudBackupRepository: CloudBackupRepository = mockk()

    private val useCase = GetCompletedBackupsUseCase(cloudBackupRepository)

    @BeforeEach
    fun setUp() {
        every { cloudBackupRepository.isCloudBackupEnabled } returns true
    }

    @Test
    fun `GIVEN hot wallet with both backups WHEN invoke THEN both types reported`() = runTest {
        // Arrange
        coEvery { cloudBackupRepository.isBackedUp(WALLET_ID) } returns true

        // Act
        val actual = useCase(hotWallet(backedUp = true))

        // Assert
        assertThat(actual)
            .containsExactly(AnalyticsParam.BackupType.Manual, AnalyticsParam.BackupType.Cloud)
    }

    @Test
    fun `GIVEN hot wallet with manual backup only WHEN invoke THEN Manual reported`() = runTest {
        // Arrange
        coEvery { cloudBackupRepository.isBackedUp(WALLET_ID) } returns false

        // Act
        val actual = useCase(hotWallet(backedUp = true))

        // Assert
        assertThat(actual).containsExactly(AnalyticsParam.BackupType.Manual)
    }

    @Test
    fun `GIVEN hot wallet with cloud backup only WHEN invoke THEN Cloud reported`() = runTest {
        // Arrange
        coEvery { cloudBackupRepository.isBackedUp(WALLET_ID) } returns true

        // Act
        val actual = useCase(hotWallet(backedUp = false))

        // Assert
        assertThat(actual).containsExactly(AnalyticsParam.BackupType.Cloud)
    }

    @Test
    fun `GIVEN hot wallet without backups WHEN invoke THEN nothing reported`() = runTest {
        // Arrange
        coEvery { cloudBackupRepository.isBackedUp(WALLET_ID) } returns false

        // Act
        val actual = useCase(hotWallet(backedUp = false))

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN cloud backup disabled WHEN invoke THEN nothing reported AND cloud is not queried`() = runTest {
        // Arrange
        every { cloudBackupRepository.isCloudBackupEnabled } returns false

        // Act
        val actual = useCase(hotWallet(backedUp = true))

        // Assert
        assertThat(actual).isEmpty()
        coVerify(exactly = 0) { cloudBackupRepository.isBackedUp(any()) }
    }

    @Test
    fun `GIVEN cold wallet WHEN invoke THEN nothing reported AND cloud is not queried`() = runTest {
        // Act
        val actual = useCase(mockk<UserWallet.Cold>())

        // Assert
        assertThat(actual).isEmpty()
        coVerify(exactly = 0) { cloudBackupRepository.isBackedUp(any()) }
    }

    private fun hotWallet(backedUp: Boolean): UserWallet.Hot = UserWallet.Hot(
        name = "Hot",
        walletId = UserWalletId(WALLET_ID),
        hotWalletId = mockk(relaxed = true),
        wallets = null,
        backedUp = backedUp,
    )

    private companion object {
        const val WALLET_ID = "0A0B0C0D"
    }
}