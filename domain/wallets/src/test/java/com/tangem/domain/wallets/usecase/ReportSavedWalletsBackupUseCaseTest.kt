package com.tangem.domain.wallets.usecase

import arrow.core.right
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class ReportSavedWalletsBackupUseCaseTest {

    private val userWallets = MutableStateFlow<List<UserWallet>?>(null)
    private val userWalletsListRepository: UserWalletsListRepository = mockk {
        every { this@mockk.userWallets } returns this@ReportSavedWalletsBackupUseCaseTest.userWallets
    }
    private val reportSeenWalletCardUseCase: ReportSeenWalletCardUseCase = mockk()

    private val useCase = ReportSavedWalletsBackupUseCase(
        userWalletsListRepository = userWalletsListRepository,
        reportSeenWalletCardUseCase = reportSeenWalletCardUseCase,
    )

    @Test
    fun `GIVEN saved wallets WHEN emitted THEN every unlocked cold wallet is reported as saved`() = runTest {
        // Arrange
        coEvery { reportSeenWalletCardUseCase(any(), any()) } returns Unit.right()
        val unlocked = coldWallet(isLocked = false)
        val locked = coldWallet(isLocked = true)
        backgroundScope.launch { useCase() }

        // Act
        userWallets.value = listOf(unlocked, locked, mockk<UserWallet.Hot>())
        runCurrent()

        // Assert
        coVerify(exactly = 1) { reportSeenWalletCardUseCase(unlocked, isNewWallet = false) }
        coVerify(exactly = 0) { reportSeenWalletCardUseCase(locked, any()) }
    }

    @Test
    fun `GIVEN locked wallet WHEN it is unlocked THEN it is reported`() = runTest {
        // Arrange
        coEvery { reportSeenWalletCardUseCase(any(), any()) } returns Unit.right()
        val unlocked = coldWallet(isLocked = false)
        backgroundScope.launch { useCase() }

        // Act
        userWallets.value = listOf(coldWallet(isLocked = true))
        runCurrent()
        userWallets.value = listOf(unlocked)
        runCurrent()

        // Assert
        coVerify(exactly = 1) { reportSeenWalletCardUseCase(any(), any()) }
        coVerify(exactly = 1) { reportSeenWalletCardUseCase(unlocked, isNewWallet = false) }
    }

    private fun coldWallet(isLocked: Boolean): UserWallet.Cold = mockk {
        every { this@mockk.isLocked } returns isLocked
    }
}