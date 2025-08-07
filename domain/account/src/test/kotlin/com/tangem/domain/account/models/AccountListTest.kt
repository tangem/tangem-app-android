package com.tangem.domain.account.models

import arrow.core.Either
import arrow.core.left
import com.google.common.truth.Truth
import com.tangem.domain.account.utils.createAccount
import com.tangem.domain.account.utils.createAccounts
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.clearMocks
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountListTest {

    @Test
    fun mainAccount() {
        // Arrange
        val mainAccount = Account.CryptoPortfolio.createMainAccount(userWalletId = userWalletId)

        val accountList = AccountList(
            userWallet = mockk(),
            accounts = setOf(mainAccount),
            totalAccounts = 1,
        )
            .getOrNull()!!

        // Act
        val actual = accountList.mainAccount

        // Assert
        val expected = mainAccount
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun canAddMoreAccounts() {
        // Arrange
        val accountList = AccountList(
            userWallet = mockk(),
            accounts = createAccounts(userWalletId = userWalletId, count = 2),
            totalAccounts = 2,
        ).getOrNull()!!

        val fullAccountList = AccountList(
            userWallet = mockk(),
            accounts = createAccounts(userWalletId = userWalletId, count = 20),
            totalAccounts = 20,
        ).getOrNull()!!

        // Act & Assert
        Truth.assertThat(accountList.canAddMoreAccounts).isTrue()
        Truth.assertThat(fullAccountList.canAddMoreAccounts).isFalse()
    }

    @Test
    fun createEmpty() {
        // Arrange
        val userWallet = mockk<UserWallet>(relaxed = true)

        // Act
        val actual = AccountList.createEmpty(userWallet)

        // Assert
        val expected = AccountList(
            userWallet = userWallet,
            accounts = setOf(Account.CryptoPortfolio.createMainAccount(userWalletId = userWallet.walletId)),
            totalAccounts = 1,
        ).getOrNull()!!

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Create {

        private val userWallet = mockk<UserWallet>()

        @BeforeEach
        fun resetMocks() {
            clearMocks(userWallet)
        }

        @ParameterizedTest
        @MethodSource("provideTestModels")
        fun invoke(model: CreateTestModel) {
            // Act
            val actual = AccountList(
                userWallet = userWallet,
                accounts = model.accounts,
                totalAccounts = model.accounts.size,
            )

            // Assert
            Truth.assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            CreateTestModel(
                accounts = emptySet(),
                expected = AccountList.Error.EmptyAccountsList.left(),
            ),
            CreateTestModel(
                accounts = setOf(
                    createAccount(userWalletId = userWalletId, derivationIndex = 1),
                ),
                expected = AccountList.Error.MainAccountNotFound.left(),
            ),
            CreateTestModel(
                accounts = setOf(
                    Account.CryptoPortfolio.createMainAccount(userWalletId),
                    Account.CryptoPortfolio.createMainAccount(userWalletId).copy(
                        accountIcon = CryptoPortfolioIcon.ofDefaultCustomAccount(),
                    ),
                ),
                expected = AccountList.Error.ExceedsMaxMainAccountsCount.left(),
            ),
            createAccounts(userWalletId = userWalletId, count = 1).let {
                CreateTestModel(
                    accounts = it,
                    expected = AccountList(userWallet = userWallet, accounts = it, totalAccounts = 1),
                )
            },
            createAccounts(userWalletId = userWalletId, count = 20).let {
                CreateTestModel(
                    accounts = it,
                    expected = AccountList(userWallet = userWallet, accounts = it, totalAccounts = 20),
                )
            },
            CreateTestModel(
                accounts = createAccounts(userWalletId = userWalletId, count = 21),
                expected = AccountList.Error.ExceedsMaxAccountsCount.left(),
            ),
            CreateTestModel(
                accounts = setOf(
                    createAccount(userWalletId = userWalletId, derivationIndex = 0),
                    createAccount(userWalletId = userWalletId, derivationIndex = 1),
                    createAccount(userWalletId = userWalletId, derivationIndex = 1),
                ),
                expected = AccountList.Error.DuplicateAccountIds.left(),
            ),
        )
    }

    data class CreateTestModel(
        val accounts: Set<Account>,
        val expected: Either<AccountList.Error, AccountList>,
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Plus {

        private val userWallet = mockk<UserWallet>()

        @ParameterizedTest
        @MethodSource("provideTestModels")
        fun invoke(model: PlusTestModel) {
            // Act
            val actual = model.initial.plus(other = model.toAdd)

            // Assert
            Truth.assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            // region Add new account
            run {
                val mainAccount = Account.CryptoPortfolio.createMainAccount(userWalletId)
                val newAccount = createAccount(userWalletId = userWalletId, derivationIndex = 1)

                PlusTestModel(
                    initial = AccountList(
                        userWallet = userWallet,
                        accounts = setOf(mainAccount),
                        totalAccounts = 1,
                    ).getOrNull()!!,
                    toAdd = newAccount,
                    expected = AccountList(
                        userWallet = userWallet,
                        accounts = setOf(mainAccount, newAccount),
                        totalAccounts = 2,
                    ),
                )
            },
            // endregion
            // region Replace existing account
            run {
                val mainAccount = Account.CryptoPortfolio.createMainAccount(userWalletId)
                val newAccount = mainAccount.copy(accountName = AccountName("New Name").getOrNull()!!)

                PlusTestModel(
                    initial = AccountList(
                        userWallet = userWallet,
                        accounts = setOf(mainAccount),
                        totalAccounts = 1,
                    ).getOrNull()!!,
                    toAdd = newAccount,
                    expected = AccountList(
                        userWallet = userWallet,
                        accounts = setOf(newAccount),
                        totalAccounts = 1,
                    ),
                )
            },
            // endregion
            PlusTestModel(
                initial = AccountList(
                    userWallet = userWallet,
                    accounts = createAccounts(userWalletId = userWalletId, count = 20),
                    totalAccounts = 20,
                ).getOrNull()!!,
                toAdd = createAccount(userWalletId = userWalletId, derivationIndex = 21),
                expected = AccountList.Error.ExceedsMaxAccountsCount.left(),
            ),
        )
    }

    data class PlusTestModel(
        val initial: AccountList,
        val toAdd: Account,
        val expected: Either<AccountList.Error, AccountList>,
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Minus {

        private val userWallet = mockk<UserWallet>()

        @ParameterizedTest
        @MethodSource("provideTestModels")
        fun invoke(model: MinusTestModel) {
            // Act
            val actual = model.initial.minus(model.toRemove)

            // Assert
            Truth.assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            // region Remove existing account
            run {
                val mainAccount = Account.CryptoPortfolio.createMainAccount(userWalletId)
                val secondaryAccount = createAccount(userWalletId = userWalletId, derivationIndex = 2)

                MinusTestModel(
                    initial = AccountList(
                        userWallet = userWallet,
                        accounts = setOf(mainAccount, secondaryAccount),
                        totalAccounts = 2,
                    ).getOrNull()!!,
                    toRemove = secondaryAccount,
                    expected = AccountList(
                        userWallet = userWallet,
                        accounts = setOf(mainAccount),
                        totalAccounts = 1,
                    ),
                )
            },
            // endregion
            // region Remove unexisting account
            run {
                val mainAccount = Account.CryptoPortfolio.createMainAccount(userWalletId)
                val notInList = createAccount(userWalletId = userWalletId, derivationIndex = 3)

                MinusTestModel(
                    initial = AccountList(
                        userWallet = userWallet,
                        accounts = setOf(mainAccount),
                        totalAccounts = 1,
                    ).getOrNull()!!,
                    toRemove = notInList,
                    expected = AccountList(
                        userWallet = userWallet,
                        accounts = setOf(mainAccount),
                        totalAccounts = 1,
                    ),
                )
            },
            // endregion
            // region EmptyAccountsList
            run {
                val mainAccount = Account.CryptoPortfolio.createMainAccount(userWalletId)

                MinusTestModel(
                    initial = AccountList(
                        userWallet = userWallet,
                        accounts = setOf(mainAccount),
                        totalAccounts = 1,
                    ).getOrNull()!!,
                    toRemove = mainAccount,
                    expected = AccountList.Error.EmptyAccountsList.left(),
                )
            },
            // endregion
            // region MainAccountNotFound
            run {
                val mainAccount = Account.CryptoPortfolio.createMainAccount(userWalletId)
                val secondaryAccount = createAccount(userWalletId = userWalletId, derivationIndex = 2)

                MinusTestModel(
                    initial = AccountList(
                        userWallet = userWallet,
                        accounts = setOf(mainAccount, secondaryAccount),
                        totalAccounts = 2,
                    ).getOrNull()!!,
                    toRemove = mainAccount,
                    expected = AccountList.Error.MainAccountNotFound.left(),
                )
            },
            // endregion
        )
    }

    data class MinusTestModel(
        val initial: AccountList,
        val toRemove: Account,
        val expected: Either<AccountList.Error, AccountList>,
    )

    private companion object {

        val userWalletId = UserWalletId("011")
    }
}