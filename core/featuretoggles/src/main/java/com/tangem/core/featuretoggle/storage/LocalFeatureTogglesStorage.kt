package com.tangem.core.featuretoggle.storage

import androidx.annotation.VisibleForTesting
import com.squareup.moshi.JsonAdapter
import com.tangem.core.featuretoggle.storage.LocalFeatureTogglesStorage.Companion.LOCAL_CONFIG_PATH
import com.tangem.datasource.asset.AssetReader
import timber.log.Timber
import kotlin.properties.Delegates

/**
 * Storage implementation for storing local feature toggles.
 * Feature toggles are declared in file [LOCAL_CONFIG_PATH].
 *
 * @property assetReader asset reader
 * @property jsonAdapter adapter for parsing local json config
 *
 * @author Andrew Khokhlov on 26/01/2023
 */
internal class LocalFeatureTogglesStorage(
    private val assetReader: AssetReader,
    private val jsonAdapter: JsonAdapter<List<FeatureToggle>>,
) : FeatureTogglesStorage {

    override var featureToggles: List<FeatureToggle> by Delegates.notNull()
        private set

    override suspend fun init() {
        runCatching { requireNotNull(jsonAdapter.fromJson(assetReader.readJson(LOCAL_CONFIG_PATH))) }
            .onSuccess { featureToggles = it }
            .onFailure { Timber.e(LocalFeatureTogglesStorage::class.java.name, "Failed to parse $LOCAL_CONFIG_PATH") }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun getConfigPath() = LOCAL_CONFIG_PATH

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun getFeatureToggles(): Iterable<FeatureToggle> = featureToggles

    private companion object {
        const val LOCAL_CONFIG_PATH: String = "configs/feature_toggles_config"
    }
}
