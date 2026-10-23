package com.tangem.domain.pay.usecase.recents

import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.recents.TokenTransferJournal
import com.tangem.domain.pay.repository.PaymentAccountRecentsStore
import com.tangem.test.mock.MockAccounts
import io.mockk.clearMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

internal class RecordTokenTransferFromSwapUseCaseTest {

    private val recentsStore: PaymentAccountRecentsStore = mockk(relaxed = true)

    private val useCase = RecordTokenTransferFromSwapUseCase(recentsStore = recentsStore)

    @BeforeEach
    fun resetMocks() {
        clearMocks(recentsStore)
    }

    @ParameterizedTest
    @MethodSource("provideJournalAccounts")
    fun `GIVEN a journal account as FROM WHEN invoke THEN the TO side is recorded in its journal`(
        journalAccount: JournalAccount,
    ) = runTest {
        // Arrange
        val portfolio = MockAccounts.createAccount(derivationIndex = 1, userWalletId = WALLET)
        val toCurrency = currency()
        val toCurrencyId = toCurrency.id

        // Act
        useCase(
            fromUserWalletId = WALLET,
            fromAccount = journalAccount.account,
            fromCurrency = currency(),
            toUserWalletId = WALLET,
            toAccount = portfolio,
            toCurrency = toCurrency,
        )

        // Assert
        coVerify(exactly = 1) {
            recentsStore.markUsed(
                userWalletId = WALLET,
                journal = journalAccount.journal,
                accountId = portfolio.accountId,
                currencyId = toCurrencyId,
            )
        }
    }

    @ParameterizedTest
    @MethodSource("provideJournalAccounts")
    fun `GIVEN a journal account as TO WHEN invoke THEN the FROM side is recorded in its journal`(
        journalAccount: JournalAccount,
    ) = runTest {
        // Arrange
        val portfolio = MockAccounts.createAccount(derivationIndex = 1, userWalletId = WALLET)
        val fromCurrency = currency()
        val fromCurrencyId = fromCurrency.id

        // Act
        useCase(
            fromUserWalletId = WALLET,
            fromAccount = portfolio,
            fromCurrency = fromCurrency,
            toUserWalletId = WALLET,
            toAccount = journalAccount.account,
            toCurrency = currency(),
        )

        // Assert
        coVerify(exactly = 1) {
            recentsStore.markUsed(
                userWalletId = WALLET,
                journal = journalAccount.journal,
                accountId = portfolio.accountId,
                currencyId = fromCurrencyId,
            )
        }
    }

    @Test
    fun `GIVEN no journal account on either side WHEN invoke THEN nothing is recorded`() = runTest {
        // Act
        useCase(
            fromUserWalletId = WALLET,
            fromAccount = MockAccounts.createAccount(derivationIndex = 1, userWalletId = WALLET),
            fromCurrency = currency(),
            toUserWalletId = WALLET,
            toAccount = MockAccounts.createAccount(derivationIndex = 2, userWalletId = WALLET),
            toCurrency = currency(),
        )

        // Assert
        coVerify(exactly = 0) { recentsStore.markUsed(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN the sides belong to different wallets WHEN invoke THEN nothing is recorded`() = runTest {
        // Act
        useCase(
            fromUserWalletId = WALLET,
            fromAccount = Account.Payment(WALLET),
            fromCurrency = currency(),
            toUserWalletId = OTHER_WALLET,
            toAccount = MockAccounts.createAccount(derivationIndex = 1, userWalletId = OTHER_WALLET),
            toCurrency = currency(),
        )

        // Assert
        coVerify(exactly = 0) { recentsStore.markUsed(any(), any(), any(), any()) }
    }

    @ParameterizedTest
    @MethodSource("provideJournalAccountPairsWithoutPortfolio")
    fun `GIVEN a journal account paired with a special account WHEN invoke THEN nothing is recorded`(
        pair: Pair<Account, Account>,
    ) = runTest {
        // Act
        useCase(
            fromUserWalletId = WALLET,
            fromAccount = pair.first,
            fromCurrency = currency(),
            toUserWalletId = WALLET,
            toAccount = pair.second,
            toCurrency = currency(),
        )

        // Assert
        coVerify(exactly = 0) { recentsStore.markUsed(any(), any(), any(), any()) }
    }

    private fun currency(): CryptoCurrency {
        val id: CryptoCurrency.ID = mockk(relaxed = true)
        return mockk(relaxed = true) { every { this@mockk.id } returns id }
    }

    class JournalAccount(val account: Account, val journal: TokenTransferJournal) {
        override fun toString(): String = journal.toString()
    }

    private companion object {
        val WALLET = UserWalletId("0123456789abcdef")
        val OTHER_WALLET = UserWalletId("fedcba9876543210")

        @JvmStatic
        fun provideJournalAccounts() = listOf(
            JournalAccount(account = Account.Payment(WALLET), journal = TokenTransferJournal.PAYMENT_ACCOUNT),
            JournalAccount(account = Account.Money(WALLET), journal = TokenTransferJournal.MONEY_ACCOUNT),
        )

        @JvmStatic
        fun provideJournalAccountPairsWithoutPortfolio() = listOf(
            Account.Payment(WALLET) to Account.Prediction(WALLET),
            Account.Money(WALLET) to Account.Payment(WALLET),
            Account.Payment(WALLET) to Account.Money(WALLET),
        )
    }
}