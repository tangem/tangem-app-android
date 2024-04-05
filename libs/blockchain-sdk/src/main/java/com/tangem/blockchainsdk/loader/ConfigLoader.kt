package com.tangem.blockchainsdk.loader

import com.tangem.datasource.config.models.ConfigValueModel

/**
 * Config loader
 *
* [REDACTED_AUTHOR]
 */
internal interface ConfigLoader {

    /** Load config [ConfigValueModel] */
    suspend fun load(): ConfigValueModel?
}
