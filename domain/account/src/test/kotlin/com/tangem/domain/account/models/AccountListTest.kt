package com.tangem.domain.account.models

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.MockAccounts
import com.tangem.test.mock.MockAccounts.createAccount
import com.tangem.test.mock.MockAccounts.createAccountList
import com.tangem.test.mock.MockAccounts.createAccounts
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AccountListTest {

    @Test
    fun mainAccount() {
        // Act
        val actual = MockAccounts.onlyMainAccount.mainAccount

        // Assert
        val expected = Account.CryptoPortfolio.createMainAccount(userWalletId = userWalletId)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun canAddMoreAccounts() {
        // Arrange
        val accountList = createAccountList(activeAccounts = 2)

        val fullAccountList = MockAccounts.fullAccountList

        // Act & Assert
        Truth.assertThat(accountList.canAddMoreAccounts).isTrue()
        Truth.assertThat(fullAccountList.canAddMoreAccounts).isFalse()
    }

    @Test
    fun activeAccounts() {
        // Arrange
        val accountList = createAccountList(activeAccounts = 5, totalAccounts = 10)

        // Act & Assert
        val expected = 5
        Truth.assertThat(accountList.activeAccounts).isEqualTo(expected)
    }

    @Test
    fun empty() {
        // Act
        val actual = AccountList.empty(userWalletId)

        // Assert
        val expected = MockAccounts.onlyMainAccount

        // Assert
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Create {

        @Test
        fun `invoke with non default sort and group types`() {
            // Arrange
            val accounts = createAccounts(count = 5)
            val sortType = TokensSortType.BALANCE
            val groupType = TokensGroupType.NETWORK

            // Act
            val actual = AccountList(
                userWalletId = userWalletId,
                accounts = accounts,
                totalAccounts = accounts.size,
                sortType = sortType,
                groupType = groupType,
            )

            // Assert
            val expected = AccountList(
                userWalletId = userWalletId,
                accounts = accounts,
                totalAccounts = accounts.size,
                sortType = sortType,
                groupType = groupType,
            )
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun invoke(model: CreateTestModel) {
            // Act
            val actual = AccountList(
                userWalletId = userWalletId,
                accounts = model.accounts,
                totalAccounts = model.totalAccounts,
            )

            // Assert
            Truth.assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            CreateTestModel(
                accounts = emptyList(),
                expected = AccountList.Error.EmptyAccountsList.left(),
            ),
            CreateTestModel(
                accounts = createAccounts(count = 21),
                expected = AccountList.Error.ExceedsMaxAccountsCount.left(),
            ),
            CreateTestModel(
                accounts = listOf(
                    createAccount(derivationIndex = 1),
                ),
                expected = AccountList.Error.MainAccountNotFound.left(),
            ),
            CreateTestModel(
                accounts = listOf(
                    Account.CryptoPortfolio.createMainAccount(userWalletId),
                    Account.CryptoPortfolio.createMainAccount(userWalletId).copy(
                        icon = CryptoPortfolioIcon.ofDefaultCustomAccount(),
                    ),
                ),
                expected = AccountList.Error.ExceedsMaxMainAccountsCount.left(),
            ),
            CreateTestModel(
                accounts = listOf(
                    createAccount(derivationIndex = 0),
                    createAccount(derivationIndex = 1),
                    createAccount(derivationIndex = 1),
                ),
                expected = AccountList.Error.DuplicateAccountIds.left(),
            ),
            CreateTestModel(
                accounts = listOf(
                    createAccount(name = "Name", derivationIndex = 0),
                    createAccount(name = "Name", derivationIndex = 1),
                ),
                expected = AccountList.Error.DuplicateAccountNames.left(),
            ),
            CreateTestModel(
                accounts = createAccounts(count = 2),
                totalAccounts = 1,
                expected = AccountList.Error.TotalAccountsLessThanActive.left(),
            ),
            createAccountList(activeAccounts = 1).let {
                CreateTestModel(
                    accounts = it.accounts,
                    expected = it.right(),
                )
            },
            createAccountList(activeAccounts = 20).let {
                CreateTestModel(
                    accounts = it.accounts,
                    expected = it.right(),
                )
            },
        )
    }

    data class CreateTestModel(
        val accounts: List<Account>,
        val totalAccounts: Int = accounts.size,
        val expected: Either<AccountList.Error, AccountList>,
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Plus {

        @ParameterizedTest
        @ProvideTestModels
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
                val newAccount = createAccount(derivationIndex = 1)

                PlusTestModel(
                    initial = AccountList(
                        userWalletId = userWalletId,
                        accounts = listOf(mainAccount),
                        totalAccounts = 1,
                    ).getOrNull()!!,
                    toAdd = newAccount,
                    expected = AccountList(
                        userWalletId = userWalletId,
                        accounts = listOf(mainAccount, newAccount),
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
                        userWalletId = userWalletId,
                        accounts = listOf(mainAccount),
                        totalAccounts = 1,
                    ).getOrNull()!!,
                    toAdd = newAccount,
                    expected = AccountList(
                        userWalletId = userWalletId,
                        accounts = listOf(newAccount),
                        totalAccounts = 1,
                    ),
                )
            },
            // endregion
            // region MainAccountNotFound
            run {
                val mainAccount = Account.CryptoPortfolio.createMainAccount(userWalletId)
                val newAccount = Account.CryptoPortfolio(
                    accountId = mainAccount.accountId,
                    accountName = mainAccount.accountName,
                    derivationIndex = DerivationIndex(1).getOrNull()!!,
                    icon = mainAccount.icon,
                    cryptoCurrencies = mainAccount.cryptoCurrencies,
                )

                PlusTestModel(
                    initial = AccountList(
                        userWalletId = userWalletId,
                        accounts = listOf(mainAccount),
                        totalAccounts = 1,
                    ).getOrNull()!!,
                    toAdd = newAccount,
                    expected = AccountList.Error.MainAccountNotFound.left(),
                )
            },
            // endregion
            // region ExceedsMaxMainAccountsCount
            run {
                val mainAccount = Account.CryptoPortfolio.createMainAccount(userWalletId)
                val newAccount = Account.CryptoPortfolio.createMainAccount(UserWalletId("012"))

                PlusTestModel(
                    initial = AccountList(
                        userWalletId = userWalletId,
                        accounts = listOf(mainAccount),
                        totalAccounts = 1,
                    ).getOrNull()!!,
                    toAdd = newAccount,
                    expected = AccountList.Error.ExceedsMaxMainAccountsCount.left(),
                )
            },
            // endregion
            // region DuplicateAccountNames
            run {
                val mainAccount = Account.CryptoPortfolio.createMainAccount(userWalletId)
                val newAccount = createAccount(derivationIndex = 1).copy(accountName = mainAccount.accountName)

                PlusTestModel(
                    initial = AccountList(
                        userWalletId = userWalletId,
                        accounts = listOf(mainAccount),
                        totalAccounts = 1,
                    ).getOrNull()!!,
                    toAdd = newAccount,
                    expected = AccountList.Error.DuplicateAccountNames.left(),
                )
            },
            // endregion
            // region ExceedsMaxAccountsCount
            PlusTestModel(
                initial = AccountList(
                    userWalletId = userWalletId,
                    accounts = createAccounts(count = 20),
                    totalAccounts = 20,
                ).getOrNull()!!,
                toAdd = createAccount(derivationIndex = 21),
                expected = AccountList.Error.ExceedsMaxAccountsCount.left(),
            ),
            // endregion
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

        @ParameterizedTest
        @ProvideTestModels
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
                val secondaryAccount = createAccount(derivationIndex = 2)

                MinusTestModel(
                    initial = AccountList(
                        userWalletId = userWalletId,
                        accounts = listOf(mainAccount, secondaryAccount),
                        totalAccounts = 2,
                    ).getOrNull()!!,
                    toRemove = secondaryAccount,
                    expected = AccountList(
                        userWalletId = userWalletId,
                        accounts = listOf(mainAccount),
                        totalAccounts = 1,
                    ),
                )
            },
            // endregion
            // region Remove unexisting account
            run {
                val mainAccount = Account.CryptoPortfolio.createMainAccount(userWalletId)
                val notInList = createAccount(derivationIndex = 3)

                val accountList = AccountList(
                    userWalletId = userWalletId,
                    accounts = listOf(mainAccount),
                    totalAccounts = 1,
                )

                MinusTestModel(
                    initial = accountList.getOrNull()!!,
                    toRemove = notInList,
                    expected = accountList,
                )
            },
            // endregion
            // region EmptyAccountsList
            run {
                val mainAccount = Account.CryptoPortfolio.createMainAccount(userWalletId)

                MinusTestModel(
                    initial = AccountList(
                        userWalletId = userWalletId,
                        accounts = listOf(mainAccount),
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
                val secondaryAccount = createAccount(derivationIndex = 2)

                MinusTestModel(
                    initial = AccountList(
                        userWalletId = userWalletId,
                        accounts = listOf(mainAccount, secondaryAccount),
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

        val userWalletId = MockAccounts.userWalletId
    }
}