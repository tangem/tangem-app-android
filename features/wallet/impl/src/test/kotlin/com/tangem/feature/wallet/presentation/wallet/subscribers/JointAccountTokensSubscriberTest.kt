package com.tangem.feature.wallet.presentation.wallet.subscribers

import arrow.core.Either
import com.tangem.domain.account.fetcher.SingleAccountListFetcher
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.status.utils.CryptoCurrencyBalanceFetcher
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.utils.ScreenLifecycleProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class JointAccountTokensSubscriberTest {

    private val userWalletId = UserWalletId("01")
    private val userWallet: UserWallet = mockk { every { walletId } returns userWalletId }
    private val singleAccountListSupplier: SingleAccountListSupplier = mockk()
    private val singleAccountListFetcher: SingleAccountListFetcher = mockk()
    private val cryptoCurrencyBalanceFetcher: CryptoCurrencyBalanceFetcher = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val otherWallet: UserWallet = mockk { every { walletId } returns UserWalletId("02") }
    private val selectedWallet = MutableStateFlow<UserWallet?>(null)

    private val ethereum = createCurrency()
    private val tether = createCurrency()

    private lateinit var screenLifecycleProvider: ScreenLifecycleProvider
    private lateinit var accountLists: MutableStateFlow<AccountList>

    @BeforeEach
    fun resetMocks() {
        clearMocks(singleAccountListSupplier, singleAccountListFetcher, cryptoCurrencyBalanceFetcher)
        selectedWallet.value = userWallet
        every { userWalletsListRepository.selectedUserWallet } returns selectedWallet
        screenLifecycleProvider = ScreenLifecycleProvider()
        coEvery { singleAccountListFetcher(any()) } returns Either.Right(Unit)
        every { cryptoCurrencyBalanceFetcher(userWalletId = any(), currencies = any<List<CryptoCurrency>>()) } just runs
    }

    @Test
    fun `GIVEN joint account WHEN currency added THEN fetches balance of added currency only`() = runTest {
        // Arrange
        givenAccountList(createAccountList(createJointAccount(ethereum)))
        collectSubscriber()

        // Act
        accountLists.value = createAccountList(createJointAccount(ethereum, tether))
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            cryptoCurrencyBalanceFetcher(userWalletId = userWalletId, currencies = listOf(tether))
        }
    }

    @Test
    fun `GIVEN joint account WHEN currency removed THEN no balance fetch`() = runTest {
        // Arrange
        givenAccountList(createAccountList(createJointAccount(ethereum, tether)))
        collectSubscriber()

        // Act
        accountLists.value = createAccountList(createJointAccount(ethereum))
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { cryptoCurrencyBalanceFetcher(userWalletId = any(), currencies = any<List<CryptoCurrency>>()) }
    }

    @Test
    fun `GIVEN non joint account WHEN currency added THEN no balance fetch`() = runTest {
        // Arrange
        givenAccountList(createAccountList(createPersonalAccount(ethereum)))
        collectSubscriber()

        // Act
        accountLists.value = createAccountList(createPersonalAccount(ethereum, tether))
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { cryptoCurrencyBalanceFetcher(userWalletId = any(), currencies = any<List<CryptoCurrency>>()) }
    }

    @Test
    fun `GIVEN joint account WHEN first list emitted THEN no balance fetch`() = runTest {
        // Arrange
        givenAccountList(createAccountList(createJointAccount(ethereum, tether)))

        // Act
        collectSubscriber()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { cryptoCurrencyBalanceFetcher(userWalletId = any(), currencies = any<List<CryptoCurrency>>()) }
    }

    @Test
    fun `GIVEN wallet with joint account WHEN screen resumed after pause THEN refreshes account list`() = runTest {
        // Arrange
        givenAccountList(createAccountList(createJointAccount(ethereum)))
        collectSubscriber()

        // Act
        screenLifecycleProvider.onPause()
        screenLifecycleProvider.onResume()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) {
            singleAccountListFetcher(SingleAccountListFetcher.Params(userWalletId = userWalletId))
        }
    }

    @Test
    fun `GIVEN wallet without joint account WHEN screen resumed after pause THEN no refresh`() = runTest {
        // Arrange
        givenAccountList(createAccountList(createPersonalAccount(ethereum)))
        collectSubscriber()

        // Act
        screenLifecycleProvider.onPause()
        screenLifecycleProvider.onResume()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { singleAccountListFetcher(any()) }
    }

    @Test
    fun `GIVEN joint account of not selected wallet WHEN screen resumed after pause THEN no refresh`() = runTest {
        // Arrange
        givenAccountList(createAccountList(createJointAccount(ethereum)))
        selectedWallet.value = otherWallet
        collectSubscriber()

        // Act
        screenLifecycleProvider.onPause()
        screenLifecycleProvider.onResume()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { singleAccountListFetcher(any()) }
    }

    @Test
    fun `GIVEN wallet with joint account WHEN subscribed and paused THEN no refresh`() = runTest {
        // Arrange
        givenAccountList(createAccountList(createJointAccount(ethereum)))

        // Act
        collectSubscriber()
        screenLifecycleProvider.onPause()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { singleAccountListFetcher(any()) }
    }

    private fun givenAccountList(accountList: AccountList) {
        accountLists = MutableStateFlow(accountList)
        every { singleAccountListSupplier(userWalletId) } returns accountLists
        coEvery { singleAccountListSupplier.getSyncOrNull(userWalletId) } answers { accountLists.value }
    }

    private fun TestScope.collectSubscriber() {
        JointAccountTokensSubscriber(
            userWallet = userWallet,
            screenLifecycleProvider = screenLifecycleProvider,
            userWalletsListRepository = userWalletsListRepository,
            singleAccountListSupplier = singleAccountListSupplier,
            singleAccountListFetcher = singleAccountListFetcher,
            cryptoCurrencyBalanceFetcher = cryptoCurrencyBalanceFetcher,
        ).subscribe(
            coroutineScope = backgroundScope + UnconfinedTestDispatcher(testScheduler),
            dispatchers = UnconfinedTestDispatcher(testScheduler),
        )
    }

    private fun createAccountList(vararg accounts: Account): AccountList = mockk {
        every { this@mockk.accounts } returns accounts.toList()
    }

    private fun createJointAccount(vararg currencies: CryptoCurrency): Account.Joint = mockk {
        every { cryptoCurrencies } returns currencies.toList()
    }

    private fun createPersonalAccount(vararg currencies: CryptoCurrency): Account.Personal = mockk {
        every { cryptoCurrencies } returns currencies.toList()
    }

    private fun createCurrency(): CryptoCurrency {
        val currencyId = mockk<CryptoCurrency.ID>()
        return mockk { every { id } returns currencyId }
    }
}