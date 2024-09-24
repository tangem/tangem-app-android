package com.tangem.datasource.config

import com.tangem.datasource.config.models.Config
import com.tangem.datasource.config.models.ConfigModel

/**
[REDACTED_AUTHOR]
 */
interface ConfigManager {

    val config: Config

    suspend fun load(configLoader: Loader<ConfigModel>, onComplete: ((config: Config) -> Unit)? = null)
}