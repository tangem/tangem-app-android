package com.tangem.data.jointaccount.cleaner

import com.tangem.data.jointaccount.store.JointAccountsStore
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class JointAccountUserWalletDataCleanerTest {

    private val accountsStore: JointAccountsStore = mockk(relaxUnitFun = true)

    private val cleaner = JointAccountUserWalletDataCleaner(accountsStore = accountsStore)

    private val walletIds = listOf(UserWalletId("011"), UserWalletId("022"))

    @Test
    fun `GIVEN deleted wallets WHEN clear THEN their joint accounts are removed`() = runTest {
        // Act
        cleaner.clear(walletIds)

        // Assert
        coVerify(exactly = 1) { accountsStore.remove(walletIds) }
    }
}