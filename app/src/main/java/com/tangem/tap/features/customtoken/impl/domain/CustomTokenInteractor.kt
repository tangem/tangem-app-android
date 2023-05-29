package com.tangem.tap.features.customtoken.impl.domain

import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.features.addCustomToken.CustomCurrency
import com.tangem.tap.features.customtoken.impl.domain.models.FoundToken

/**
 * Custom token interactor
 *
[REDACTED_AUTHOR]
 */
interface CustomTokenInteractor {

    /** Find token by [address] and [blockchain] */
    suspend fun findToken(address: String, blockchain: Blockchain): FoundToken

    /** Save token [customCurrency] */
    suspend fun saveToken(customCurrency: CustomCurrency)
}