package com.tangem.data.account.cleaner

import com.tangem.data.account.store.AccountsResponseStoreFactory
import com.tangem.data.account.store.LegacyUserTokensResponseStore
import com.tangem.data.common.cache.etag.ETagsStore
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AccountsUserWalletDataCleanerTest {

    private val accountsResponseStoreFactory: AccountsResponseStoreFactory = mockk()
    private val legacyUserTokensResponseStore: LegacyUserTokensResponseStore = mockk()
    private val eTagsStore: ETagsStore = mockk()

    private val cleaner = AccountsUserWalletDataCleaner(
        accountsResponseStoreFactory = accountsResponseStoreFactory,
        legacyUserTokensResponseStore = legacyUserTokensResponseStore,
        eTagsStore = eTagsStore,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(accountsResponseStoreFactory, legacyUserTokensResponseStore, eTagsStore)
        coEvery { accountsResponseStoreFactory.clear(any<List<UserWalletId>>()) } just Runs
        coEvery { legacyUserTokensResponseStore.clear(any<List<UserWalletId>>()) } just Runs
        coEvery { eTagsStore.clear(any<List<UserWalletId>>()) } just Runs
    }

    @Test
    fun `GIVEN wallets WHEN clear THEN each store is cleared once with all ids in a single call`() = runTest {
        // Arrange
        val ids = listOf(WALLET_A, WALLET_B)

        // Act
        cleaner.clear(ids)

        // Assert
        coVerify(exactly = 1) { accountsResponseStoreFactory.clear(ids) }
        coVerify(exactly = 1) { legacyUserTokensResponseStore.clear(ids) }
        coVerify(exactly = 1) { eTagsStore.clear(ids) }
    }

    private companion object {
        val WALLET_A = UserWalletId("011")
        val WALLET_B = UserWalletId("022")
    }
}