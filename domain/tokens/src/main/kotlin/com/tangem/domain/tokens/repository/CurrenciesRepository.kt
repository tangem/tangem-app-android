package com.tangem.domain.tokens.repository

import com.tangem.domain.core.error.DataError
import com.tangem.domain.core.lce.LceFlow
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.FeePaidCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWalletId
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
     * Add currencies to a specific user wallet.
     *
     * @param userWalletId The unique identifier of the user wallet.
     * @param currencies The currencies which must be added.
     * @throws DataError.UserWalletError.WrongUserWallet If single-currency user wallet
     * ID provided.
     */
    suspend fun addCurrencies(userWalletId: UserWalletId, currencies: List<CryptoCurrency>)

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
    fun getWalletCurrenciesUpdates(userWalletId: UserWalletId): LceFlow<Throwable, List<CryptoCurrency>>

    /**
     * Retrieves the primary cryptocurrency for a specific single-currency user wallet.
     *
     * @param userWalletId The unique identifier of the user wallet.
     * @return The primary cryptocurrency associated with the user wallet.
     * @throws DataError.UserWalletError.WrongUserWallet If multi-currency user wallet
     * ID provided.
     */
    suspend fun getSingleCurrencyWalletPrimaryCurrency(userWalletId: UserWalletId): CryptoCurrency

    /**
     * Retrieves the cryptocurrencies for a specific single-currency user wallet with tokens on the card.
     *
     * @param userWalletId The unique identifier of the user wallet.
     * @return The primary cryptocurrency associated with the user wallet.
     * @throws DataError.UserWalletError.WrongUserWallet If multi-currency user wallet
     * ID provided.
     */
    suspend fun getSingleCurrencyWalletWithCardCurrencies(userWalletId: UserWalletId): List<CryptoCurrency>

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
     * Retrieves updates of the list of cryptocurrencies within a multi-currency wallet.
     *
     * Loads remote cryptocurrencies if they have expired.
     *
     * @param userWalletId The unique identifier of the user wallet.
     * @return A [LceFlow] emitting the set of cryptocurrencies associated with the user wallet. May emit an
     * [DataError.UserWalletError.WrongUserWallet] if single-currency user wallet ID provided.
     */
    fun getMultiCurrencyWalletCurrenciesUpdatesLce(userWalletId: UserWalletId): LceFlow<Throwable, List<CryptoCurrency>>

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

    fun getMissedAddressesCryptoCurrencies(userWalletId: UserWalletId): Flow<List<CryptoCurrency>>

    /**
     * Determines whether the currency sending is blocked by network pending transaction
     *
     * @param cryptoCurrencyStatus currency status
     * @param coinStatus main currency status in [cryptoCurrencyStatus] network
     */
    fun isSendBlockedByPendingTransactions(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        coinStatus: CryptoCurrencyStatus?,
    ): Boolean

    /**
     * Retrieves fee paid currency for specific [currency].
     */
    suspend fun getFeePaidCurrency(userWalletId: UserWalletId, currency: CryptoCurrency): FeePaidCurrency

    /**
     * Creates token [cryptoCurrency] based on current token and [network] it`s will be added
     */
    fun createTokenCurrency(cryptoCurrency: CryptoCurrency.Token, network: Network): CryptoCurrency.Token

    /**
     * Creates token [cryptoCurrency] based on [contractAddress] and [networkId] it`s will be added
     */
    suspend fun createTokenCurrency(
        userWalletId: UserWalletId,
        contractAddress: String,
        networkId: String,
    ): CryptoCurrency.Token

    suspend fun hasTokens(userWalletId: UserWalletId, network: Network): Boolean
}
