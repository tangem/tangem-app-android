package com.tangem.data.collectibles.account.cleaner

import com.tangem.domain.collectibles.delegate.CollectiblesHotDelegate
import com.tangem.domain.collectibles.account.repository.CollectiblesAccountRepository
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class CollectiblesUserWalletDataCleanerTest {

    private val accountRepository: CollectiblesAccountRepository = mockk(relaxUnitFun = true)
    private val hotDelegate: CollectiblesHotDelegate = mockk(relaxUnitFun = true)

    private val cleaner = CollectiblesUserWalletDataCleaner(
        accountRepository = accountRepository,
        hotDelegate = hotDelegate,
    )

    private val walletIds = listOf(UserWalletId("011"), UserWalletId("022"))

    @Test
    fun `GIVEN deleted wallets WHEN clear THEN their account record and delegate are removed`() = runTest {
        // Act
        cleaner.clear(walletIds)

        // Assert
        coVerify(exactly = 1) {
            accountRepository.clearAccount(walletIds[0])
            accountRepository.clearAccount(walletIds[1])
            hotDelegate.forget(AccountId.forCollectiblesAccount(userWalletId = walletIds[0]))
            hotDelegate.forget(AccountId.forCollectiblesAccount(userWalletId = walletIds[1]))
        }
    }

    @Test
    fun `GIVEN the record fails for one wallet WHEN clear THEN its delegate and the other wallet are still cleared`() =
        runTest {
            // Arrange
            coEvery { accountRepository.clearAccount(walletIds[0]) } throws IllegalStateException("disk")

            // Act
            cleaner.clear(walletIds)

            // Assert
            coVerify(exactly = 1) {
                hotDelegate.forget(AccountId.forCollectiblesAccount(userWalletId = walletIds[0]))
                accountRepository.clearAccount(walletIds[1])
            }
        }
}