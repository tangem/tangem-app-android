package com.tangem.data.account.fetcher

import arrow.core.left
import arrow.core.right
import com.tangem.common.test.utils.assertEitherLeft
import com.tangem.common.test.utils.assertEitherRight
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.account.fetcher.MultiAccountListFetcher
import com.tangem.domain.account.fetcher.SingleAccountListFetcher
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultMultiAccountListFetcherTest {

    private val singleAccountListFetcher: SingleAccountListFetcher = mockk()
    private val userWalletsStore: UserWalletsStore = mockk(relaxUnitFun = true)

    private val fetcher = DefaultMultiAccountListFetcher(singleAccountListFetcher, userWalletsStore)

    private val userWalletId1 = UserWalletId("011")
    private val userWalletId2 = UserWalletId("012")

    private val userWalletIds = setOf(userWalletId1, userWalletId2)
    private val userWallets = userWalletIds.map {
        mockk<UserWallet> {
            every { this@mockk.walletId } returns it
        }
    }

    @AfterEach
    fun tearDown() {
        clearMocks(singleAccountListFetcher, userWalletsStore)
    }

    @Test
    fun `invoke returns Right when fetch succeeds for Set`() = runTest {
        // Arrange
        val params = MultiAccountListFetcher.Params.Set(ids = userWalletIds)

        userWalletIds.onEach {
            coEvery {
                singleAccountListFetcher(params = SingleAccountListFetcher.Params(it))
            } returns Unit.right()
        }

        // Act
        val actual = fetcher.invoke(params)

        // Assert
        assertEitherRight(actual)

        coVerify(ordering = Ordering.SEQUENCE) {
            singleAccountListFetcher(params = SingleAccountListFetcher.Params(userWalletId1))
            singleAccountListFetcher(params = SingleAccountListFetcher.Params(userWalletId2))
        }
    }

    @Test
    fun `invoke returns Left when fetch throws exception for Set`() = runTest {
        // Arrange
        val params = MultiAccountListFetcher.Params.Set(ids = userWalletIds)

        val exception = Exception("Fetch failed")
        coEvery {
            singleAccountListFetcher(params = SingleAccountListFetcher.Params(userWalletId1))
        } returns exception.left()

        coEvery {
            singleAccountListFetcher(params = SingleAccountListFetcher.Params(userWalletId2))
        } returns Unit.right()

        // Act
        val actual = fetcher.invoke(params)

        // Assert
        val expected = IllegalStateException(
            """
                Failed to fetch accounts for wallets:
                UserWalletId(011...011)=java.lang.Exception: Fetch failed
            """.trimIndent(),
        )
        assertEitherLeft(actual, expected)

        coVerify(ordering = Ordering.SEQUENCE) {
            singleAccountListFetcher(params = SingleAccountListFetcher.Params(userWalletId1))
            singleAccountListFetcher(params = SingleAccountListFetcher.Params(userWalletId2))
        }
    }

    @Test
    fun `invoke returns Right when fetch succeeds for All`() = runTest {
        // Arrange
        val params = MultiAccountListFetcher.Params.All

        every { userWalletsStore.userWalletsSync } returns listOf(userWallets.first())

        coEvery {
            singleAccountListFetcher(params = SingleAccountListFetcher.Params(userWalletId1))
        } returns Unit.right()

        // Act
        val actual = fetcher.invoke(params)

        // Assert
        assertEitherRight(actual)

        coVerify(ordering = Ordering.SEQUENCE) {
            userWalletsStore.userWalletsSync
            singleAccountListFetcher(params = SingleAccountListFetcher.Params(userWalletId1))
        }
    }

    @Test
    fun `invoke returns Left when fetch throws exception for All`() = runTest {
        // Arrange
        val params = MultiAccountListFetcher.Params.All

        every { userWalletsStore.userWalletsSync } returns userWallets

        val exception = Exception("Fetch failed")
        coEvery {
            singleAccountListFetcher(params = SingleAccountListFetcher.Params(userWalletId1))
        } returns exception.left()

        coEvery {
            singleAccountListFetcher(params = SingleAccountListFetcher.Params(userWalletId2))
        } returns Unit.right()

        // Act
        val actual = fetcher.invoke(params)

        // Assert
        val expected = IllegalStateException(
            """
                Failed to fetch accounts for wallets:
                UserWalletId(011...011)=java.lang.Exception: Fetch failed
            """.trimIndent(),
        )
        assertEitherLeft(actual, expected)

        coVerify(ordering = Ordering.SEQUENCE) {
            userWalletsStore.userWalletsSync
            singleAccountListFetcher(params = SingleAccountListFetcher.Params(userWalletId1))
            singleAccountListFetcher(params = SingleAccountListFetcher.Params(userWalletId2))
        }
    }
}