package com.tangem.data.tokens

import arrow.core.left
import arrow.core.right
import com.tangem.common.test.utils.assertEither
import com.tangem.common.test.utils.assertEitherRight
import com.tangem.data.common.account.WalletAccountsFetcher
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.express.ExpressServiceFetcher
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesFetcher
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AccountListCryptoCurrenciesFetcherTest {

    private val userWalletsStore: UserWalletsStore = mockk(relaxUnitFun = true)
    private val walletAccountsFetcher: WalletAccountsFetcher = mockk(relaxUnitFun = true)
    private val expressServiceFetcher: ExpressServiceFetcher = mockk()
    private val dispatchers = TestingCoroutineDispatcherProvider()

    private val fetcher = AccountListCryptoCurrenciesFetcher(
        userWalletsStore = userWalletsStore,
        walletAccountsFetcher = walletAccountsFetcher,
        expressServiceFetcher = expressServiceFetcher,
        dispatchers = dispatchers,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(userWalletsStore, walletAccountsFetcher)
    }

    @Test
    fun `returns failure if wallet is not multi-currency`() = runTest {
        // Arrange
        val params = MultiWalletCryptoCurrenciesFetcher.Params(userWalletId = userWalletId)
        val mockUserWallet = mockk<UserWallet> { every { isMultiCurrency } returns false }
        every { userWalletsStore.getSyncStrict(key = params.userWalletId) } returns mockUserWallet

        // Act
        val actual = fetcher(params)

        // Assert
        val expected = IllegalStateException(
            "${AccountListCryptoCurrenciesFetcher::class.simpleName} supports only multi-currency wallet",
        ).left()
        assertEither(actual, expected)

        verify { userWalletsStore.getSyncStrict(key = params.userWalletId) }
        coVerify(inverse = true) { walletAccountsFetcher.fetch(any()) }
    }

    @Test
    fun `returns accounts if wallet is multi-currency`() = runTest {
        // Arrange
        val params = MultiWalletCryptoCurrenciesFetcher.Params(userWalletId = userWalletId)
        val mockUserWallet = mockk<UserWallet> { every { isMultiCurrency } returns true }
        val response = mockk<GetWalletAccountsResponse>(relaxed = true)

        every { userWalletsStore.getSyncStrict(key = params.userWalletId) } returns mockUserWallet
        coEvery { walletAccountsFetcher.fetch(userWalletId = params.userWalletId) } returns response
        coEvery { expressServiceFetcher.fetch(userWallet = mockUserWallet, assetIds = emptySet()) } returns Unit.right()

        // Act
        val actual = fetcher(params)

        // Assert
        assertEitherRight(actual)

        coVerify(ordering = Ordering.SEQUENCE) {
            userWalletsStore.getSyncStrict(key = params.userWalletId)
            walletAccountsFetcher.fetch(userWalletId = params.userWalletId)
            expressServiceFetcher.fetch(userWallet = mockUserWallet, assetIds = emptySet())
        }
    }

    @Test
    fun `returns error if walletAccountsFetcher returns error`() = runTest {
        // Arrange
        val params = MultiWalletCryptoCurrenciesFetcher.Params(userWalletId = userWalletId)
        val mockUserWallet = mockk<UserWallet> { every { isMultiCurrency } returns true }
        val error = RuntimeException("fetch error")

        every { userWalletsStore.getSyncStrict(key = params.userWalletId) } returns mockUserWallet
        coEvery { walletAccountsFetcher.fetch(userWalletId = params.userWalletId) } throws error

        // Act
        val actual = fetcher(params)

        // Assert
        val expected = error.left()
        assertEither(actual, expected)

        coVerify(ordering = Ordering.SEQUENCE) {
            userWalletsStore.getSyncStrict(key = params.userWalletId)
            walletAccountsFetcher.fetch(userWalletId = params.userWalletId)
        }
    }

    private companion object {
        val userWalletId = UserWalletId("012")
    }
}