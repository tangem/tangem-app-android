package com.tangem.data.account.utils

import com.google.common.truth.Truth
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.wallet.UserWalletId
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetWalletAccountsResponseExtTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class FlattenTokens {

        @Test
        fun `flattenTokens returns empty list when accounts are empty`() {
            // Arrange
            val response = GetWalletAccountsResponse(
                wallet = GetWalletAccountsResponse.Wallet(
                    version = 1,
                    group = UserTokensResponse.GroupType.NONE,
                    sort = UserTokensResponse.SortType.MANUAL,
                    totalAccounts = 0,
                ),
                accounts = emptyList(),
                unassignedTokens = emptyList(),
            )

            // Act
            val actual = response.flattenTokens()

            // Assert
            Truth.assertThat(actual).isEmpty()
        }

        @Test
        fun `flattenTokens returns empty list when single account has empty tokens`() {
            // Arrange
            val account = createWalletAccountDTO(derivationIndex = 0)

            val response = GetWalletAccountsResponse(
                wallet = GetWalletAccountsResponse.Wallet(
                    version = 1,
                    group = UserTokensResponse.GroupType.NONE,
                    sort = UserTokensResponse.SortType.MANUAL,
                    totalAccounts = 1,
                ),
                accounts = listOf(account),
                unassignedTokens = emptyList(),
            )

            // Act
            val actual = response.flattenTokens()

            // Assert
            Truth.assertThat(actual).isEmpty()
        }

        @Test
        fun `flattenTokens returns all tokens from multiple accounts`() {
            // Arrange
            val token1 = createUserToken(id = "0")
            val token2 = createUserToken(id = "1")
            val token3 = createUserToken(id = "2")

            val account1 = createWalletAccountDTO(derivationIndex = 0, tokens = listOf(token1, token2))
            val account2 = createWalletAccountDTO(derivationIndex = 1, tokens = listOf(token3))
            val account3 = createWalletAccountDTO(derivationIndex = 2)

            val response = GetWalletAccountsResponse(
                wallet = GetWalletAccountsResponse.Wallet(
                    version = 1,
                    group = UserTokensResponse.GroupType.NONE,
                    sort = UserTokensResponse.SortType.MANUAL,
                    totalAccounts = 2,
                ),
                accounts = listOf(account1, account2, account3),
                unassignedTokens = emptyList(),
            )

            // Act
            val actual = response.flattenTokens()

            // Assert
            Truth.assertThat(actual).containsExactly(token1, token2, token3)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ToUserTokensResponse {

        @Test
        fun `toUserTokensResponse returns correct UserTokensResponse for empty accounts and unassignedTokens`() {
            // Arrange
            val response = GetWalletAccountsResponse(
                wallet = GetWalletAccountsResponse.Wallet(
                    version = 1,
                    group = UserTokensResponse.GroupType.NONE,
                    sort = UserTokensResponse.SortType.MANUAL,
                    totalAccounts = 0,
                ),
                accounts = emptyList(),
                unassignedTokens = emptyList(),
            )

            // Act
            val actual = response.toUserTokensResponse()

            // Assert
            val expected = UserTokensResponse(
                group = UserTokensResponse.GroupType.NONE,
                sort = UserTokensResponse.SortType.MANUAL,
                tokens = emptyList(),
            )

            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `toUserTokensResponse includes tokens from accounts and unassignedTokens`() {
            // Arrange
            val token1 = createUserToken(id = "0")
            val token2 = createUserToken(id = "1")
            val account = createWalletAccountDTO(derivationIndex = 0, tokens = listOf(token1))
            val unassignedToken = token2

            val response = GetWalletAccountsResponse(
                wallet = GetWalletAccountsResponse.Wallet(
                    version = 1,
                    group = UserTokensResponse.GroupType.NETWORK,
                    sort = UserTokensResponse.SortType.BALANCE,
                    totalAccounts = 1,
                ),
                accounts = listOf(account),
                unassignedTokens = listOf(unassignedToken),
            )

            // Act
            val actual = response.toUserTokensResponse()

            // Assert
            val expected = UserTokensResponse(
                group = UserTokensResponse.GroupType.NETWORK,
                sort = UserTokensResponse.SortType.BALANCE,
                tokens = listOf(token1),
            )

            Truth.assertThat(actual).isEqualTo(expected)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetWalletAccountsResponseAssignTokens {

        @Test
        fun `assignTokens correctly assigns tokens to accounts`() {
            // Arrange
            val accountId = "957B88B12730E646E0F33D3618B77DFA579E8231E3C59C7104BE7165611C8027"
            val token1 = createUserToken(id = "0", accountId = null)
            val token2 = createUserToken(id = "1", accountId = null)
            val account1 = createWalletAccountDTO(derivationIndex = 0)
            val account2 = createWalletAccountDTO(derivationIndex = 1)
            val response = GetWalletAccountsResponse(
                wallet = GetWalletAccountsResponse.Wallet(
                    version = 1,
                    group = UserTokensResponse.GroupType.NONE,
                    sort = UserTokensResponse.SortType.MANUAL,
                    totalAccounts = 2,
                ),
                accounts = listOf(account1, account2),
                unassignedTokens = listOf(token1, token2),
            )

            // Act
            val actual = response.assignTokens(userWalletId)

            // Assert
            val expected = GetWalletAccountsResponse(
                wallet = response.wallet,
                accounts = listOf(
                    account1.copy(
                        tokens = listOf(
                            token1.copy(accountId = accountId),
                            token2.copy(accountId = accountId),
                        ),
                    ),
                    account2,
                ),
                unassignedTokens = emptyList(),
            )

            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `assignTokens does not change accounts if there are no unassignedTokens`() {
            // Arrange
            val account = createWalletAccountDTO(derivationIndex = 0)
            val response = GetWalletAccountsResponse(
                wallet = GetWalletAccountsResponse.Wallet(
                    version = 1,
                    group = UserTokensResponse.GroupType.NONE,
                    sort = UserTokensResponse.SortType.MANUAL,
                    totalAccounts = 1,
                ),
                accounts = listOf(account),
                unassignedTokens = emptyList(),
            )

            // Act
            val actual = response.assignTokens(userWalletId)

            // Assert
            val expected = GetWalletAccountsResponse(
                wallet = response.wallet,
                accounts = listOf(account),
                unassignedTokens = emptyList(),
            )

            Truth.assertThat(actual).isEqualTo(expected)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class WalletAccountDTOListAssignTokens {

        @Test
        fun `assignTokens correctly assigns tokens to accounts`() {
            // Arrange
            val accountId = "957B88B12730E646E0F33D3618B77DFA579E8231E3C59C7104BE7165611C8027"
            val token1 = createUserToken(id = "0", accountId = null)
            val token2 = createUserToken(id = "1", accountId = null)
            val account1 = createWalletAccountDTO(derivationIndex = 0)
            val account2 = createWalletAccountDTO(derivationIndex = 1)

            // Act
            val actual = listOf(account1, account2).assignTokens(
                userWalletId = userWalletId,
                tokens = listOf(token1, token2),
            )

            // Assert
            val expected = listOf(
                account1.copy(
                    tokens = listOf(
                        token1.copy(accountId = accountId),
                        token2.copy(accountId = accountId),
                    ),
                ),
                account2,
            )

            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `assignTokens does not change accounts if there are no unassignedTokens`() {
            // Arrange
            val accountId = "957B88B12730E646E0F33D3618B77DFA579E8231E3C59C7104BE7165611C8027"
            val token1 = createUserToken(id = "0", accountId = accountId)
            val account1 = createWalletAccountDTO(derivationIndex = 0)

            // Act
            val actual = listOf(account1).assignTokens(
                userWalletId = userWalletId,
                tokens = listOf(token1),
            )

            // Assert
            val expected = listOf(
                account1.copy(
                    tokens = listOf(
                        token1.copy(accountId = accountId),
                    ),
                ),
            )

            Truth.assertThat(actual).isEqualTo(expected)
        }
    }

    private fun createWalletAccountDTO(derivationIndex: Int, tokens: List<UserTokensResponse.Token> = emptyList()) =
        WalletAccountDTO(
            id = AccountId.forCryptoPortfolio(
                userWalletId = userWalletId,
                derivationIndex = DerivationIndex(derivationIndex).getOrNull()!!,
            ).value,
            name = "Name #$derivationIndex",
            derivationIndex = derivationIndex,
            icon = "icon",
            iconColor = "color",
            tokens = tokens,
            totalTokens = tokens.size,
            totalNetworks = 1,
        )

    private fun createUserToken(id: String, accountId: String? = "account_id") = UserTokensResponse.Token(
        id = id,
        accountId = accountId,
        networkId = "ethereum",
        derivationPath = "m/44'/60'/0'/0/0",
        name = "Token",
        symbol = "T",
        contractAddress = "0x$id",
        decimals = 18,
    )

    private companion object {

        val userWalletId = UserWalletId("011")
    }
}