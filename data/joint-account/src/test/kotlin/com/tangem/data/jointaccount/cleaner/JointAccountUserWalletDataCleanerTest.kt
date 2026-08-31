package com.tangem.data.jointaccount.cleaner

import com.tangem.data.jointaccount.store.JointAccountsStore
import com.tangem.domain.jointaccount.store.JointAccountInvitesStore
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class JointAccountUserWalletDataCleanerTest {

    private val invitesStore: JointAccountInvitesStore = mockk(relaxUnitFun = true)
    private val accountsStore: JointAccountsStore = mockk(relaxUnitFun = true)

    private val cleaner = JointAccountUserWalletDataCleaner(invitesStore = invitesStore, accountsStore = accountsStore)

    private val walletIds = listOf(UserWalletId("011"), UserWalletId("022"))

    @Test
    fun `GIVEN deleted wallets WHEN clear THEN their joint accounts and invites are removed`() = runTest {
        // Act
        cleaner.clear(walletIds)

        // Assert
        coVerify(exactly = 1) { accountsStore.remove(walletIds) }
        coVerify(exactly = 1) {
            invitesStore.clearAll(walletIds[0])
            invitesStore.clearAll(walletIds[1])
        }
    }

    @Test
    fun `GIVEN the invites store fails for one wallet WHEN clear THEN the other wallet is still cleared`() = runTest {
        // Arrange
        coEvery { invitesStore.clearAll(walletIds[0]) } throws IllegalStateException("disk")

        // Act
        cleaner.clear(walletIds)

        // Assert
        coVerify(exactly = 1) { invitesStore.clearAll(walletIds[1]) }
    }
}