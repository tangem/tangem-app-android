package com.tangem.data.txhistory.fetcher

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.account.AccountId
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.test.mock.MockAccounts
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.job
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultAccountTxHistoryFetcherTest {

    @Test
    fun `exposes the account it was created with`() = runTest {
        val fetcher = createFetcher(ACCOUNT_ID, createUtils())

        assertThat(fetcher.accountId).isEqualTo(ACCOUNT_ID)
    }

    @Test
    fun `close cancels the fetcher scope`() = runTest {
        val utils = createUtils()
        val fetcher = createFetcher(ACCOUNT_ID, utils)

        // Act
        fetcher.close()

        // Assert
        assertThat(utils.fetcherScope.coroutineContext.job.isActive).isFalse()
    }

    private fun TestScope.createUtils(): DefaultTxHistoryFetcherUtils = DefaultTxHistoryFetcherUtils(
        appScope = TestAppCoroutineScope(testScope = this),
        analyticsEventHandler = mockk(relaxed = true),
        analyticsExceptionHandler = mockk(relaxed = true),
    )

    private fun createFetcher(accountId: AccountId, utils: DefaultTxHistoryFetcherUtils) =
        DefaultAccountTxHistoryFetcher(accountId = accountId, utils = utils)

    private companion object {
        val ACCOUNT_ID = AccountId.forMainCryptoPortfolio(MockAccounts.userWalletId)
    }
}