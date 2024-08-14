package com.tangem.datasource.api.common.config.managers

import com.tangem.datasource.config.ConfigManager
import com.tangem.datasource.config.Loader
import com.tangem.datasource.config.models.Config
import com.tangem.datasource.config.models.ConfigModel
import com.tangem.datasource.config.models.ExpressModel

/**
 * Mock [ConfigManager] implementation for [ProdApiConfigsManagerTest]
 *
* [REDACTED_AUTHOR]
 */
internal class MockConfigManager : ConfigManager {

    override val config = Config(
        express = ExpressModel(apiKey = ProdApiConfigsManagerTest.EXPRESS_API_KEY, signVerifierPublicKey = ""),
        devExpress = ExpressModel(apiKey = ProdApiConfigsManagerTest.EXPRESS_DEV_API_KEY, signVerifierPublicKey = ""),
    )

    override suspend fun load(configLoader: Loader<ConfigModel>, onComplete: ((config: Config) -> Unit)?) = Unit
    override fun turnOff(name: String) = Unit
    override fun resetToDefault(name: String) = Unit
}
