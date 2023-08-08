package com.tangem.domain.tokens.repository

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

/**
 * Repository for everything related to the tokens of user wallet
 * */
interface CurrenciesRepository {

    /**
     * Saves the given list of cryptocurrencies, along with the preferences for grouping and sorting, for a specific
     * multi-currency user wallet.
     *
     * @param userWalletId The unique identifier of the user wallet.
     * @param currencies The list of cryptocurrencies to be saved.
     * @param isGroupedByNetwork A boolean flag indicating whether the tokens should be grouped by network.
     * @param isSortedByBalance A boolean flag indicating whether the tokens should be sorted by balance.
     * @throws com.tangem.domain.core.error.DataError.UserWalletError.WrongUserWallet If single-currency user wallet
     * ID provided.
     */
    suspend fun saveTokens(
        userWalletId: UserWalletId,
        currencies: List<CryptoCurrency>,
        isGroupedByNetwork: Boolean,
        isSortedByBalance: Boolean,
    )

    /**
     * Retrieves the primary cryptocurrency for a specific single-currency user wallet.
     *
     * @param userWalletId The unique identifier of the user wallet.
     * @return The primary cryptocurrency associated with the user wallet.
     * @throws com.tangem.domain.core.error.DataError.UserWalletError.WrongUserWallet If multi-currency user wallet
     * ID provided.
     */
    suspend fun getSingleCurrencyWalletPrimaryCurrency(userWalletId: UserWalletId): CryptoCurrency

    /**
     * Retrieves the list of cryptocurrencies within a multi-currency wallet.
     *
     * @param userWalletId The unique identifier of the user wallet.
     * @param refresh A boolean flag indicating whether the data should be refreshed.
     * @return A [Flow] emitting the set of cryptocurrencies associated with the user wallet.
     * @throws com.tangem.domain.core.error.DataError.UserWalletError.WrongUserWallet If single-currency user wallet
     * ID provided.
     */
    fun getMultiCurrencyWalletCurrencies(userWalletId: UserWalletId, refresh: Boolean): Flow<List<CryptoCurrency>>

    /**
     * Retrieves the cryptocurrency for a specific multi-currency user wallet.
     *
     * @param userWalletId The unique identifier of the user wallet.
     * @param id The unique identifier of the cryptocurrency to be retrieved.
     * @return The cryptocurrency associated with the user wallet and ID.
     * @throws com.tangem.domain.core.error.DataError.UserWalletError.WrongUserWallet If single-currency user wallet
     * ID provided.
     */
    suspend fun getMultiCurrencyWalletCurrency(userWalletId: UserWalletId, id: CryptoCurrency.ID): CryptoCurrency

    /**
     * Determines whether the tokens within a specific multi-currency user wallet are grouped.
     *
     * @param userWalletId The unique identifier of the user wallet.
     * @return A [Flow] emitting a boolean value indicating whether the tokens are grouped.
     * @throws com.tangem.domain.core.error.DataError.UserWalletError.WrongUserWallet If single-currency user wallet
     * ID provided.
     */
    fun isTokensGrouped(userWalletId: UserWalletId): Flow<Boolean>

    /**
     * Determines whether the tokens within a specific multi-currency user wallet are sorted by balance.
     *
     * @param userWalletId The unique identifier of the user wallet.
     * @return A [Flow] emitting a boolean value indicating whether the tokens are sorted by balance.
     * @throws com.tangem.domain.core.error.DataError.UserWalletError.WrongUserWallet If single-currency user wallet
     * ID provided.
     */
    fun isTokensSortedByBalance(userWalletId: UserWalletId): Flow<Boolean>
}
