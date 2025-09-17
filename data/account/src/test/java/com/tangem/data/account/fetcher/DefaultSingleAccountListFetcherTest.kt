package com.tangem.data.account.fetcher

import com.tangem.common.test.utils.assertEitherLeft
import com.tangem.common.test.utils.assertEitherRight
import com.tangem.data.common.account.WalletAccountsFetcher
import com.tangem.domain.account.fetcher.SingleAccountListFetcher
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultSingleAccountListFetcherTest {

    private val walletAccountsFetcher: WalletAccountsFetcher = mockk(relaxUnitFun = true)
    private val fetcher = DefaultSingleAccountListFetcher(walletAccountsFetcher)

    private val userWalletId = UserWalletId("011")

    @AfterEach
    fun tearDown() {
        clearMocks(walletAccountsFetcher)
    }

    @Test
    fun `invoke returns Right when fetch succeeds`() = runTest {
        // Arrange
        val params = SingleAccountListFetcher.Params(userWalletId = userWalletId)

        // Act
        val actual = fetcher.invoke(params)

        // Assert
        assertEitherRight(actual)

        coVerify(ordering = Ordering.SEQUENCE) {
            walletAccountsFetcher.fetch(userWalletId)
        }
    }

    @Test
    fun `invoke returns Left when fetch throws exception`() = runTest {
        // Arrange
        val params = SingleAccountListFetcher.Params(userWalletId = userWalletId)

        val exception = Exception("Fetch failed")
        coEvery { walletAccountsFetcher.fetch(userWalletId) } throws exception

        // Act
        val actual = fetcher.invoke(params)

        // Assert
        val expected = exception
        assertEitherLeft(actual, expected)

        coVerify(ordering = Ordering.SEQUENCE) {
            walletAccountsFetcher.fetch(userWalletId)
        }
    }
}