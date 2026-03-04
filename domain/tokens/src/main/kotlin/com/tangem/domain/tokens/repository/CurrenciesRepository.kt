package com.tangem.domain.tokens.repository

import com.tangem.domain.card.CardTypesResolver
import com.tangem.domain.core.error.DataError
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.model.FeePaidCurrency
import kotlinx.coroutines.flow.Flow

/**
 * Repository for everything related to the tokens of user wallet
 * */
@Suppress("TooManyFunctions")
interface CurrenciesRepository {

    /**
     * Retrieves the list of cryptocurrencies within a user wallet.
     *
     * This method returns a list of cryptocurrencies associated with the user wallet regardless of whether
     * it is a multi-currency or single-currency wallet.
     *
     * @param userWalletId The unique identifier of the user wallet.
     * @return A list of [CryptoCurrency].
     */
    @Deprecated("Use MultiWalletCryptoCurrenciesSupplier")
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

    fun createCoinCurrency(network: Network): CryptoCurrency.Coin

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

    fun isNetworkFeeZero(userWalletId: UserWalletId, network: Network): Boolean

    @Throws
    fun getCardTypesResolver(userWalletId: UserWalletId): CardTypesResolver?
}