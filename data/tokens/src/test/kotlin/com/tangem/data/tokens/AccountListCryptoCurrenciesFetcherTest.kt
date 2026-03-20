package com.tangem.data.tokens

import arrow.core.left
import com.tangem.data.common.account.WalletAccountsFetcher
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.tokens.MultiWalletAccountListFetcher
import com.tangem.test.core.assertEither
import com.tangem.test.core.assertEitherRight
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AccountListCryptoCurrenciesFetcherTest {

    private val userWalletsListRepository: UserWalletsListRepository = mockk(relaxUnitFun = true)
    private val walletAccountsFetcher: WalletAccountsFetcher = mockk(relaxUnitFun = true)
    private val dispatchers = TestingCoroutineDispatcherProvider()

    private val fetcher = AccountListCryptoCurrenciesFetcher(
        userWalletsListRepository = userWalletsListRepository,
        walletAccountsFetcher = walletAccountsFetcher,
        dispatchers = dispatchers,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(userWalletsListRepository, walletAccountsFetcher)
    }

    @Test
    fun `returns failure if wallet is not multi-currency`() = runTest {
        // Arrange
        val params = MultiWalletAccountListFetcher.Params(userWalletId = userWalletId)
        val mockUserWallet = mockk<UserWallet> {
            every { walletId } returns userWalletId
            every { isMultiCurrency } returns false
        }
        val userWalletsFlow = MutableStateFlow(listOf(mockUserWallet))

        every { userWalletsListRepository.userWallets } returns userWalletsFlow

        // Act
        val actual = fetcher(params)

        // Assert
        val expected = IllegalStateException(
            "${AccountListCryptoCurrenciesFetcher::class.simpleName} supports only multi-currency wallet",
        ).left()
        assertEither(actual, expected)

        verify { userWalletsListRepository.userWallets }
        coVerify(inverse = true) { walletAccountsFetcher.fetch(any()) }
    }

    @Test
    fun `returns accounts if wallet is multi-currency`() = runTest {
        // Arrange
        val params = MultiWalletAccountListFetcher.Params(userWalletId = userWalletId)
        val mockUserWallet = mockk<UserWallet> {
            every { walletId } returns userWalletId
            every { isMultiCurrency } returns true
        }
        val response = mockk<GetWalletAccountsResponse>(relaxed = true)

        val userWalletsFlow = MutableStateFlow(listOf(mockUserWallet))

        every { userWalletsListRepository.userWallets } returns userWalletsFlow
        coEvery { walletAccountsFetcher.fetch(userWalletId = params.userWalletId) } returns response

        // Act
        val actual = fetcher(params)

        // Assert
        assertEitherRight(actual)

        coVerify(ordering = Ordering.SEQUENCE) {
            userWalletsListRepository.userWallets
            walletAccountsFetcher.fetch(userWalletId = params.userWalletId)
        }
    }

    @Test
    fun `returns error if walletAccountsFetcher returns error`() = runTest {
        // Arrange
        val params = MultiWalletAccountListFetcher.Params(userWalletId = userWalletId)
        val mockUserWallet = mockk<UserWallet> {
            every { walletId } returns userWalletId
            every { isMultiCurrency } returns true
        }
        val error = RuntimeException("fetch error")

        val userWalletsFlow = MutableStateFlow(listOf(mockUserWallet))

        every { userWalletsListRepository.userWallets } returns userWalletsFlow
        coEvery { walletAccountsFetcher.fetch(userWalletId = params.userWalletId) } throws error

        // Act
        val actual = fetcher(params)

        // Assert
        val expected = error.left()
        assertEither(actual, expected)

        coVerify(ordering = Ordering.SEQUENCE) {
            userWalletsListRepository.userWallets
            walletAccountsFetcher.fetch(userWalletId = params.userWalletId)
        }
    }

    private companion object {
        val userWalletId = UserWalletId("012")
    }
}