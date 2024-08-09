package com.tangem.datasource.api.common.config.managers

import com.tangem.datasource.api.common.config.ApiConfig

/**
 * Api configs manager
 *
* [REDACTED_AUTHOR]
 */
interface ApiConfigsManager {

    /** Initialize resources */
    suspend fun initialize() {}

    /** Get base url of api by [id] */
    fun getBaseUrl(id: ApiConfig.ID): String
}
