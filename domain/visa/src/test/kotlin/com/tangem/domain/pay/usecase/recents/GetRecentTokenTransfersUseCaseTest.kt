package com.tangem.domain.pay.usecase.recents

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.account.models.AccountCurrencyId
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.recents.RecentTokenTransfer
import com.tangem.domain.pay.repository.PaymentAccountRecentsStore
import com.tangem.test.mock.MockAccounts
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class GetRecentTokenTransfersUseCaseTest {

    private val recentsStore: PaymentAccountRecentsStore = mockk()
    private val singleAccountListSupplier: SingleAccountListSupplier = mockk()

    private val useCase = GetRecentTokenTransfersUseCase(
        recentsStore = recentsStore,
        singleAccountListSupplier = singleAccountListSupplier,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(recentsStore, singleAccountListSupplier)
    }

    @Test
    fun `GIVEN empty journal WHEN invoke THEN emits empty list`() = runTest {
        // Arrange
        val account = MockAccounts.createAccount(derivationIndex = 1, userWalletId = WALLET)
        stubAccounts(listOf(account))
        coEvery { recentsStore.getUsage(WALLET) } returns emptyMap()

        // Act
        val actual = useCase(WALLET).first()

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN an entry whose account is gone WHEN invoke THEN it is dropped`() = runTest {
        // Arrange
        val currency = currency()
        val account = MockAccounts.createAccount(
            derivationIndex = 1,
            userWalletId = WALLET,
            cryptoCurrencies = listOf(currency),
        )
        val missingAccount = MockAccounts.createAccount(derivationIndex = 2, userWalletId = WALLET)
        stubAccounts(listOf(account))
        coEvery { recentsStore.getUsage(WALLET) } returns mapOf(usage(missingAccount, currency) to 1L)

        // Act
        val actual = useCase(WALLET).first()

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN an entry whose currency is gone WHEN invoke THEN it is dropped`() = runTest {
        // Arrange
        val account = MockAccounts.createAccount(derivationIndex = 1, userWalletId = WALLET)
        stubAccounts(listOf(account))
        coEvery { recentsStore.getUsage(WALLET) } returns mapOf(usage(account, currency()) to 1L)

        // Act
        val actual = useCase(WALLET).first()

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN several entries WHEN invoke THEN newest first and capped at three`() = runTest {
        // Arrange
        val currencies = List(size = 4) { currency() }
        val account = MockAccounts.createAccount(
            derivationIndex = 1,
            userWalletId = WALLET,
            cryptoCurrencies = currencies,
        )
        stubAccounts(listOf(account))
        coEvery { recentsStore.getUsage(WALLET) } returns mapOf(
            usage(account, currencies[0]) to 10L,
            usage(account, currencies[1]) to 40L,
            usage(account, currencies[2]) to 30L,
            usage(account, currencies[3]) to 20L,
        )

        // Act
        val actual = useCase(WALLET).first()

        // Assert
        assertThat(actual).containsExactly(
            RecentTokenTransfer(account = account, currency = currencies[1]),
            RecentTokenTransfer(account = account, currency = currencies[2]),
            RecentTokenTransfer(account = account, currency = currencies[3]),
        ).inOrder()
    }

    @Test
    fun `GIVEN a joint account entry WHEN invoke THEN it is resolved`() = runTest {
        // Arrange
        val currency = currency()
        val joint = MockAccounts.createJointAccount(
            ownerKeyIndex = 0,
            userWalletId = WALLET,
            cryptoCurrencies = listOf(currency),
        )
        stubAccounts(listOf(MockAccounts.createAccount(derivationIndex = 1, userWalletId = WALLET), joint))
        coEvery { recentsStore.getUsage(WALLET) } returns mapOf(usage(joint, currency) to 5L)

        // Act
        val actual = useCase(WALLET).first()

        // Assert
        assertThat(actual).containsExactly(RecentTokenTransfer(account = joint, currency = currency))
    }

    private fun stubAccounts(accounts: List<Account>) {
        val accountList = AccountList(
            userWalletId = WALLET,
            accounts = listOf(Account.Personal.createMainAccount(WALLET)) + accounts,
            totalAccounts = accounts.size + 1,
            totalArchivedAccounts = 0,
            totalJointAccounts = accounts.count { it is Account.Joint },
        ).getOrNull()!!
        every { singleAccountListSupplier(WALLET) } returns flowOf(accountList)
    }

    private fun usage(account: Account.CryptoPortfolio, currency: CryptoCurrency): AccountCurrencyId =
        account.accountId to currency.id

    private fun currency(): CryptoCurrency {
        val id: CryptoCurrency.ID = mockk(relaxed = true)
        return mockk(relaxed = true) { every { this@mockk.id } returns id }
    }

    private companion object {
        val WALLET = UserWalletId("0123456789abcdef")
    }
}