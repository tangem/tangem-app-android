package com.tangem.datasource.exchangeservice.hotcrypto

import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.tokens.model.CryptoCurrency

/**
 * Hot crypto loader
 *
 * @author Andrew Khokhlov on 19/01/2025
 */
interface HotCryptoLoader {

    /**
     * Fetch hot crypto
     *
     * @param tokens already added tokens
     */
    suspend fun fetch(tokens: List<UserTokensResponse.Token>)

    /**
     * Update hot crypto
     *
     * @param currencies already added crypto currencies
     */
    suspend fun update(currencies: List<CryptoCurrency>)
}
