package com.tangem.datasource.local.config.issuers

import com.tangem.core.local.assets.AssetLoader
import com.tangem.core.local.assets.loadList
import com.tangem.datasource.local.config.issuers.models.Issuer
import com.tangem.core.local.datastore.RuntimeStateStore

internal class DefaultIssuersConfigStorage(
    private val assetLoader: AssetLoader,
    private val runtimeStateStore: RuntimeStateStore<List<Issuer>>,
) : IssuersConfigStorage {

    override suspend fun getConfig(): List<Issuer> {
        val cachedData = runtimeStateStore.get().value

        if (cachedData.isNotEmpty()) return cachedData

        val issuers = assetLoader.loadList<Issuer>(fileName = ISSUERS_FILE_NAME)

        runtimeStateStore.store(value = issuers)

        return issuers
    }

    private companion object {
        const val ISSUERS_FILE_NAME = "tangem-app-config/issuers"
    }
}