package com.tangem.blockchainsdk.loader

import com.tangem.datasource.config.models.ConfigValueModel

/**
 * Config loader
 *
 * @author Andrew Khokhlov on 04/04/2024
 */
internal interface ConfigLoader {

    /** Load config [ConfigValueModel] */
    suspend fun load(): ConfigValueModel?
}
