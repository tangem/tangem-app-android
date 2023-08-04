package com.tangem.domain.tokens.repository

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

/**
 * Repository for everything related to the tokens of user wallet
 * */
interface TokensRepository {

    /**
     * Saves the given set of cryptocurrencies, along with the preferences for grouping and sorting, for a specific
     * multi-currency user wallet.
     *
     * @param userWalletId The unique identifier of the user wallet.
     * @param currencies The set of cryptocurrencies to be saved.
     * @param isGroupedByNetwork A boolean flag indicating whether the tokens should be grouped by network.
     * @param isSortedByBalance A boolean flag indicating whether the tokens should be sorted by balance.
     */
    suspend fun saveTokens(
        userWalletId: UserWalletId,
        currencies: Set<CryptoCurrency>,
        isGroupedByNetwork: Boolean,
        isSortedByBalance: Boolean,
    )

    /**
     * Retrieves the primary cryptocurrency for a specific single-currency user wallet.
     *
     * @param userWalletId The unique identifier of the user wallet.
     * @return The primary cryptocurrency associated with the user wallet.
     */
    suspend fun getPrimaryCurrency(userWalletId: UserWalletId): CryptoCurrency

    /**
     * Retrieves the set of cryptocurrencies within a multi-currency wallet.
     *
     * @param userWalletId The unique identifier of the user wallet.
     * @param refresh A boolean flag indicating whether the data should be refreshed.
     * @return A [Flow] emitting the set of cryptocurrencies associated with the user wallet.
     */
    fun getMultiCurrencyWalletCurrencies(userWalletId: UserWalletId, refresh: Boolean): Flow<Set<CryptoCurrency>>

    /**
     * Determines whether the tokens within a specific multi-currency user wallet are grouped.
     *
     * @param userWalletId The unique identifier of the user wallet.
     * @return A [Flow] emitting a boolean value indicating whether the tokens are grouped.
     */
    fun isTokensGrouped(userWalletId: UserWalletId): Flow<Boolean>

    /**
     * Determines whether the tokens within a specific multi-currency user wallet are sorted by balance.
     *
     * @param userWalletId The unique identifier of the user wallet.
     * @return A [Flow] emitting a boolean value indicating whether the tokens are sorted by balance.
     */
    fun isTokensSortedByBalance(userWalletId: UserWalletId): Flow<Boolean>
}
