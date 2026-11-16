package com.tangem.data.account.store

import com.tangem.data.account.utils.assignTokens
import com.tangem.data.common.currency.WalletTokensLocalStore
import com.tangem.data.common.tokens.UserTokensBackwardCompatibility
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.datasource.api.tangemTech.models.account.flattenTokens
import com.tangem.domain.models.wallet.UserWalletId
import javax.inject.Inject

internal class DefaultWalletTokensLocalStore @Inject constructor(
    private val accountsResponseStoreFactory: AccountsResponseStoreFactory,
) : WalletTokensLocalStore {

    private val userTokensBackwardCompatibility = UserTokensBackwardCompatibility()

    override suspend fun addRemoteTokens(userWalletId: UserWalletId, tokens: List<UserTokensResponse.Token>) {
        val compatibleTokens = userTokensBackwardCompatibility.applyCompatibilityAndGetUpdated(tokens)

        accountsResponseStoreFactory.create(userWalletId).updateData { saved ->
            checkNotNull(saved) { "Remote tokens of $userWalletId are not stored: no saved accounts document" }
                .withRemoteTokens(userWalletId = userWalletId, tokens = compatibleTokens)
        }
    }

    private fun GetWalletAccountsResponse.withRemoteTokens(
        userWalletId: UserWalletId,
        tokens: List<UserTokensResponse.Token>,
    ): GetWalletAccountsResponse {
        val (unassignedTokens, accountTokens) = tokens.partition { it.accountId == null }
        val savedAccountIds = accounts.map { it.id }.toSet()
        val orphanTokensCount = accountTokens.count { it.accountId !in savedAccountIds }

        check(orphanTokensCount == 0) {
            "$orphanTokensCount remote tokens of $userWalletId belong to accounts missing locally"
        }

        val withAccountTokens = copy(accounts = accounts.withAddedTokens(accountTokens))
        val savedTokens = withAccountTokens.flattenTokens().toSet()

        return withAccountTokens.copy(
            accounts = withAccountTokens.accounts.assignTokens(
                userWalletId = userWalletId,
                tokens = unassignedTokens.filterNot { it in savedTokens },
            ),
        )
    }

    private fun List<WalletAccountDTO>.withAddedTokens(tokens: List<UserTokensResponse.Token>): List<WalletAccountDTO> {
        val tokensByAccountId = tokens.groupBy { it.accountId }

        return map { account ->
            val savedTokens = account.tokens.orEmpty()
            val savedTokenSet = savedTokens.toSet()
            val addedTokens = tokensByAccountId[account.id].orEmpty().filterNot { it in savedTokenSet }

            if (addedTokens.isEmpty()) account else account.copy(tokens = savedTokens + addedTokens)
        }
    }
}