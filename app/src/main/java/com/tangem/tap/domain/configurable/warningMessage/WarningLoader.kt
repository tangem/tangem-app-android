package com.tangem.tap.domain.configurable.warningMessage

import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tangem.tap.common.analytics.FirebaseAnalyticsHandler
import com.tangem.tap.domain.configurable.Loader

/**
[REDACTED_AUTHOR]
 */
class RemoteWarningLoader(
        private val moshi: Moshi,
) : Loader<List<WarningMessage>> {

    override fun load(onComplete: (List<WarningMessage>) -> Unit) {
        val emptyConfig = listOf<WarningMessage>()
        val remoteConfig = Firebase.remoteConfig
        remoteConfig.fetchAndActivate().addOnCompleteListener {
            if (!it.isSuccessful) {
                onComplete(emptyConfig)
                return@addOnCompleteListener
            }

            val config = remoteConfig.getValue(Loader.warnings)
            val jsonConfig = config.asString()
            if (jsonConfig.isEmpty()) {
                onComplete(emptyConfig)
                return@addOnCompleteListener
            }

            val adapterType = Types.newParameterizedType(List::class.java, WarningMessage::class.java)
            val warningsAdapter: JsonAdapter<List<WarningMessage>> = moshi.adapter(adapterType)
            try {
                val warnings = warningsAdapter.fromJson(jsonConfig) ?: listOf()
                onComplete(warnings)
            } catch (ex: Exception) {
                handleError(ex)
                onComplete(emptyConfig)
            }
        }.addOnFailureListener {
            handleError(it)
            onComplete(emptyConfig)
        }
    }

    private fun handleError(ex: Exception) {
        FirebaseAnalyticsHandler.logException("remote_config_error.warnings", ex)
    }
}