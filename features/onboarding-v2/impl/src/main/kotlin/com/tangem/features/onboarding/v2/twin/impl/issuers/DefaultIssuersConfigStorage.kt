package com.tangem.features.onboarding.v2.twin.impl.issuers

import com.tangem.core.local.assets.AssetLoader
import com.tangem.core.local.assets.loadList
import com.tangem.core.local.datastore.RuntimeStateStore
import com.tangem.features.onboarding.v2.twin.impl.issuers.models.Issuer

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