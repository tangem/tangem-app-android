package com.tangem.domain.tokens.repository

import com.tangem.domain.card.CardTypesResolver
import com.tangem.domain.core.error.DataError
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.FeePaidCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

/**
 * Repository for everything related to the tokens of user wallet
 * */
@Suppress("TooManyFunctions")
interface CurrenciesRepository {

    /**
     * Saves the given list of cryptocurrencies, along with the preferences for grouping and sorting, for a specific
     * multi-currency user wallet.
     *
     * @param userWalletId The unique identifier of the user wallet.
     * @param currencies The list of cryptocurrencies to be saved.
     * @param isGroupedByNetwork A boolean flag indicating whether the tokens should be grouped by network.
     * @param isSortedByBalance A boolean flag indicating whether the tokens should be sorted by balance.
     * @throws DataError.UserWalletError.WrongUserWallet If single-currency user wallet
     * ID provided.
     */
    suspend fun saveTokens(
        userWalletId: UserWalletId,
        currencies: List<CryptoCurrency>,
        isGroupedByNetwork: Boolean,
        isSortedByBalance: Boolean,
    )

    /**
     * Saves the given list of cryptocurrencies for a specific multi-currency user wallet.
     *
     * @param userWalletId The unique identifier of the user wallet.
     * @param currencies The list of cryptocurrencies to be saved.
     */
    suspend fun saveNewCurrenciesList(userWalletId: UserWalletId, currencies: List<CryptoCurrency>)

    /**
     * Add currencies to a specific user wallet.
     *
     * @param userWalletId The unique identifier of the user wallet.
     * @param currencies The currencies which must be added.
     * @throws DataError.UserWalletError.WrongUserWallet If single-currency user wallet
     * ID provided.
     */
    suspend fun addCurrencies(userWalletId: UserWalletId, currencies: List<CryptoCurrency>): List<CryptoCurrency>

    /**
     * Saves the given list of cryptocurrencies for a specific multi-currency user wallet.
     *
     * @param userWalletId The unique identifier of the user wallet.
     * @param currencies The list of cryptocurrencies to be saved.
     */
    @Deprecated("Tech debt")
    suspend fun saveNewCurrenciesListCache(userWalletId: UserWalletId, currencies: List<CryptoCurrency>)

    /**
     * Add currencies to a specific user wallet.
     *
     * @param userWalletId The unique identifier of the user wallet.
     * @param currencies The currencies which must be added.
     * @throws DataError.UserWalletError.WrongUserWallet If single-currency user wallet
     * ID provided.
     */
    @Deprecated("Tech debt")
    suspend fun addCurrenciesCache(userWalletId: UserWalletId, currencies: List<CryptoCurrency>): List<CryptoCurrency>

    /**
     * Removes currency from a specific user wallet.
     *
     * @param userWalletId The unique identifier of the user wallet.
     * @param currency The currency which must be removed.
     * @throws DataError.UserWalletError.WrongUserWallet If multi-currency user wallet
     * ID provided.
     */
    suspend fun removeCurrency(userWalletId: UserWalletId, currency: CryptoCurrency)

    /**
     * Removes currencies from a specific user wallet.
     *
     * @param userWalletId The unique identifier of the user wallet.
     * @param currencies The currencies which must be removed.
     * @throws DataError.UserWalletError.WrongUserWallet If single-currency user wallet
     * ID provided.
     */
    suspend fun removeCurrencies(userWalletId: UserWalletId, currencies: List<CryptoCurrency>)

    /**
     * Retrieves the list of cryptocurrencies within a user wallet.
     *
     * This method returns a list of cryptocurrencies associated with the user wallet regardless of whether
     * it is a multi-currency or single-currency wallet.
     *
     * @param userWalletId The unique identifier of the user wallet.
     * @return A list of [CryptoCurrency].
     */
    fun getWalletCurrenciesUpdates(userWalletId: UserWalletId): Flow<List<CryptoCurrency>>

    /**
     * Retrieves the primary cryptocurrency for a specific single-currency user wallet.
     *
     * @param userWalletId The unique identifier of the user wallet.
     * @param refresh Indicates whether to force a refresh of the status data.
     * @return The primary cryptocurrency associated with the user wallet.
     * @throws DataError.UserWalletError.WrongUserWallet If multi-currency user wallet
     * ID provided.
     */
    suspend fun getSingleCurrencyWalletPrimaryCurrency(
        userWalletId: UserWalletId,
        refresh: Boolean = false,
    ): CryptoCurrency

    /**
     * Retrieves the cryptocurrencies for a specific single-currency user wallet with tokens on the card.
     *
     * @param userWalletId The unique identifier of the user wallet.
     * @param refresh Indicates whether to force a refresh of the status data.
     * @return The primary cryptocurrency associated with the user wallet.
     * @throws DataError.UserWalletError.WrongUserWallet If multi-currency user wallet
     * ID provided.
     */
    suspend fun getSingleCurrencyWalletWithCardCurrencies(
        userWalletId: UserWalletId,
        refresh: Boolean = false,
    ): List<CryptoCurrency>

    /**
     * Retrieves the cryptocurrency for a specific single-currency user old wallet
     * that stores token on card
     *
     * @param userWalletId The unique identifier of the user wallet.
     * @param id The unique identifier of the cryptocurrency to be retrieved.
     * @return The cryptocurrency associated with the user wallet and ID.
     * @throws DataError.UserWalletError.WrongUserWallet If single-currency user wallet
     * ID provided.
     */
    suspend fun getSingleCurrencyWalletWithCardCurrency(
        userWalletId: UserWalletId,
        id: CryptoCurrency.ID,
    ): CryptoCurrency

