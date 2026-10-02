package com.tangem.data.sociallogin.store

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.error.SaveWalletError
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class UserWalletSocialBackupStoreTest {

    private val userWalletsListRepository: UserWalletsListRepository = mockk()

    private val store = UserWalletSocialBackupStore(userWalletsListRepositoryLazy = { userWalletsListRepository })

    @BeforeEach
    fun resetMocks() {
        clearMocks(userWalletsListRepository)
        val wallets = listOf(MARKED_WALLET, UNMARKED_WALLET, COLD_WALLET)
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(wallets)
        coEvery { userWalletsListRepository.userWalletsSync() } returns wallets
        coEvery { userWalletsListRepository.saveWithoutLock(any(), any()) } answers { firstArg<UserWallet>().right() }
    }

    @Test
    fun `GIVEN listed wallets WHEN getCompletedWalletIds THEN only the marked hot wallets are reported`() = runTest {
        // Act
        val actual = store.getCompletedWalletIds().first()

        // Assert
        assertThat(actual).containsExactly(MARKED_WALLET.walletId.stringValue)
    }

    @Test
    fun `GIVEN a listed wallet WHEN setCompleted THEN its record is saved with the mark`() = runTest {
        // Act
        store.setCompleted(walletId = UNMARKED_WALLET.walletId.stringValue, isCompleted = true)

        // Assert
        coVerify(exactly = 1) {
            userWalletsListRepository.saveWithoutLock(UNMARKED_WALLET.copy(isSocialBackupCompleted = true), true)
        }
    }

    @Test
    fun `GIVEN a wallet not on the device WHEN setCompleted THEN nothing is saved`() = runTest {
        // Act
        store.setCompleted(walletId = "0000", isCompleted = false)

        // Assert
        coVerify(exactly = 0) { userWalletsListRepository.saveWithoutLock(any(), any()) }
    }

    @Test
    fun `GIVEN the mark already matches WHEN setCompleted THEN nothing is saved`() = runTest {
        // Act
        store.setCompleted(walletId = MARKED_WALLET.walletId.stringValue, isCompleted = true)

        // Assert
        coVerify(exactly = 0) { userWalletsListRepository.saveWithoutLock(any(), any()) }
    }

    @Test
    fun `GIVEN saving fails WHEN setCompleted THEN it throws`() = runTest {
        // Arrange
        coEvery { userWalletsListRepository.saveWithoutLock(any(), any()) } returns
            SaveWalletError.DataError(messageId = null).left()

        // Act
        val actual = runCatching {
            store.setCompleted(walletId = MARKED_WALLET.walletId.stringValue, isCompleted = false)
        }.exceptionOrNull()

        // Assert
        assertThat(actual).isInstanceOf(IllegalStateException::class.java)
    }

    private companion object {
        val MARKED_WALLET = hotWallet(id = "0A0A", isSocialBackupCompleted = true)
        val UNMARKED_WALLET = hotWallet(id = "0B0B", isSocialBackupCompleted = false)
        val COLD_WALLET: UserWallet.Cold = mockk {
            every { walletId } returns UserWalletId(stringValue = "0C0C")
        }

        fun hotWallet(id: String, isSocialBackupCompleted: Boolean) = UserWallet.Hot(
            name = "Wallet $id",
            walletId = UserWalletId(stringValue = id),
            hotWalletId = mockk(),
            wallets = null,
            backedUp = false,
            isSocialBackupCompleted = isSocialBackupCompleted,
        )
    }
}