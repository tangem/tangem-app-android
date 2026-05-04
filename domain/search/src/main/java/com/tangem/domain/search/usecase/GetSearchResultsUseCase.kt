package com.tangem.domain.search.usecase

import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.MultiAccountStatusListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.account.filterCryptoPortfolio
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.search.model.SearchResult
import com.tangem.domain.search.model.UserAssetSearchEntry
import com.tangem.domain.search.repository.SearchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Primary search use case that produces [SearchResult] based on the current query.
 *
 * Behavior depends on the query:
 * - **Empty query** — returns search history: text hints and recently viewed tokens.
 * - **Non-empty query** — performs the search across all unlocked user wallets, matching currencies by name or symbol.
 *
 * @property searchRepository            local search history storage
 * @property multiAccountStatusListSupplier supplier for loaded account status lists across all wallets
 * @property userWalletsListRepository    repository providing the list of user wallets
 */
class GetSearchResultsUseCase(
    private val searchRepository: SearchRepository,
    private val multiAccountStatusListSupplier: MultiAccountStatusListSupplier,
    private val userWalletsListRepository: UserWalletsListRepository,
) {

    /**
     * Produces a [Flow] of [SearchResult] for the given [query].
     *
     * @param query        the search query string; blank means "show history"
     */
    operator fun invoke(query: String): Flow<SearchResult> {
        return if (query.isBlank()) {
            observeHistory()
        } else {
            observeUserAssets(query)
        }
    }

    private fun observeHistory(): Flow<SearchResult> {
        return combine(
            searchRepository.getTextHints(),
            searchRepository.getRecentTokens(),
        ) { hints, tokens ->
            SearchResult(
                textHints = hints,
                recentTokens = tokens,
                userAssets = emptyList(),
            )
        }
    }

    private fun observeUserAssets(query: String): Flow<SearchResult> {
        val lowerQuery = query.lowercase()
        return combine(
            multiAccountStatusListSupplier(),
            userWalletsListRepository.userWallets,
        ) { statusLists, wallets ->
            val unlockedWallets = wallets
                .orEmpty()
                .filterNot(UserWallet::isLocked)
                .associateBy { it.walletId }

            if (unlockedWallets.isEmpty()) return@combine emptyList()

            statusLists
                .filter { it.userWalletId in unlockedWallets }
                .flatMap { statusList -> extractMatchingAssets(statusList, unlockedWallets, lowerQuery) }
        }.map { userAssets ->
            SearchResult(
                textHints = emptyList(),
                recentTokens = emptyList(),
                userAssets = userAssets,
            )
        }
    }

    private fun extractMatchingAssets(
        statusList: AccountStatusList,
        wallets: Map<UserWalletId, UserWallet>,
        lowerQuery: String,
    ): List<UserAssetSearchEntry> {
        val wallet = wallets[statusList.userWalletId] ?: return emptyList()
        return statusList.accountStatuses
            .filterCryptoPortfolio()
            .flatMap { accountStatus ->
                accountStatus.flattenCurrencies()
                    .filter { currencyStatus ->
                        val name = currencyStatus.currency.name.lowercase()
                        val symbol = currencyStatus.currency.symbol.lowercase()
                        name.contains(lowerQuery) || symbol.contains(lowerQuery)
                    }
                    .map { currencyStatus ->
                        UserAssetSearchEntry(
                            userWalletId = statusList.userWalletId,
                            userWalletName = wallet.name,
                            accountId = accountStatus.accountId,
                            accountName = accountStatus.account.accountName,
                            currencyStatus = currencyStatus,
                        )
                    }
            }
    }
}