    /**
     * Retrieves updates of the list of cryptocurrencies within a multi-currency wallet.
     *
     * Loads remote cryptocurrencies if they have expired.
     *
     * @param userWalletId The unique identifier of the user wallet.
     * @return A [Flow] emitting the set of cryptocurrencies associated with the user wallet.
     * @throws DataError.UserWalletError.WrongUserWallet If single-currency user wallet
     * ID provided.
     */
    fun getMultiCurrencyWalletCurrenciesUpdates(userWalletId: UserWalletId): Flow<List<CryptoCurrency>>

    /**
     * Retrieves the list of cryptocurrencies within a multi-currency wallet.
     *
     * Loads cryptocurrencies if they have expired or if [refresh] is `true`.
     *
     * @param userWalletId The unique identifier of the user wallet.
     * @param refresh A boolean flag indicating whether the data should be refreshed.
     * @return A list of [CryptoCurrency].
     * @throws DataError.UserWalletError.WrongUserWallet If single-currency user wallet
     * ID provided.
     */
    suspend fun getMultiCurrencyWalletCurrenciesSync(
        userWalletId: UserWalletId,
        refresh: Boolean = false,
    ): List<CryptoCurrency>

    /**
     * Retrieves the list of cryptocurrencies within a multi-currency wallet.
     * Returns previously loaded currencies or empty list
     *
     * @param userWalletId The unique identifier of the user wallet.
     * @return A list of [CryptoCurrency].
     * @throws DataError.UserWalletError.WrongUserWallet If single-currency user wallet
     * ID provided.
     */
    suspend fun getMultiCurrencyWalletCachedCurrenciesSync(userWalletId: UserWalletId): List<CryptoCurrency>

    /**
     * Retrieves the cryptocurrency for a specific multi-currency user wallet.
     *
     * @param userWalletId The unique identifier of the user wallet.
     * @param id The unique identifier of the cryptocurrency to be retrieved.
     * @return The cryptocurrency associated with the user wallet and ID.
     * @throws DataError.UserWalletError.WrongUserWallet If single-currency user wallet
     * ID provided.
     */
    suspend fun getMultiCurrencyWalletCurrency(userWalletId: UserWalletId, id: CryptoCurrency.ID): CryptoCurrency

    /**
     * Retrieves the cryptocurrency for a specific multi-currency user wallet.
     *
     * @param userWalletId The unique identifier of the user wallet.
     * @param id The unique identifier of the cryptocurrency to be retrieved.
     * @return The cryptocurrency associated with the user wallet and ID.
     * @throws DataError.UserWalletError.WrongUserWallet If single-currency user wallet
     * ID provided.
     */
    suspend fun getMultiCurrencyWalletCurrency(userWalletId: UserWalletId, id: String): CryptoCurrency

    /**
     * Get the coin for a specific network.
     *
     * @param userWalletId The unique identifier of the user wallet.
     * @param networkId    The unique identifier of the network.
     * @param derivationPath currency derivation path.
     */
    suspend fun getNetworkCoin(
        userWalletId: UserWalletId,
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
    ): CryptoCurrency.Coin

    /**
     * Determines whether the tokens within a specific multi-currency user wallet are grouped.
     *
     * @param userWalletId The unique identifier of the user wallet.
     * @return A [Flow] emitting a boolean value indicating whether the tokens are grouped.
     * @throws DataError.UserWalletError.WrongUserWallet If single-currency user wallet
     * ID provided.
     */
    fun isTokensGrouped(userWalletId: UserWalletId): Flow<Boolean>

    /**
     * Determines whether the tokens within a specific multi-currency user wallet are sorted by balance.
     *
     * @param userWalletId The unique identifier of the user wallet.
     * @return A [Flow] emitting a boolean value indicating whether the tokens are sorted by balance.
     * @throws DataError.UserWalletError.WrongUserWallet If single-currency user wallet
     * ID provided.
     */
    fun isTokensSortedByBalance(userWalletId: UserWalletId): Flow<Boolean>

    /**
     * Determines whether the currency sending is blocked by network pending transaction
     *
     * @param userWalletId         the unique identifier of the user wallet
     * @param cryptoCurrencyStatus currency status
     */
    suspend fun isSendBlockedByPendingTransactions(
        userWalletId: UserWalletId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): Boolean

    /**
     * Retrieves fee paid currency for specific [network].
     */
    suspend fun getFeePaidCurrency(userWalletId: UserWalletId, network: Network): FeePaidCurrency

    /**
     * Creates token [cryptoCurrency] based on current token and [network] it`s will be added
     */
    fun createTokenCurrency(cryptoCurrency: CryptoCurrency.Token, network: Network): CryptoCurrency.Token

    /** Creates token [CryptoCurrency.Token] based on [contractAddress] and [networkId] for specified [userWalletId] */
    suspend fun createTokenCurrency(
        userWalletId: UserWalletId,
        contractAddress: String,
        networkId: String,
    ): CryptoCurrency.Token

    /** Get crypto currencies by [currencyRawId] from all user wallets */
    fun getAllWalletsCryptoCurrencies(currencyRawId: CryptoCurrency.RawID): Flow<Map<UserWallet, List<CryptoCurrency>>>

    fun isNetworkFeeZero(userWalletId: UserWalletId, network: Network): Boolean

    /**
     * Synchronizes local tokens with remote data for a specific user wallet.
     * This method ensures that the local token list matches the remote state by fetching
     * the token data from the local cache and push it to backend.
     *
     * @param userWalletId The unique identifier of the user wallet to sync tokens for.
     * @throws Exception if the sync request to the backend fails
     */
    @Throws
    suspend fun syncTokens(userWalletId: UserWalletId)

    @Throws
    fun getCardTypesResolver(userWalletId: UserWalletId): CardTypesResolver?
}