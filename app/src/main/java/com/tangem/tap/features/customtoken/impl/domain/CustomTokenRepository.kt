package com.tangem.tap.features.customtoken.impl.domain

import com.tangem.tap.features.customtoken.impl.domain.models.FoundToken

/**
 * Custom token repository
 *
* [REDACTED_AUTHOR]
 */
interface CustomTokenRepository {

    /** Find token by [address] and [networkId] */
    suspend fun findToken(address: String, networkId: String?): FoundToken
}
