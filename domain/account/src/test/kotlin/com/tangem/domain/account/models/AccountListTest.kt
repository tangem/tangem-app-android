package com.tangem.domain.account.models

import arrow.core.Either
import arrow.core.left
import com.google.common.truth.Truth
import com.tangem.domain.account.utils.randomAccountId
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.wallet.UserWallet
import io.mockk.clearMocks
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.random.Random

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountListTest {

    @Test
    fun mainAccount() {
        // Arrange
        val mainAccount = createAccount(isMain = true)

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
            accounts = createAccounts(count = 2),
            totalAccounts = 2,
        ).getOrNull()!!

        val fullAccountList = AccountList(
            userWallet = mockk(),
            accounts = createAccounts(20),
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
                accounts = setOf(createAccount(isMain = false)),
                expected = AccountList.Error.MainAccountNotFound.left(),
            ),
            CreateTestModel(
                accounts = setOf(
                    createAccount(isMain = true),
                    createAccount(isMain = true),
                ),
                expected = AccountList.Error.ExceedsMaxMainAccountsCount.left(),
            ),
            createAccount(isMain = true).let {
                CreateTestModel(
                    accounts = setOf(it),
                    expected = AccountList(
                        userWallet = userWallet,
                        accounts = setOf(it),
                        totalAccounts = 1,
                    ),
                )
            },
            createAccounts(count = 20).let {
                CreateTestModel(
                    accounts = it,
                    expected = AccountList(
                        userWallet = userWallet,
                        accounts = it,
                        totalAccounts = 20,
                    ),
                )
            },
            CreateTestModel(
                accounts = createAccounts(21),
                expected = AccountList.Error.ExceedsMaxAccountsCount.left(),
            ),
            CreateTestModel(
                accounts = setOf(
                    createAccount(
                        accountId = AccountId(value = "1", userWalletId = mockk()),
                        isMain = true,
                    ),
                    createAccount(
                        accountId = AccountId(value = "1", userWalletId = mockk()),
                        isMain = false,
                    ),
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
                val mainAccount = createAccount(
                    accountId = AccountId(value = "1", userWalletId = mockk()),
                    isMain = true,
                )

                val newAccount = createAccount(isMain = false)

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
                val mainAccount = createAccount(
                    accountId = AccountId(value = "1", userWalletId = mockk()),
                    isMain = true,
                )

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
                    accounts = createAccounts(20),
                    totalAccounts = 20,
                ).getOrNull()!!,
                toAdd = createAccount(isMain = false),
                expected = AccountList.Error.ExceedsMaxAccountsCount.left(),
            ),
            PlusTestModel(
                initial = AccountList(
                    userWallet = userWallet,
                    accounts = setOf(createAccount(isMain = true)),
                    totalAccounts = 1,
                ).getOrNull()!!,
                toAdd = createAccount(isMain = true),
                expected = AccountList.Error.ExceedsMaxMainAccountsCount.left(),
            ),
            PlusTestModel(
                initial = AccountList(
                    userWallet = userWallet,
                    accounts = setOf(
                        createAccount(isMain = true),
                        createAccount(isMain = false),
                    ),
                    totalAccounts = 2,
                ).getOrNull()!!,
                toAdd = createAccount(isMain = true),
                expected = AccountList.Error.ExceedsMaxMainAccountsCount.left(),
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
                val mainAccount = createAccount(
                    accountId = AccountId(value = "1", userWalletId = mockk()),
                    isMain = true,
                )

                val secondaryAccount = createAccount(
                    accountId = AccountId(value = "2", userWalletId = mockk()),
                    isMain = false,
                )

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
                val mainAccount = createAccount(
                    accountId = AccountId(value = "1", userWalletId = mockk()),
                    isMain = true,
                )
                val notInList = createAccount(
                    accountId = AccountId(value = "3", userWalletId = mockk()),
                    isMain = false,
                )
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
                val mainAccount = createAccount(isMain = true)

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
                val mainAccount = createAccount(
                    accountId = AccountId(value = "1", userWalletId = mockk()),
                    isMain = true,
                )
                val secondaryAccount = createAccount(
                    accountId = AccountId(value = "2", userWalletId = mockk()),
                    isMain = false,
                )
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

    private fun createAccounts(count: Int): Set<Account.CryptoPortfolio> {
        return buildSet {
            add(createAccount(isMain = true))
            repeat(count - 1) {
                add(createAccount(isMain = false))
            }
        }
    }

    private fun createAccount(
        accountId: AccountId = AccountId(value = randomAccountId(length = 5), userWalletId = mockk()),
        accountIcon: CryptoPortfolioIcon = CryptoPortfolioIcon.ofDefaultCustomAccount(),
        isMain: Boolean,
    ): Account.CryptoPortfolio {
        return Account.CryptoPortfolio(
            accountId = accountId,
            name = "Test Account",
            accountIcon = accountIcon,
            derivationIndex = if (isMain) 0 else Random.nextInt(1, 21),
            isArchived = false,
            cryptoCurrencyList = Account.CryptoPortfolio.CryptoCurrencyList(
                currencies = emptySet(),
                sortType = TokensSortType.NONE,
                groupType = TokensGroupType.NONE,
            ),
        )
            .getOrNull()!!
    }
}