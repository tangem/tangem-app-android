package com.tangem.datasource.api.common.config.managers

import com.tangem.datasource.api.common.config.ApiConfig

/**
 * Api configs manager
 *
 * @author Andrew Khokhlov on 07/08/2024
 */
interface ApiConfigsManager {

    /** Initialize resources */
    suspend fun initialize() {}

    /** Get base url of api by [id] */
    fun getBaseUrl(id: ApiConfig.ID): String
}
