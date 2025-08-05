package com.tangem.domain.account.models

import arrow.core.Either
import arrow.core.left
import com.google.common.truth.Truth
import com.tangem.domain.account.utils.randomAccountId
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWallet
import io.mockk.clearMocks
import io.mockk.every
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

    private fun createAccounts(count: Int): Set<Account.CryptoPortfolio> {
        return buildSet {
            add(createAccount(isMain = true))
            repeat(count - 1) {
                add(createAccount(isMain = false))
            }
        }
    }

    private fun createAccount(
        accountId: AccountId = AccountId(value = randomAccountId(5), userWalletId = mockk()),
        isMain: Boolean = false,
    ): Account.CryptoPortfolio {
        return mockk<Account.CryptoPortfolio> {
            every { this@mockk.accountId } returns accountId
            every { this@mockk.isMainAccount } returns isMain
        }
    }
}