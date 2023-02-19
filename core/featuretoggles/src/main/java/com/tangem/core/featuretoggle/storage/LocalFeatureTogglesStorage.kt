package com.tangem.core.featuretoggle.storage

import android.content.Context
import com.squareup.moshi.JsonAdapter
import com.tangem.core.featuretoggle.storage.LocalFeatureTogglesStorage.Companion.LOCAL_CONFIG_PATH
import timber.log.Timber
import java.io.BufferedReader
import kotlin.properties.Delegates

/**
 * Storage implementation for storing local feature toggles.
 * Feature toggles are declared in file [LOCAL_CONFIG_PATH].
 *
 * @property context     android context
 * @property jsonAdapter adapter for parsing local json config
 *
 * @author Andrew Khokhlov on 26/01/2023
 */
internal class LocalFeatureTogglesStorage(
    private val context: Context,
    private val jsonAdapter: JsonAdapter<List<FeatureToggle>>,
) : FeatureTogglesStorage {

    override var featureToggles: List<FeatureToggle> by Delegates.notNull()
        private set

    override suspend fun init() {
        runCatching { jsonAdapter.fromJson(getConfigJson()) }
            .onSuccess { featureToggles = requireNotNull(it) }
            .onFailure { Timber.e(LocalFeatureTogglesStorage::class.java.name, it.toString()) }
    }

    private fun getConfigJson(): String = context.assets.open(LOCAL_CONFIG_PATH)
        .bufferedReader()
        .use(BufferedReader::readText)

    private companion object {
        const val LOCAL_CONFIG_PATH: String = "configs/feature_toggles_config.json"
    }
}
