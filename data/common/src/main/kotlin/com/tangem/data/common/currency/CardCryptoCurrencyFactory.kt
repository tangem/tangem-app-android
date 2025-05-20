package com.tangem.data.common.currency

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId

/**
 * Factory for creating list of [CryptoCurrency] for selected card
 *
[REDACTED_AUTHOR]
 */
interface CardCryptoCurrencyFactory {

    /**
     * Universal method for creating list of [CryptoCurrency] in [network] for any card
     *
     * @param userWalletId user wallet id that determines type of card
     * @param network      network
     */
    @Throws
    suspend fun create(userWalletId: UserWalletId, network: Network): List<CryptoCurrency>

    /**
     * Create currencies for multi currency card
     *
     * @param userWallet user wallet
     * @param networks   networks
     */
    @Throws
    suspend fun createCurrenciesForMultiCurrencyCard(
        userWallet: UserWallet,
        networks: Set<Network>,
    ): Map<Network, List<CryptoCurrency>>

    /**
     * Create default coins for multi currency card
     *
     * @param scanResponse scan response
     */
    fun createDefaultCoinsForMultiCurrencyCard(scanResponse: ScanResponse): List<CryptoCurrency.Coin>

    /**
     * Create primary currency for single currency card
     *
     * @param scanResponse scan response
     */
    @Throws
    fun createPrimaryCurrencyForSingleCurrencyCard(scanResponse: ScanResponse): CryptoCurrency

    /**
     * Create currencies for single currency card with token (like, NODL)
     *
     * @param scanResponse scan response
     */
    @Throws
    fun createCurrenciesForSingleCurrencyCardWithToken(scanResponse: ScanResponse): List<CryptoCurrency>
}