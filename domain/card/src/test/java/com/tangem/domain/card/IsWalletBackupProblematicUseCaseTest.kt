package com.tangem.domain.card

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWallet
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IsWalletBackupProblematicUseCaseTest {

    private val backupValidator: BackupValidator = mockk()

    private val useCase = IsWalletBackupProblematicUseCase(backupValidator = backupValidator)

    @BeforeEach
    fun setup() {
        clearMocks(backupValidator)
    }

    @Test
    fun `returns false for hot wallet`() {
        val wallet = mockk<UserWallet.Hot>()

        assertThat(useCase(wallet)).isFalse()
    }

    @Test
    fun `returns true for cold wallet with persisted backup error`() {
        val wallet = coldWallet(hasBackupError = true)
        every { backupValidator.isValidBackupStatus(any()) } returns true

        assertThat(useCase(wallet)).isTrue()
    }

    @Test
    fun `returns true for cold wallet with invalid backup status`() {
        val wallet = coldWallet(hasBackupError = false)
        every { backupValidator.isValidBackupStatus(any()) } returns false

        assertThat(useCase(wallet)).isTrue()
    }

    @Test
    fun `returns false for cold wallet without backup error and valid backup status`() {
        val wallet = coldWallet(hasBackupError = false)
        every { backupValidator.isValidBackupStatus(any()) } returns true

        assertThat(useCase(wallet)).isFalse()
    }

    private fun coldWallet(hasBackupError: Boolean): UserWallet.Cold = mockk {
        every { this@mockk.hasBackupError } returns hasBackupError
        every { scanResponse } returns mockk {
            every { card } returns mockk()
        }
    }
}