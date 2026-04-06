package com.tangem.domain.tokens.repository

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.model.FeePaidCurrency

/**
 * Repository for everything related to the tokens of user wallet
 * */
interface CurrenciesRepository {

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
}