package com.tangem.features.earn.impl.tracker

import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.features.earn.product.api.session.EarnTxRef
import com.tangem.grow.datasource.preferences.GrowPreferencesKeys
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject

/** A JSON string: [EarnTxRef] is a kotlinx type, and the object helpers of [AppPreferencesStore] use Moshi. */
internal class EarnTxStore @Inject constructor(
    private val appPreferencesStore: AppPreferencesStore,
    private val keys: GrowPreferencesKeys,
) {

    suspend fun add(ref: EarnTxRef) {
        appPreferencesStore.editData { preferences ->
            val refs = decode(preferences[keys.earnSentTransactionsKey])
            preferences[keys.earnSentTransactionsKey] = encode(refs.filterNot { it.id == ref.id } + ref)
        }
    }

    suspend fun remove(id: String) {
        appPreferencesStore.editData { preferences ->
            val refs = decode(preferences[keys.earnSentTransactionsKey])
            preferences[keys.earnSentTransactionsKey] = encode(refs.filterNot { it.id == id })
        }
    }

    suspend fun all(): List<EarnTxRef> = decode(appPreferencesStore.data.first()[keys.earnSentTransactionsKey])

    private fun encode(refs: List<EarnTxRef>): String = json.encodeToString(serializer, refs)

    private fun decode(value: String?): List<EarnTxRef> {
        if (value == null) return emptyList()

        return try {
            json.decodeFromString(serializer, value)
        } catch (e: Exception) {
            TangemLogger.e("Earn journal is unreadable, dropped", e)
            emptyList()
        }
    }

    private companion object {
        val serializer = ListSerializer(EarnTxRef.serializer())

        val json = Json { ignoreUnknownKeys = true }
    }
}