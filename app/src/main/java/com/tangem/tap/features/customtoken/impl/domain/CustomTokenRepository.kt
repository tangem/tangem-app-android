package com.tangem.tap.features.customtoken.impl.domain

import com.tangem.tap.features.customtoken.impl.domain.models.FoundToken

/**
 * Custom token repository
 *
 * @author Andrew Khokhlov on 25/04/2023
 */
interface CustomTokenRepository {

    /**
     * Find token by [address] and [networkId]
     *
     * @throws com.tangem.datasource.api.common.response.ApiResponseError
     * */
    suspend fun findToken(address: String, networkId: String?): FoundToken
}
